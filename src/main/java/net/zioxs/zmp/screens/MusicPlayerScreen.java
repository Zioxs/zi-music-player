package net.zioxs.zmp.screens;

import imgui.ImFont;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.zioxs.zmp.imgui.RenderInterface;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.zioxs.zmp.music.*;

import java.util.List;

import static net.zioxs.zmp.imgui.ImGuiImpl.loadFont;
import static net.zioxs.zmp.imgui.ImGuiImpl.loadFontWithIcons;

public final class MusicPlayerScreen extends Screen implements RenderInterface {
    private static ImFont mcFont;

    private MusicCategory activeTab = MusicCategory.AMBIENT;
    private final ImString query = new ImString(256);

    public MusicPlayerScreen() {
        super(Component.literal("Example Screen"));
        if (mcFont == null)
            mcFont = loadFontWithIcons("/assets/zmp/fonts/minecraft.ttf", "/assets/zmp/fonts/fa-solid-900.ttf", 16);

    }

    @Override
    protected void init() {
    }

    @Override
    public void render(ImGuiIO io) {

        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize;

        if (ImGui.begin("Music Player", flags)) {
            ImGui.setWindowPos(0, 0, ImGuiCond.Always);
            ImGui.setWindowSize(500, io.getDisplaySizeY(), ImGuiCond.Always);

            ImGui.pushFont(mcFont);

            ImGui.textColored(116 / 255f, 87 / 255f, 228 / 255f, 1f, "Zi's Music Player");
            ImGui.separator();
            ImGui.spacing();

            List<MusicEntry> list = MusicRegistry.search(query.get(), activeTab);
            MusicEntry playing = MusicPlayerManager.getInstance().getCurrentEntry();

            ImGui.setNextItemOpen(true, ImGuiCond.Appearing);
            if (ImGui.treeNode("Tracks List")) {
                if (ImGui.beginTabBar("Categories")) {
                    if (ImGui.beginTabItem("Ambient")) {
                        activeTab = MusicCategory.AMBIENT;
                        ImGui.endTabItem();
                    }
                    if (ImGui.beginTabItem("Discs")) {
                        activeTab = MusicCategory.DISCS;
                        ImGui.endTabItem();
                    }
                    if (ImGui.beginTabItem("Playlist")) {
                        activeTab = MusicCategory.PLAYLIST;
                        ImGui.endTabItem();
                    }
                    ImGui.endTabBar();
                }

                ImGui.setNextItemWidth(-1);
                ImGui.inputTextWithHint("##search", "Search...", query);

                ImGui.spacing();

                // Re-fetch list in case search/tab changed this frame
                list = MusicRegistry.search(query.get(), activeTab);

                ImGui.beginChild("##trackList", 0, -200, true);
                for (int i = 0; i < list.size(); i++) {
                    MusicEntry entry = list.get(i);
                    boolean isSelected = playing != null && playing.id().equals(entry.id());

                    ImGui.pushID(entry.id());

                    // Star icon to toggle playlist status
                    boolean inPlaylist = PlaylistManager.contains(entry.id());
                    if (inPlaylist) {
                        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 116 / 255f, 87 / 255f, 228 / 255f, 1f); // Accent Purple
                    } else {
                        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 138 / 255f, 138 / 255f, 138 / 255f, 1f); // Disabled Gray
                    }
                    if (ImGui.selectable("\uf005##star_" + entry.id(), false, 0, 16, 0)) { // \uf005 is FontAwesome Star
                        PlaylistManager.toggle(entry.id());
                    }

                    ImGui.popStyleColor();

                    ImGui.sameLine();
                    if (ImGui.selectable(entry.title() + " - " + entry.artist() + " (" + entry.duration() + ")", isSelected)) {
                        MusicPlayerManager.getInstance().play(entry, list, i);
                    }
                    ImGui.popID();
                }
                ImGui.endChild();
                ImGui.treePop();
            }
            ImGui.spacing();

            MusicPlayerManager pm = MusicPlayerManager.getInstance();

            if (ImGui.treeNode("Settings")) {
                float musicVolOpt = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
                float[] vol = new float[] { musicVolOpt * 100f };
                if (ImGui.sliderFloat("Music Volume", vol, 0f, 100f, "%.0f%%")) {
                    musicVolOpt = (vol[0] / 100f);
                    
                    // Update live playback volume
                    Minecraft.getInstance().getSoundManager().updateCategoryVolume(SoundSource.MUSIC, musicVolOpt);

                    Minecraft.getInstance().options.getSoundSourceOptionInstance(SoundSource.MUSIC).set((double) musicVolOpt);
                }
                if (ImGui.isItemDeactivatedAfterEdit()) {
                    Minecraft.getInstance().options.save();
                }

                imgui.type.ImBoolean hudEnabled = new imgui.type.ImBoolean(pm.isShowHudOverlay());
                if (toggleSwitch("HUD Overlay", hudEnabled)) {
                    pm.toggleHudOverlay();
                }

                imgui.type.ImBoolean delayEnabled = new imgui.type.ImBoolean(!pm.isVanillaDelayDisabled());
                if (toggleSwitch("Vanilla Music Delay", delayEnabled)) {
                    pm.toggleVanillaDelay();
                }
                ImGui.treePop();
            }
            ImGui.spacing();

