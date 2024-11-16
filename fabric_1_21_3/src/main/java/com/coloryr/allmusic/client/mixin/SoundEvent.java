package com.coloryr.allmusic.client.mixin;

import com.coloryr.allmusic.client.AllMusic;
import com.coloryr.allmusic.client.player.APlayer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class SoundEvent {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance soundInstance, CallbackInfo info) {
        if (AllMusic.nowPlaying.isPlay()) {
            SoundCategory data = soundInstance.getCategory();
            switch (data) {
                case RECORDS, MUSIC -> info.cancel();
            }
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance soundInstance, int delay, CallbackInfo info) {
        if (AllMusic.nowPlaying.isPlay()) {
            SoundCategory data = soundInstance.getCategory();
            switch (data) {
                case RECORDS, MUSIC -> info.cancel();
            }
        }
    }

    @Inject(method = "reloadSounds", at = @At("RETURN"))
    public void reload(CallbackInfo info){
        AllMusic.reload();
    }
}
