package net.fabric_extras.structure_pool.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabric_extras.structure_pool.internal.StructurePoolExtension;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.List;

@Mixin(StructurePoolBasedGenerator.StructurePoolGenerator.class)
public class StructurePoolBasedGenerator_StructurePoolGeneratorMixin {
    @Shadow @Final private Registry<StructurePool> registry;
    private HashMap<Identifier, HashMap<Identifier, Integer>> limitedSpawns = new HashMap<>();
    @Nullable private Identifier currentPoolId;

    /**
     * Structure sub-pools are shuffled during the generation process,
     * so we save here what is the sub-pool id that is currently being generated.
     */

    @WrapOperation(method = "generatePiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/structure/pool/StructurePoolBasedGenerator$StructurePoolGenerator;getPoolKey(Lnet/minecraft/structure/StructureTemplate$StructureBlockInfo;)Lnet/minecraft/registry/RegistryKey;"))
    private RegistryKey<StructurePool> getPoolKey_Wrapped(StructureTemplate.StructureBlockInfo blockInfo, Operation<RegistryKey<StructurePool>> original) {
        var key = original.call(blockInfo);
        var poolId = key.getValue();
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

        currentPoolId = key.getValue();
        return key;
    }

    /**
     * Filtering out elements that are not allowed are over spawn limit
     */

    @WrapOperation(method = "generatePiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/structure/pool/StructurePool;getElementIndicesInRandomOrder(Lnet/minecraft/util/math/random/Random;)Ljava/util/List;"))
    private List<StructurePoolElement> getElementIndicesInRandomOrder_Wrapped(StructurePool pool, Random random, Operation<List<StructurePoolElement>> original) {
        var result = original.call(pool, random);
        result.removeIf(element -> !limitAllowsSpawn(element));
        return result;
    }

    /**
     * Updating limit count upon successful spawn
     */

    @WrapOperation(method = "generatePiece", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean children_Wrapped(List list, Object object,
                                                 Operation<Boolean> original) {
        if (object instanceof PoolStructurePiece piece) {
            consumeLimit(piece.getPoolElement());
        }
        return original.call(list, object);
    }

    @Unique
    @Nullable private Identifier resolveStructureElementId(StructurePoolElement element) {
        if (currentPoolId == null) {
            return null;
        }
        var pool = registry.get(currentPoolId);
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
