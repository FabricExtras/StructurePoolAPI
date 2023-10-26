package net.fabric_extras.structure_pool.api;

import com.mojang.datafixers.util.Pair;
import net.fabric_extras.structure_pool.internal.StructurePoolExtension;
import net.fabric_extras.structure_pool.mixin.StructurePoolAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;

public class StructurePoolAPI {
    public static void loadConfig(StructurePoolConfig config) {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            for(var entry: config.entries) {
                var pooldId = new Identifier(entry.pool);
                for (var structure: entry.structures) {
                    var structureId = new Identifier(structure.id);
                    addToStructurePool(server, pooldId, structureId, structure.weight);
                    if (structure.limit > 0) {
                        limitSpawn(pooldId, structureId, structure.limit);
                    }
                }
            }
        });
    }

    /**
     * Adding to structure pool
     */

    private static final RegistryKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY = RegistryKey.of(
            RegistryKeys.PROCESSOR_LIST, new Identifier("minecraft", "empty"));


    public static void addToStructurePool(MinecraftServer server, Identifier poolId, Identifier structureId, int weight) {
        RegistryEntry<StructureProcessorList> emptyProcessorList = server.getRegistryManager()
                .get(RegistryKeys.PROCESSOR_LIST)
                .entryOf(EMPTY_PROCESSOR_LIST_KEY);

        var poolGetter = server.getRegistryManager()
                .get(RegistryKeys.TEMPLATE_POOL)
                .getOrEmpty(poolId);

        if (poolGetter.isEmpty()) {
            System.err.println("StructurePool API: cannot add to " + poolId + " as it cannot be found!");
            return;
        }

        var pool = poolGetter.get();

        var pieceList = ((StructurePoolAccessor) pool).getElements();
        var piece = StructurePoolElement.ofProcessedSingle(structureId.toString(), emptyProcessorList)
                .apply(StructurePool.Projection.RIGID);
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
