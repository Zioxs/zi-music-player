package net.zioxs.zmp;


import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.zioxs.zmp.music.MusicEntry;
import net.zioxs.zmp.music.MusicPlayerManager;
import net.zioxs.zmp.music.MusicRegistry;
import net.zioxs.zmp.music.PlaylistManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.zioxs.zmp.screens.MusicPlayerScreen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Zmp implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("zpm");

    public static final KeyMapping EXAMPLE_KEYBINDING = new KeyMapping(
            "key.imguiexample.example_keybinding",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
    );

    public final static KeyMapping OPEN_SCREEN = new KeyMapping(
            "key.zmp.open_screen",
            GLFW.GLFW_KEY_M,
            KeyMapping.Category.MISC
    );;


    @Override
    public void onInitialize() {
        PlaylistManager.load();

        ResourceManagerHelperImpl.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.fromNamespaceAndPath("zmp", "music_loader");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                MusicRegistry.loadFromResourcePack(resourceManager);
            }
        });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SCREEN.consumeClick()) {
                if (client.screen == null) {
                     client.setScreen(new MusicPlayerScreen());
                } else if (client.screen.getClass().getSimpleName().equals("MusicPlayerScreen")) {
                    client.setScreen(null);
                }
            }

            MusicPlayerManager.getInstance().tick(client);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (!MusicPlayerManager.getInstance().isShowHudOverlay()) return;
            MusicEntry playing = MusicPlayerManager.getInstance().getCurrentEntry();
            if (playing == null) return;

            Minecraft client = Minecraft.getInstance();
            if (client.options.hideGui) return;

            int screenWidth = context.guiWidth();
            int screenHeight = context.guiHeight();

            int w = 150;
            int h = 34;
            int padding = 6;
            int x = screenWidth - w - padding;
            int y = screenHeight - h - padding;

            context.fill(x, y, x + w, y + h, 0xB0060610);
            context.fill(x, y, x + w, y + 1, 0xFF1A1A36);
            context.fill(x + w - 1, y, x + w, y + h, 0xFF1A1A36);
            context.fill(x, y + h - 1, x + w, y + h, 0xFF1A1A36);
            context.fill(x, y, x + 2, y + h, 0xFF4324B3);

            boolean isVanilla = !MusicPlayerManager.getInstance().isUserInitiated();
            String title = playing.title();
            if (isVanilla) {
                String tag = " [Vanilla]";
                int tagW = client.font.width(tag);
                if (client.font.width(title) + tagW > w - 8) {
                    title = client.font.plainSubstrByWidth(title, w - 14 - tagW) + "…";
                }
                context.drawString(client.font, title, x + 6, y + 6, 0xFFFFFFFF, false);
                context.drawString(client.font, tag, x + 6 + client.font.width(title), y + 6, 0xFF7457E4, false);
            } else {
                if (client.font.width(title) > w - 8) {
                    title = client.font.plainSubstrByWidth(title, w - 14) + "…";
                }
                context.drawString(client.font, title, x + 6, y + 6, 0xFFFFFFFF, false);
            }

            long start = MusicPlayerManager.getInstance().getStartTime();
            String durationSuffix = "";
            if (start > 0) {
                int elapsedSecs = (int) ((System.currentTimeMillis() - start) / 1000);
                elapsedSecs = Math.min(elapsedSecs, playing.durationSeconds());
                String elapsedStr = String.format("%d:%02d", elapsedSecs / 60, elapsedSecs % 60);
                durationSuffix = "[" + elapsedStr + " / " + playing.duration() + "]";
            }

            String sub = playing.artist();
            if (client.font.width(sub) > w - 10 - client.font.width(durationSuffix)) {
                sub = client.font.plainSubstrByWidth(sub, w - 16 - client.font.width(durationSuffix)) + "…";
            }
            context.drawString(client.font, sub, x + 6, y + 18, 0xFFAAAAAA, false);

            if (!durationSuffix.isEmpty()) {
                context.drawString(client.font, durationSuffix, x + w - client.font.width(durationSuffix) - 4, y + 18, 0xFF778899, false);
            }
        });
    }
}
