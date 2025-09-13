package net.fabric_extras.structure_pool.neoforge;

import net.fabric_extras.structure_pool.StructurePoolMod;
import net.neoforged.fml.common.Mod;

@Mod(StructurePoolMod.ID)
public final class NeoForgeMod {
    public NeoForgeMod() {
        // Run our common setup.
        StructurePoolMod.init();
    }
}
