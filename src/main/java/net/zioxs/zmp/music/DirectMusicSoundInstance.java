package net.zioxs.zmp.music;

import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class DirectMusicSoundInstance extends AbstractSoundInstance {
    private final Identifier rawPathId;

    public DirectMusicSoundInstance(Identifier soundEvent, Identifier rawPathId) {
        super(soundEvent, SoundSource.MUSIC, RandomSource.create());
        this.rawPathId = rawPathId;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.looping = false;
        this.delay = 0;
        this.attenuation = Attenuation.NONE;
        this.relative = true;
    }

    @Override
    public @Nullable WeighedSoundEvents resolve(SoundManager soundManager) {
        WeighedSoundEvents set = super.resolve(soundManager);
        if (set != null) {
            // Using 1.21.1 Sound constructor
            this.sound = new Sound(
                    this.rawPathId,
                    ConstantFloat.of(1.0f),
                    ConstantFloat.of(1.0f),
                    1,
                    Sound.Type.FILE,
                    true,
                    false,
                    16
            );
        }
        return set;
    }
}
