package net.rpg_foundation.structure_pool.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.rpg_foundation.structure_pool.api.StructurePoolAPI;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// `JigsawPlacement$Placer` is a private inner class, so a class literal would need an access
// widener just to compile. The string form needs no accessibility at all -- Mixin works on
// bytecode -- which is why this mod ships no access widener / access transformer.
//
// CAUTION, verified empirically on 26.1.2 (Fabric dev server): if this string is wrong, the run
// does NOT crash. `"required": true` and `injectors.defaultRequire: 1` only govern injection
// points *inside a target that was found*; a missing target class is logged as
//   [main/WARN] (FabricLoader/Mixin) @Mixin target <name> was not found structure_pool_api.mixins.json:...
// and the game boots clean, with the spawn caps silently disabled. javac cannot check the string
// and Loom 1.17 ships no Mixin AP, so grep this log line after any rename of the target.
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public class StructurePoolBasedGenerator_StructurePoolGeneratorMixin {
    /**
     * Remaining spawn budget for this one jigsaw placement: injected pool id -> structure id ->
     * spawns left. One `Placer` instance is one structure placement, so the budget is naturally
     * scoped and thrown away with it.
     *
     * <p>Elements are attributed through {@link StructurePoolAPI#injectedElement} — the element
     * instance itself carries its (pool, structure). The previous implementation instead tracked
     * "which pool is being expanded right now" from a `PoolAliasLookup.lookup` wrap, which was only
     * ever set for the *target* pool; `tryPlacingChildren` also draws from the pool's **fallback**
     * pool, and those elements were silently exempted from their cap.
     */
    @Unique
    private Map<Identifier, Map<Identifier, Integer>> limitedSpawns = new HashMap<>();

    /**
     * Filtering out elements that are over their spawn limit
     */

    @WrapOperation(method = "tryPlacingChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;getShuffledTemplates(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private List<StructurePoolElement> getElementIndicesInRandomOrder_Wrapped(StructureTemplatePool pool, RandomSource random, Operation<List<StructurePoolElement>> original) {
        var result = original.call(pool, random);
        result.removeIf(element -> !limitAllowsSpawn(element));
        return result;
    }

    /**
     * Updating limit count upon successful spawn
     */

    @WrapOperation(method = "tryPlacingChildren", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean children_Wrapped(List list, Object object,
                                                 Operation<Boolean> original) {
        if (object instanceof PoolElementStructurePiece piece) {
            consumeLimit(piece.getElement());
        }
        return original.call(list, object);
    }

    /**
     * @return spawns left for this element in this placement, or null when the element is not one
     *         of ours or carries no configured limit.
     */
    @Unique
    @Nullable
    private Integer remainingSpawns(StructurePoolElement element) {
        var injected = StructurePoolAPI.injectedElement(element);
        if (injected == null) {
            return null;
        }
        var poolLimits = StructurePoolAPI.spawnLimitations.get(injected.poolId());
        if (poolLimits == null) {
            return null;
        }
        var perk = poolLimits.get(injected.structureId());
        if (perk == null) {
            return null;
        }
        return limitedSpawns
                .computeIfAbsent(injected.poolId(), id -> new HashMap<>())
                .computeIfAbsent(injected.structureId(), id -> perk.limit());
    }

    @Unique
    private boolean limitAllowsSpawn(StructurePoolElement element) {
        var remaining = remainingSpawns(element);
        return remaining == null || remaining > 0;
    }

    @Unique
    private void consumeLimit(StructurePoolElement element) {
        var remaining = remainingSpawns(element);
        if (remaining == null || remaining <= 0) {
            return;
        }
        var injected = StructurePoolAPI.injectedElement(element);
        limitedSpawns.get(injected.poolId()).put(injected.structureId(), remaining - 1);
    }
}
