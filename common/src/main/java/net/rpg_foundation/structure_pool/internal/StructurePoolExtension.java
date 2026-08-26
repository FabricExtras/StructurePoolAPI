package net.rpg_foundation.structure_pool.internal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.jetbrains.annotations.Nullable;

public interface StructurePoolExtension {
    void remember(StructurePoolElement element, Identifier identifier);
    @Nullable Identifier identify(StructurePoolElement element);
}
