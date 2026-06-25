package net.zioxs.zmp.music;

import net.minecraft.resources.Identifier;

public record MusicEntry(
        String id,
        String title,
        String artist,
        String duration,
        int durationSeconds,
        String rawPath,
        Identifier soundLocation,
        MusicCategory category
) {}
