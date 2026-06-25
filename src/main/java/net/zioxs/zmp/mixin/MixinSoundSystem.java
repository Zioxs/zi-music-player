package net.zioxs.zmp.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.zioxs.zmp.music.MusicEntry;
import net.zioxs.zmp.music.MusicPlayerManager;
import net.zioxs.zmp.music.MusicRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MixinSoundSystem {

    @Inject(method = "play", at = @At("TAIL"))
    private void zmp$onSoundPlay(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (soundInstance.getSource() == SoundSource.MUSIC || soundInstance.getSource() == SoundSource.RECORDS) {
            if (soundInstance.getSound() != null) {
                Identifier loc = soundInstance.getSound().getLocation();
                MusicEntry entry = MusicRegistry.findByRawPath(loc.getPath());
                
                if (entry != null && !MusicPlayerManager.getInstance().isUserInitiatedActive()) {
                    MusicPlayerManager.getInstance().adoptVanillaTrack(soundInstance, entry);
                }
            }
        }
    }
}
