package net.rpg_foundation.structure_pool.api;

import com.mojang.datafixers.util.Pair;
import net.rpg_foundation.structure_pool.internal.StructurePoolExtension;
import net.rpg_foundation.structure_pool.mixin.StructurePoolAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import java.util.ArrayList;
import java.util.HashMap;

public class StructurePoolAPI {
    private static final ArrayList<StructurePoolConfig.Entry> pendingInjections = new ArrayList<>();
    public static void injectAll(StructurePoolConfig config) {
        synchronized (pendingInjections) {
            pendingInjections.addAll(config.entries);
        }
    }

    public static void processInjections(MinecraftServer server) {
        synchronized (pendingInjections) {
            for(var entry: pendingInjections) {
                var pooldId = Identifier.parse(entry.pool);
                for (var structure: entry.structures) {
                    var structureId = Identifier.parse(structure.id);
                    injectIntoStructurePool(server, pooldId, structureId, structure.weight);
                    if (structure.limit > 0) {
                        limitSpawn(pooldId, structureId, structure.limit);
                    }
                }
            }
        }
        // pendingInjections.clear();
    }

    /**
     * Adding to structure pool
     */

    private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY = ResourceKey.create(
            Registries.PROCESSOR_LIST, Identifier.withDefaultNamespace("empty"));


    public static void injectIntoStructurePool(MinecraftServer server, Identifier poolId, Identifier structureId, int weight) {
        Holder<StructureProcessorList> emptyProcessorList = server.registryAccess()
                .lookupOrThrow(Registries.PROCESSOR_LIST)
                .getOrThrow(EMPTY_PROCESSOR_LIST_KEY);

        var poolGetter = server.registryAccess()
                .lookupOrThrow(Registries.TEMPLATE_POOL)
                .getOptional(poolId);

        if (poolGetter.isEmpty()) {
            System.err.println("StructurePool API: cannot add to " + poolId + " as it cannot be found!");
            return;
        }

        var pool = poolGetter.get();

        var pieceList = ((StructurePoolAccessor) pool).getElements();
        var piece = StructurePoolElement.single(structureId.toString(), emptyProcessorList)
                .apply(StructureTemplatePool.Projection.RIGID);
        ((StructurePoolExtension)pool).remember(piece, structureId);
        var list = new ArrayList<>(((StructurePoolAccessor) pool).getElementCounts());
        list.add(Pair.of(piece, weight));
        ((StructurePoolAccessor) pool).setElementCounts(list);

        for (int i = 0; i < weight; ++i) {
            pieceList.add(piece);
        }
    }

    /**
     * Limiting spawn count
     */

    public record SpawnPerk(int limit) { }
    public static HashMap<Identifier, HashMap<Identifier, SpawnPerk>> spawnLimitations = new HashMap<>();

    public static void limitSpawn(Identifier poolId, Identifier structureId, int limit) {
        if (!spawnLimitations.containsKey(poolId)) {
            spawnLimitations.put(poolId, new HashMap<>());
        }
        spawnLimitations.get(poolId).put(structureId, new SpawnPerk(limit));
    }
}
