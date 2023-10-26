package net.testmod;

import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;

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
    }
}