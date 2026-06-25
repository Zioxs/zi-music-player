package net.zioxs.zmp.music;

public enum MusicCategory {
    DISCS("Discs"),
    AMBIENT("Ambient"),
    PLAYLIST("Playlist");

    private final String displayName;

    MusicCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
