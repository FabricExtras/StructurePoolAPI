package net.fabric_extras.structure_pool.forge;

import net.fabric_extras.structure_pool.StructurePoolMod;
import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(StructurePoolMod.ID)
public final class ForgeMod {
    public ForgeMod() {
        // ServerAboutToStartEvent -- NOT ServerStartingEvent. On Forge 47 the order inside
        // `MinecraftServer#setupServer` is:
        //     handleServerAboutToStart -> loadWorld() -> handleServerStarting
        // `loadWorld()` already generates the spawn region, so injecting at ServerStartingEvent
        // would miss any structure placed in those chunks. ServerAboutToStartEvent is the
        // Forge-side equivalent of Fabric's ServerLifecycleEvents.SERVER_STARTING, which fires
        // just before `setupServer()` is entered.
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        StructurePoolMod.init();
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        StructurePoolAPI.processInjections(event.getServer());
    }
}
