package net.zioxs.zmp.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.zioxs.zmp.toast.MusicToast;

import java.util.List;

public class MusicPlayerManager {

    private static MusicPlayerManager INSTANCE;

    public enum PlayMode {
        PLAY_ONCE("Play Once", "\uf04b"),
        REPEAT_ALL("Repeat All", "\uf363"),
        REPEAT_TRACK("Repeat Track", "\uf365"),
        SHUFFLE("Shuffle", "\uf074");

        private final String displayName;
        private final String icon;
        PlayMode(String displayName, String icon) { this.displayName = displayName; this.icon = icon; }
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
    }

    private SoundInstance currentInstance;
    private MusicEntry    currentEntry;
    private MusicToast    currentToast;

    private List<MusicEntry> playlist;
    private int              playlistIndex = -1;
    private PlayMode         playMode      = PlayMode.PLAY_ONCE;

    private boolean isUserInitiated = false;
    private long startTime = 0;
    private boolean showHudOverlay = true;

    private final List<MusicEntry> history = new java.util.ArrayList<>();

    private MusicPlayerManager() {}

    public static MusicPlayerManager getInstance() {
        if (INSTANCE == null) INSTANCE = new MusicPlayerManager();
        return INSTANCE;
    }

    public List<MusicEntry> getHistory() {
        return history;
    }

    public void play(MusicEntry entry, List<MusicEntry> playlist, int index) {
        Minecraft mc = Minecraft.getInstance();
        stopCurrent(mc);

        mc.getMusicManager().stopPlaying();

        currentInstance = new DirectMusicSoundInstance(
                entry.soundLocation(),
                Identifier.fromNamespaceAndPath("minecraft", entry.rawPath())
        );

        mc.getSoundManager().play(currentInstance);
        currentEntry  = entry;

        if (history.isEmpty() || !history.get(history.size() - 1).id().equals(entry.id())) {
            history.add(entry);
            if (history.size() > 50) history.remove(0);
        }

        this.playlist = playlist;
        this.playlistIndex = index;
        isUserInitiated = true;
        startTime = System.currentTimeMillis();

        MusicToast active = mc.getToastManager().getToast(MusicToast.class, MusicToast.TYPE);
        if (active != null) {
            active.updateEntry(entry);
            currentToast = active;
        } else {
            currentToast = new MusicToast(entry);
            mc.getToastManager().addToast(currentToast);
        }
    }

    public void adoptVanillaTrack(SoundInstance instance, MusicEntry entry) {
        currentInstance = instance;
        currentEntry    = entry;
        isUserInitiated = false;
        startTime = System.currentTimeMillis();

        this.playlist = MusicRegistry.getByCategory(MusicCategory.AMBIENT);
        this.playlistIndex = this.playlist.indexOf(entry);

        Minecraft mc = Minecraft.getInstance();
        MusicToast active = mc.getToastManager().getToast(MusicToast.class, MusicToast.TYPE);
        if (active != null) {
            active.updateEntry(entry);
            currentToast = active;
        } else {
            currentToast = new MusicToast(entry);
            mc.getToastManager().addToast(currentToast);
        }
    }

    public void stop() {
        stopCurrent(Minecraft.getInstance());
        currentEntry  = null;
        playlist      = null;
        playlistIndex = -1;
        isUserInitiated = false;
        startTime = 0;
    }

    public void playNext() {
        if (playlist == null || playlist.isEmpty()) return;
        playlistIndex = (playlistIndex + 1) % playlist.size();
        play(playlist.get(playlistIndex), playlist, playlistIndex);
    }

    public void playPrevious() {
        if (playlist == null || playlist.isEmpty()) return;
        playlistIndex = (playlistIndex - 1 + playlist.size()) % playlist.size();
        play(playlist.get(playlistIndex), playlist, playlistIndex);
    }

    private net.minecraft.client.multiplayer.ClientLevel lastWorld;

    public void tick(Minecraft mc) {
        if (mc.level != lastWorld) {
            if (lastWorld != null && isUserInitiated) {
                stop();
            }
            lastWorld = mc.level;
        }

        if (currentInstance == null) return;

        if (!mc.getSoundManager().isActive(currentInstance)) {
            currentInstance = null;
            isUserInitiated = false;

            if (playlist != null && !playlist.isEmpty() && playMode != PlayMode.PLAY_ONCE) {
                if (playMode == PlayMode.SHUFFLE) {
                    playlistIndex = net.minecraft.util.RandomSource.create().nextInt(playlist.size());
                } else if (playMode == PlayMode.REPEAT_ALL) {
                    playlistIndex = (playlistIndex + 1) % playlist.size();
                }
                // If REPEAT_TRACK, playlistIndex remains unchanged!
                
                MusicEntry next = playlist.get(playlistIndex);
                play(next, playlist, playlistIndex);
            } else {
                currentEntry  = null;
                playlist      = null;
                playlistIndex = -1;
            }
        }
    }

    public boolean isUserInitiatedActive() {
        return isUserInitiated && currentInstance != null &&
               Minecraft.getInstance().getSoundManager().isActive(currentInstance);
    }

    public boolean isUserInitiated() { return isUserInitiated; }

    public MusicEntry getCurrentEntry() { return currentEntry; }
    public long getStartTime() { return startTime; }

    public boolean isShowHudOverlay() { return showHudOverlay; }
    public void toggleHudOverlay() { showHudOverlay = !showHudOverlay; }

    private boolean vanillaDelayDisabled = false;
    public boolean isVanillaDelayDisabled() { return vanillaDelayDisabled; }
    public void toggleVanillaDelay() { vanillaDelayDisabled = !vanillaDelayDisabled; }

    public PlayMode getPlayMode() { return playMode; }
    public void setPlayMode(PlayMode mode) { this.playMode = mode; }

    private void stopCurrent(Minecraft mc) {
        if (currentInstance != null) {
            mc.getSoundManager().stop(currentInstance);
            currentInstance = null;
        }
    }
}
