package net.rpg_foundation.structure_pool.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.rpg_foundation.structure_pool.api.StructurePoolAPI;
import net.rpg_foundation.structure_pool.internal.StructurePoolExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.List;

@Mixin(JigsawPlacement.Placer.class)
public class StructurePoolBasedGenerator_StructurePoolGeneratorMixin {
    @Shadow @Final private Registry<StructureTemplatePool> pools;
    private HashMap<Identifier, HashMap<Identifier, Integer>> limitedSpawns = new HashMap<>();
    @Nullable private Identifier currentPoolId;

    /**
     * Structure sub-pools are shuffled during the generation process,
     * so we save here what is the sub-pool id that is currently being generated.
     */

    @WrapOperation(method = "tryPlacingChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasLookup;lookup(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<StructureTemplatePool> getPoolKey_Wrapped(PoolAliasLookup aliasLookup, ResourceKey<StructureTemplatePool> poolKey, Operation<ResourceKey<StructureTemplatePool>> original) {
        var key = original.call(aliasLookup, poolKey);
        var poolId = key.identifier();
        // System.out.println("getPoolKey_Wrapped " + " | at: " + blockInfo.pos() + " | Generating: " + key.getValue() + " | session: " + Integer.toHexString(this.hashCode()));
        if (!limitedSpawns.containsKey(poolId) && StructurePoolAPI.spawnLimitations.containsKey(poolId)) {
            var freshLimitations = new HashMap<Identifier, Integer>();
            for (var entry: StructurePoolAPI.spawnLimitations.get(poolId).entrySet()) {
                var structureId = entry.getKey();
                var spawnPerk = entry.getValue();
                var spawnLimit = spawnPerk.limit();
                freshLimitations.put(structureId, spawnLimit);
            }
            limitedSpawns.put(poolId, freshLimitations);
            // System.out.println("Adding limitations for pool: " + poolId + " | entries: " + freshLimitations);
        }

        currentPoolId = key.identifier();
        return key;
    }

    /**
     * Filtering out elements that are not allowed are over spawn limit
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

    @Unique
    @Nullable private Identifier resolveStructureElementId(StructurePoolElement element) {
        if (currentPoolId == null) {
            return null;
        }
        var pool = pools.getValue(currentPoolId);
        if (pool == null) {
            return null;
        }
        return ((StructurePoolExtension)pool).identify(element);
    }

    private boolean limitAllowsSpawn(StructurePoolElement element) {
        var structureId = resolveStructureElementId(element);
        if (structureId == null) {
            return true;
        }
        if (!limitedSpawns.containsKey(currentPoolId)) {
            return true;
        }
        if (!limitedSpawns.get(currentPoolId).containsKey(structureId)) {
            return true;
        }
        var limit = limitedSpawns.get(currentPoolId).get(structureId);
        // System.out.println("limitAllowsSpawn " + " | structureId: " + structureId + " | limit: " + limit);
        return limit > 0;
    }

    private void consumeLimit(StructurePoolElement element) {
        var structureId = resolveStructureElementId(element);
        if (structureId == null) { return; }
        var poolSpecificLimits = limitedSpawns.get(currentPoolId);
        if (poolSpecificLimits == null) { return; }
        if (!poolSpecificLimits.containsKey(structureId)) { return; }
        var limit = poolSpecificLimits.get(structureId);
        // System.out.println("limitWillBeConsumed " + " | structureId: " + structureId + " | limit: " + limit);
        if (limit > 0) {
            poolSpecificLimits.put(structureId, limit - 1);
        }
    }
}