            if (ImGui.treeNode("History")) {
                java.util.List<MusicEntry> history = pm.getHistory();
                if (history.isEmpty()) {
                    ImGui.textDisabled("No tracks played yet.");
                } else {
                    ImGui.beginChild("##historyList", 0, 100, true);
                    for (int i = history.size() - 1; i >= 0; i--) {
                        MusicEntry entry = history.get(i);
                        if (ImGui.selectable(entry.title() + " - " + entry.artist())) {
                            pm.play(entry, java.util.List.of(entry), 0);
                        }
                    }
                    ImGui.endChild();
                }
                ImGui.treePop();
            }
            ImGui.spacing();

            MusicEntry currentlyPlaying = pm.getCurrentEntry();
            if (currentlyPlaying != null) {
                ImGui.textColored(116 / 255f, 87 / 255f, 228 / 255f, 1f, "  Now Playing");
                ImGui.text("  " + currentlyPlaying.title() + (pm.isUserInitiated() ? "" : " [Vanilla]" ) + " - " + currentlyPlaying.artist());

                long elapsedSeconds = (System.currentTimeMillis() - pm.getStartTime()) / 1000;
                int elapsedMins = (int) (elapsedSeconds / 60);
                int elapsedSecs = (int) (elapsedSeconds % 60);
                String timeStr = String.format("%d:%02d / %s", elapsedMins, elapsedSecs, currentlyPlaying.duration());

                ImGui.textColored(138 / 255f, 138 / 255f, 138 / 255f, 1f, "  " + timeStr);
            } else {
                ImGui.textColored(138 / 255f, 138 / 255f, 138 / 255f, 1f, "  Nothing playing");
            }

            ImGui.spacing();

            if (ImGui.button("\uf048")) pm.playPrevious(); // backward-step
            ImGui.sameLine();

            boolean isPlaying = pm.isUserInitiatedActive() || (!pm.isUserInitiated() && pm.getCurrentEntry() != null);
            if (ImGui.button(isPlaying ? "\uf04c" : "\uf04b")) { // pause or play
                if (isPlaying) pm.stop();
                else if (list.size() > 0) pm.play(list.get(0), list, 0);
            }
            ImGui.sameLine();
            if (ImGui.button("\uf051")) pm.playNext(); // forward-step

            ImGui.sameLine();
            ImGui.setNextItemWidth(200);
            if (ImGui.beginCombo("##playmode", pm.getPlayMode().getIcon() + " " + pm.getPlayMode().getDisplayName())) {
                for (MusicPlayerManager.PlayMode mode : MusicPlayerManager.PlayMode.values()) {
                    boolean isSelected = (pm.getPlayMode() == mode);
                    if (ImGui.selectable(mode.getIcon() + " " + mode.getDisplayName(), isSelected)) {
                        pm.setPlayMode(mode);
                    }
                    if (isSelected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endCombo();
            }

        }

        ImGui.popFont();
        ImGui.end();
    }

    private boolean toggleSwitch(String label, imgui.type.ImBoolean value) {
        float rowHeight = 16f;
        float toggleW = 34f;

        // Draw label
        ImGui.alignTextToFramePadding();
        ImGui.text(label);

        // Right-align the toggle
        ImGui.sameLine(ImGui.getWindowContentRegionMaxX() - toggleW);

        imgui.ImVec2 pos = ImGui.getCursorScreenPos();
        ImGui.invisibleButton("##" + label, toggleW, rowHeight);

        boolean changed = false;
        if (ImGui.isItemClicked()) {
            value.set(!value.get());
            changed = true;
        }

        boolean on = value.get();
        imgui.ImDrawList dl = ImGui.getWindowDrawList();

        int trackColor = ImGui.getColorU32(on ? imgui.flag.ImGuiCol.ButtonActive : imgui.flag.ImGuiCol.FrameBg);
        dl.addRectFilled(pos.x, pos.y, pos.x + toggleW, pos.y + rowHeight, trackColor);

        float knobW = 14f;
        float knobH = 12f;
        float kx = on ? pos.x + toggleW - 1f - knobW : pos.x + 1f;
        float ky = pos.y + (rowHeight - knobH) * 0.5f;

        int knobColor = ImGui.getColorU32(on ? imgui.flag.ImGuiCol.Text : imgui.flag.ImGuiCol.TextDisabled);
        dl.addRectFilled(kx, ky, kx + knobW, ky + knobH, knobColor);

        return changed;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Only relevant in singleplayer
    }

}
