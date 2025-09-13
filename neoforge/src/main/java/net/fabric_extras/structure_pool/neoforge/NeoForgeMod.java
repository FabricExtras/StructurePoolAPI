package net.fabric_extras.structure_pool.neoforge;

import net.neoforged.fml.common.Mod;

import net.fabric_extras.structure_pool.ExampleMod;

@Mod(StructurePoolMod.ID)
public final class NeoForgeMod {
    public NeoForgeMod() {
        // Run our common setup.
        ExampleMod.init();
    }
}
