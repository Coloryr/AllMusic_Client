package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.render.TextureRender;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

import java.io.IOException;

public class TexRender extends TextureRender {
    private final SimpleTexture sourceTexture;

    public TexRender(String texture) {
        super(texture);
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(AllMusicClient.MODID, texture);
        sourceTexture = new SimpleTexture(location);

        try {
            TextureContents contents = sourceTexture.loadContents(Minecraft.getInstance().getResourceManager());

            width = contents.image().getWidth();
            height = contents.image().getHeight();

            sourceTexture.apply(contents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawPic(float x, float y, float alpha) {
        Matrix3x2fStack matrix = new Matrix3x2fStack();

        int w1 = width / 2;
        int h1 = height / 2;

        matrix.translation(x + w1, y + h1);

        int x0 = -w1;
        int x1 = w1;
        int y0 = -h1;
        int y1 = h1;
        float u0 = 0;
        float u1 = 1;
        float v0 = 0;
        float v1 = 1;

        int color = 0xFFFFFF00 + (int) (255 * alpha);

        AllMusicClient.context.guiRenderState.submitGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(sourceTexture.getTextureView()), matrix, x0, y0, x1, y1, u0, u1, v0, v1, color, AllMusicClient.context.scissorStack.peek()));
    }

    @Override
    public void drawPic(float x, float y, float width, float alpha) {
        Matrix3x2fStack matrix = new Matrix3x2fStack();

        int w1 = (int) (((float) this.width / 2) * width);
        int h1 = height / 2;

        matrix.translation(x + w1, y + h1);

        int x0 = -w1;
        int x1 = w1;
        int y0 = -h1;
        int y1 = h1;
        float u0 = 0;
        float u1 = width;
        float v0 = 0;
        float v1 = 1;

        int color = 0xFFFFFF00 + (int) (255 * alpha);

        AllMusicClient.context.guiRenderState.submitGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(sourceTexture.getTextureView()), matrix, x0, y0, x1, y1, u0, u1, v0, v1, color, AllMusicClient.context.scissorStack.peek()));
    }
}
