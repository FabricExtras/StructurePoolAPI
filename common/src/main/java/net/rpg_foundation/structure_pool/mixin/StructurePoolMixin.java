package net.rpg_foundation.structure_pool.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.rpg_foundation.structure_pool.internal.StructurePoolExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.Map;

@Mixin(StructureTemplatePool.class)
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
