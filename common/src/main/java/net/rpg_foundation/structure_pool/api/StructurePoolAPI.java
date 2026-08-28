package net.rpg_foundation.structure_pool.api;

import com.google.common.collect.MapMaker;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.rpg_foundation.structure_pool.mixin.StructurePoolAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * Identity of one element this API appended to a pool.
     *
     * <p>Every {@link #injectIntoStructurePool} call builds a brand-new {@code SinglePoolElement}
     * instance for exactly one (pool, structure) pair, and {@code StructurePoolElement} declares no
     * {@code equals}/{@code hashCode} — so the element instance alone identifies both ends. That is
     * what lets the {@code JigsawPlacement$Placer} mixin attribute an element without knowing which
     * pool it was drawn from (which is what the old duck-interface + `PoolAliasLookup.lookup` wrap
     * did, and got wrong for elements served through a pool's *fallback* pool).
     */
    public record InjectedElement(Identifier poolId, Identifier structureId) { }

    /**
     * Weak *identity* map (MapMaker.weakKeys uses ==), so entries die with the pool objects when a
     * world is closed. A plain static IdentityHashMap would leak one entry per injection per
     * server start.
     */
    private static final Map<StructurePoolElement, InjectedElement> injectedElements =
            new MapMaker().weakKeys().makeMap();

    /**
     * Internal — used by the {@code JigsawPlacement$Placer} mixin.
     * @return the (pool, structure) this element was injected for, or null if we did not inject it.
     */
    @Nullable
    public static InjectedElement injectedElement(StructurePoolElement element) {
        return injectedElements.get(element);
    }

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
        injectedElements.put(piece, new InjectedElement(poolId, structureId));
        // `getTemplates()` is vanilla-public (@VisibleForTesting) and returns the live `rawTemplates` list.
        var list = new ArrayList<>(pool.getTemplates());
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
