package net.fabric_extras.structure_pool.internal;

import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface StructurePoolExtension {
    void remember(StructurePoolElement element, Identifier identifier);
    @Nullable Identifier identify(StructurePoolElement element);
}
