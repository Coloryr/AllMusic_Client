package com.coloryr.allmusic.client.mixin;

import com.coloryr.allmusic.client.core.AllMusicCore;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public class SoundEvent {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if (AllMusicCore.isPlay()) {
            SoundCategory data = sound.getCategory();
            switch (data) {
                case RECORDS, MUSIC:
                {
                    cir.cancel();
                    cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
                }
            }
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance soundInstance, int delay, CallbackInfo info) {
        if (AllMusicCore.isPlay()) {
            SoundCategory data = soundInstance.getCategory();
            switch (data) {
                case RECORDS, MUSIC -> info.cancel();
            }
        }
    }

    @Inject(method = "reloadSounds", at = @At("RETURN"))
    public void reload(CallbackInfo info){
        AllMusicCore.reload();
    }
}
