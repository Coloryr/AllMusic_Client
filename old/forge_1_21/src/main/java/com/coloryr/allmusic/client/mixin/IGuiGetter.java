package com.coloryr.allmusic.client.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.LayeredDraw;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface IGuiGetter {
    @Accessor(value = "layers")
    LayeredDraw getLayers();
}
