package net.rpg_foundation.structure_pool.neoforge;

import net.rpg_foundation.structure_pool.StructurePoolMod;
import net.rpg_foundation.structure_pool.api.StructurePoolAPI;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(StructurePoolMod.ID)
public final class NeoForgeMod {
    public NeoForgeMod() {
        NeoForge.EVENT_BUS.addListener(ServerStartingEvent.class, this::onServerStarting);
        StructurePoolMod.init();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        StructurePoolAPI.processInjections(event.getServer());
    }
}
