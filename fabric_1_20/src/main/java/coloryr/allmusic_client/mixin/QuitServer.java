package coloryr.allmusic_client.mixin;

import coloryr.allmusic_client.AllMusic;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class QuitServer {
    @Inject(method = "disconnect()V", at = @At("TAIL"))
    public void Quit(CallbackInfo info) {
        AllMusic.onServerQuit();
    }
}
