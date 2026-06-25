package net.zioxs.zmp.mixin;

import net.minecraft.client.sounds.MusicManager;
import net.zioxs.zmp.music.MusicPlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MixinMusicManager {

    @Shadow
    private int nextSongDelay;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void zmp$suppressVanillaMusic(CallbackInfo ci) {
        if (MusicPlayerManager.getInstance().isUserInitiatedActive()) {
            ci.cancel();
            return;
        }
        
        if (MusicPlayerManager.getInstance().isVanillaDelayDisabled() && this.nextSongDelay > 0) {
            this.nextSongDelay = 0;
        }
    }
}
