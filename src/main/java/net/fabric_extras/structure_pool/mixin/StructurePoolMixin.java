package net.fabric_extras.structure_pool.mixin;

import net.fabric_extras.structure_pool.internal.StructurePoolExtension;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.Map;

@Mixin(StructurePool.class)
public class StructurePoolMixin implements StructurePoolExtension {
    private Map<StructurePoolElement, Identifier> identifiedElements = new HashMap<>();
    @Override
    public void remember(StructurePoolElement element, Identifier identifier) {
        identifiedElements.put(element, identifier);
    }

    @Override
    public @Nullable Identifier identify(StructurePoolElement element) {
        return identifiedElements.get(element);
    }
}
