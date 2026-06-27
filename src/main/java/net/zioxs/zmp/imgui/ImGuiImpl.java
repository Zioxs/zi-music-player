/*
 * This file is part of fabric-imgui-example-mod - https://github.com/florianreuth/fabric-imgui-example-mod
 * by Florian Reuth and contributors
 */
package net.zioxs.zmp.imgui;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import imgui.*;
import imgui.extension.implot.ImPlot;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

public final class ImGuiImpl {

    private final static ImGuiImplGlfw imGuiImplGlfw = new ImGuiImplGlfw();
    private final static ImGuiImplGl3 imGuiImplGl3 = new ImGuiImplGl3();

    private static short[] glyphRanges;

    public static void create(final long handle) {
        ImGui.createContext();
        ImPlot.createContext();

        final ImGuiIO data = ImGui.getIO();
        data.setIniFilename("zmp.ini"); // TODO; Change this to your modid

        // If you want to have custom fonts, you can use the following code here
//        minecraftFont = loadFont("/fonts/minecraft.ttf", 16);
//        In ImGui windows, you can set the font like this:
//        ImGui.pushFont(defaultFont);
//        ImGui.popFont();

        data.setConfigFlags(ImGuiConfigFlags.DockingEnable);

        EditorStyle.apply(ImGui.getStyle());

        // In case you want to enable Viewports on Windows, replace the line above with this one:
        //data.setConfigFlags(ImGuiConfigFlags.DockingEnable | ImGuiConfigFlags.ViewportsEnable);

//        imGuiImplGlfw.init(handle, true);
//        imGuiImplGl3.init();
        invokeBackend(imGuiImplGlfw, "init", new Class<?>[]{ long.class, boolean.class }, handle, false);
        invokeBackend(imGuiImplGl3, "init", new Class<?>[]{});

    }

    public static void beginImGuiRendering() {
        // Minecraft will not bind the framebuffer unless it is needed, so do it manually and hope Vulcan never gets real:tm:
        final RenderTarget framebuffer = Minecraft.getInstance().getMainRenderTarget();
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, ((GlTexture) framebuffer.getColorTexture()).getFbo(((GlDevice) RenderSystem.getDevice()).directStateAccess(), null));
        GL11C.glViewport(0, 0, framebuffer.width, framebuffer.height);

        imGuiImplGl3.newFrame();
        imGuiImplGlfw.newFrame(); // Handle keyboard and mouse interactions
        ImGui.newFrame();
    }

    public static void endImGuiRendering() {
        ImGui.render();
        imGuiImplGl3.renderDrawData(ImGui.getDrawData());

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long pointer = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();

            GLFW.glfwMakeContextCurrent(pointer);
        }
    }

    /**
     * Loads a font from the given path with the specified pixel size.
     *
     * @param path      The path to the font file.
     * @param pixelSize The desired pixel size of the font.
     * @return The loaded ImFont instance.
     */

    public static ImFont loadFontWithIcons(final String path, final String iconPath, final int pixelSize) {
        final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
        rangesBuilder.addRanges(ImGui.getIO().getFonts().getGlyphRangesDefault());
        rangesBuilder.addRanges(ImGui.getIO().getFonts().getGlyphRangesCyrillic());
        rangesBuilder.addRanges(ImGui.getIO().getFonts().getGlyphRangesJapanese());
        glyphRanges = rangesBuilder.buildRanges();

        final ImFontConfig config = new ImFontConfig();
        config.setGlyphRanges(glyphRanges);

        ImFont font;
        try (final InputStream in = Objects.requireNonNull(ImGuiImpl.class.getResourceAsStream(path))) {
            final byte[] fontData = IOUtils.toByteArray(in);
            font = ImGui.getIO().getFonts().addFontFromMemoryTTF(fontData, pixelSize, config);
        } catch (final IOException e) {
            config.destroy();
            throw new UncheckedIOException("Failed to load font from path: " + path, e);
        }
        config.destroy();

        // Load Icon Font and Merge
        final ImFontConfig iconConfig = new ImFontConfig();
        iconConfig.setMergeMode(true);
        iconConfig.setGlyphMinAdvanceX((float) pixelSize);
        // FontAwesome Solid range
        iconConfig.setGlyphRanges(new short[] { (short) 0xe000, (short) 0xf8ff, 0 });

        try (final InputStream in = Objects.requireNonNull(ImGuiImpl.class.getResourceAsStream(iconPath))) {
            final byte[] iconData = IOUtils.toByteArray(in);
            ImGui.getIO().getFonts().addFontFromMemoryTTF(iconData, pixelSize, iconConfig);
        } catch (final IOException e) {
            iconConfig.destroy();
            throw new UncheckedIOException("Failed to load icon font from path: " + iconPath, e);
        }
        iconConfig.destroy();

        ImGui.getIO().getFonts().build();

        // Return the merged font (which is the primary one)
        return font;
    }

    public static ImFont loadFont(final String path, final int pixelSize) {
//        if (glyphRanges == null) {
            final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();

            rangesBuilder.addRanges(ImGui.getIO().getFonts().getGlyphRangesDefault());
            rangesBuilder.addRanges(ImGui.getIO().getFonts().getGlyphRangesCyrillic());
            rangesBuilder.addRanges(ImGui.getIO().getFonts().getGlyphRangesJapanese());

            glyphRanges = rangesBuilder.buildRanges();
//        }

        final ImFontConfig config = new ImFontConfig();
        config.setGlyphRanges(glyphRanges);
        try (final InputStream in = Objects.requireNonNull(ImGuiImpl.class.getResourceAsStream(path))) {
            final byte[] fontData = IOUtils.toByteArray(in);
            return ImGui.getIO().getFonts().addFontFromMemoryTTF(fontData, pixelSize, config);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to load font from path: " + path, e);
        } finally {
            config.destroy();
        }
    }

    public static void dispose() {
        imGuiImplGl3.shutdown();
        imGuiImplGlfw.shutdown();

        ImPlot.destroyContext();
        ImGui.destroyContext();
    }

    private static boolean invokeBackend(Object backend, String name, Class<?>[] paramTypes, Object... args)
    {
        java.lang.reflect.Method method;
        try
        {
            method = backend.getClass().getMethod(name, paramTypes);
        }
        catch (NoSuchMethodException absent)
        {
            return false;
        }

        try
        {
            method.invoke(backend, args);
            return true;
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("imgui backend " + name + " is not accessible", e);
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new IllegalStateException("imgui backend " + name + " failed", cause);
        }
    }

}
