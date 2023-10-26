package net.testmod;

import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabric_extras.structure_pool.api.StructurePoolConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TestMod implements ModInitializer {
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            // Target structure pool
            var targetPool = new Identifier("minecraft:village/plains/houses");
            // Our structure (found amongst data files)
            var structureId = new Identifier("testmod:village/plains/gazebo");
            StructurePoolAPI.injectIntoStructurePool(
                    server,
                    targetPool, // Target structure pool
                    structureId, // Our structure (found amongst data files)
                    10 // Weight (higher numbers increase relative spawn chance)
            );
            StructurePoolAPI.limitSpawn(targetPool, structureId, 3);
        });

        // This config represents a fully serializable JSON structure
        var config = new StructurePoolConfig();
        // Now we are adding default entries, but the entire config could just be read from a JSON file
        config.entries = new ArrayList<>(List.of(
           new StructurePoolConfig.Entry(
                   "minecraft:village/desert/houses",
                   "testmod:village/plains/gazebo",
                   10,
                   2
           )
        ));
        // Inject all entries from the config
        StructurePoolAPI.injectAll(config);
    }
}