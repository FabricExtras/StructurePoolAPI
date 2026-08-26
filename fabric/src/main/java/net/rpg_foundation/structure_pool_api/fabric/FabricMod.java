package net.rpg_foundation.structure_pool_api.fabric;

import net.rpg_foundation.structure_pool_api.StructurePoolMod;
import net.rpg_foundation.structure_pool_api.api.StructurePoolAPI;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class FabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        StructurePoolMod.init();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            StructurePoolAPI.processInjections(server);
        });
    }
}
