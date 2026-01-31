package com.coloryr.allmusic.client.mixin;

import com.coloryr.allmusic.client.AllMusicClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class GuiShow {
    @Inject(method = {"renderStatusEffectOverlay"}, at = {@At(value = "HEAD")})
    public void Gui(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        AllMusicClient.update(context);
    }
}
