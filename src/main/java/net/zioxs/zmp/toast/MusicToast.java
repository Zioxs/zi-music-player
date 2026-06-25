package net.zioxs.zmp.toast;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.util.Mth;
import net.zioxs.zmp.music.MusicEntry;
import org.jspecify.annotations.NonNull;

public class MusicToast implements Toast {

    private static final long SLIDE_MS   = 250L;
    private static final long HOLD_MS    = 3500L;
    private static final long TOTAL_MS   = SLIDE_MS + HOLD_MS + SLIDE_MS;

    private static final int W = 160;
    private static final int H = 42;
    private static final int BORDER_W = 3;
    private static final int PAD = 6;

    private static final int C_BG        = 0xF0060610;
    private static final int C_BORDER    = 0xFF1A1A36;
    private static final int C_ACCENT    = 0xFF4324B3;
    private static final int C_HEADER    = 0xFF7457E4;
    private static final int C_TITLE     = 0xFFFFFFFF;
    private static final int C_SUB       = 0xFFAAAAAA;

    public static final Object TYPE = new Object();

    private MusicEntry entry;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;
    private long myStartTime = -1;

    public MusicToast(MusicEntry entry) {
        this.entry = entry;
    }

    @Override
    public int width()  { return W; }

    @Override
    public int height() { return H; }

    @Override
    public @NonNull Visibility getWantedVisibility() {
        return this.visibility;
    }


    @Override
    public void update(ToastManager manager, long time) {
        if (myStartTime != -1 && (time - myStartTime) >= TOTAL_MS) {
            this.visibility = Toast.Visibility.HIDE;
        }
    }

    public void hide() {
        this.visibility = Toast.Visibility.HIDE;
    }

    public void updateEntry(MusicEntry entry) {
        this.entry = entry;
        this.myStartTime = -1;
        this.visibility = Toast.Visibility.SHOW;
    }

    @Override
    public Object getToken() {
        return TYPE;
    }

    @Override
    public void render(GuiGraphics context, Font textRenderer, long startTime) {
        if (myStartTime == -1) {
            myStartTime = startTime;
        }
        long elapsed = startTime - myStartTime;
        if (elapsed >= TOTAL_MS) return;

        float progress;
        if (elapsed < SLIDE_MS) {
            progress = (float) elapsed / SLIDE_MS;
        } else if (elapsed < SLIDE_MS + HOLD_MS) {
            progress = 1.0f;
        } else {
            progress = 1.0f - (float)(elapsed - SLIDE_MS - HOLD_MS) / SLIDE_MS;
        }
        float eased = 1f - (float) Math.pow(1.0 - Mth.clamp(progress, 0f, 1f), 3.0);
        float xOffset = (1f - eased) * W;

        context.pose().pushMatrix();
        context.pose().translate(xOffset, 0);

        context.fill(0, 0, W, H, C_BG);
        context.fill(0, 0, W, 1, C_BORDER);
        context.fill(W - 1, 0, W, H, C_BORDER);
        context.fill(0, H - 1, W, H, C_BORDER);
        context.fill(0, 0, BORDER_W, H, C_ACCENT);

        // Horizontal gradient
        for (int i = 0; i < 40; i++) {
            int alpha = (int) (0x1A * (1.0f - (i / 40f)));
            context.fill(BORDER_W + i, 0, BORDER_W + i + 1, H, (alpha << 24) | 0x4324B3);
        }

        int textX = BORDER_W + PAD;

        context.drawString(textRenderer, "Now Playing", textX, PAD, C_HEADER, false);

        boolean isVanilla = !net.zioxs.zmp.music.MusicPlayerManager.getInstance().isUserInitiated();
        String title = entry.title();
        if (isVanilla) {
            String tag = " [Vanilla]";
            int tagW = textRenderer.width(tag);
            if (textRenderer.width(title) + tagW > W - textX - PAD) {
                title = textRenderer.plainSubstrByWidth(title, W - textX - PAD - 6 - tagW) + "…";
            }
            context.drawString(textRenderer, title, textX, PAD + 11, C_TITLE, false);
            context.drawString(textRenderer, tag, textX + textRenderer.width(title), PAD + 11, C_HEADER, false);
        } else {
            if (textRenderer.width(title) > W - textX - PAD) {
                title = textRenderer.plainSubstrByWidth(title, W - textX - PAD - 6) + "…";
            }
            context.drawString(textRenderer, title, textX, PAD + 11, C_TITLE, false);
        }

        String sub = entry.artist() + " \u2022 " + entry.duration();
        if (textRenderer.width(sub) > W - textX - PAD) {
            sub = textRenderer.plainSubstrByWidth(sub, W - textX - PAD - 6) + "…";
        }
        context.drawString(textRenderer, sub, textX, PAD + 22, C_SUB, false);

        context.pose().popMatrix();
    }
}
