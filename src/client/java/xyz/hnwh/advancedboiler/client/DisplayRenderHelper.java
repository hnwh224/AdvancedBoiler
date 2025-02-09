package xyz.hnwh.advancedboiler.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;

public class DisplayRenderHelper {

    public static void renderDisplays(WorldRenderContext context) {
        Matrix4f positionMatrix = context.positionMatrix();
        Vec3d playerPos = context.camera().getPos();
        Tessellator tessellator = Tessellator.getInstance();

        for(BoilerDisplay display : AdvancedboilerClient.displayManager.getDisplays()) {


            BufferBuilder buffer;

            if(display.getStreamState().equals(StreamState.ONLINE)) {
                display.getRenderer().paint();
                RenderSystem.setShaderTexture(0, display.getRenderer().getTextureID());
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
                buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            } else {
                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();

            Vec3i pos1 = display.getPos1();
            Vec3i pos2 = display.getPos2();

            // Extract coordinates
            float x1 = pos1.getX();
            float y1 = pos1.getY();
            float z1 = pos1.getZ();

            float x2 = pos2.getX();
            float y2 = pos2.getY();
            float z2 = pos2.getZ();

            // Calculate the min and max for each axis
            float minX = Math.min(x1, x2);
            float maxX = Math.max(x1, x2);
            float minY = Math.min(y1, y2);
            float maxY = Math.max(y1, y2);
            float minZ = Math.min(z1, z2);
            float maxZ = Math.max(z1, z2);

            boolean mirror = false;

            switch (display.getDirection()) {
                case 0 -> {
                    maxZ += 0.9f;
                    minZ += 0.9f;

                    maxX += 1;
                    maxY += 1;
                }
                case 1 -> {
                    maxX += 0.1f;
                    minX += 0.1f;

                    maxY += 1;
                    maxZ += 1;
                }
                case 2 -> {
                    maxZ += 0.1f;
                    minZ += 0.1f;

                    maxX += 1;
                    maxY += 1;
                    mirror = true;
                }
                case 3 -> {
                    maxX += 0.9f;
                    minX += 0.9f;

                    maxY += 1;
                    maxZ += 1;
                    mirror = true;
                }
                case 4 -> {
                    maxY += 0.1f;
                    minY += 0.1f;

                    maxX += 1;
                    maxZ += 1;
                }
                case 5 -> {
                    maxY += 0.9f;
                    minY += 0.9f;

                    maxX += 1;
                    maxZ += 1;

                    mirror = true;
                }
            }

            if(!display.getStreamState().equals(StreamState.ONLINE)) {
                if (minY == maxY) {
                    buffer.vertex(positionMatrix, minX - (float) playerPos.x, minY - (float) playerPos.y, minZ - (float) playerPos.z).color(255, 0, 0, 255);
                    buffer.vertex(positionMatrix, minX - (float) playerPos.x, minY - (float) playerPos.y, maxZ - (float) playerPos.z).color(0, 255, 0, 255);
                    buffer.vertex(positionMatrix, maxX - (float) playerPos.x, minY - (float) playerPos.y, maxZ - (float) playerPos.z).color(0, 0, 255, 255);
                    buffer.vertex(positionMatrix, maxX - (float) playerPos.x, minY - (float) playerPos.y, minZ - (float) playerPos.z).color(255, 255, 0, 255);
                } else {
                    buffer.vertex(positionMatrix, (float) (minX - playerPos.x), (float) (minY - playerPos.y), (float) (minZ - playerPos.z)).color(0xFF0000FF);
                    buffer.vertex(positionMatrix, (float) (minX - playerPos.x), (float) (maxY - playerPos.y), (float) (minZ - playerPos.z)).color(0xFF00FF00);
                    buffer.vertex(positionMatrix, (float) (maxX - playerPos.x), (float) (maxY - playerPos.y), (float) (maxZ - playerPos.z)).color(0xFFFF0000);
                    buffer.vertex(positionMatrix, (float) (maxX - playerPos.x), (float) (minY - playerPos.y), (float) (maxZ - playerPos.z)).color(0xFFFF00FF);
                }
            } else {
                if (minY == maxY) {
                    Vec2f uv = rotateUV(new Vec2f(1.0f, 1.0f), display.getRotation(), mirror, false);
                    buffer.vertex(positionMatrix, minX - (float) playerPos.x, minY - (float) playerPos.y, minZ - (float) playerPos.z).texture(uv.x, uv.y).color(255, 0, 0, 255);
                    uv = rotateUV(new Vec2f(1.0f, 0.0f), display.getRotation(), mirror, false);
                    buffer.vertex(positionMatrix, minX - (float) playerPos.x, minY - (float) playerPos.y, maxZ - (float) playerPos.z).texture(uv.x, uv.y).color(0, 255, 0, 255);
                    uv = rotateUV(new Vec2f(0.0f, 0.0f), display.getRotation(), mirror, false);
                    buffer.vertex(positionMatrix, maxX - (float) playerPos.x, minY - (float) playerPos.y, maxZ - (float) playerPos.z).texture(uv.x, uv.y).color(0, 0, 255, 255);
                    uv = rotateUV(new Vec2f(0.0f, 1.0f), display.getRotation(), mirror, false);
                    buffer.vertex(positionMatrix, maxX - (float) playerPos.x, minY - (float) playerPos.y, minZ - (float) playerPos.z).texture(uv.x, uv.y).color(255, 255, 0, 255);
                } else {
                    Vec2f uv = rotateUV(new Vec2f(1.0f, 1.0f), display.getRotation(), mirror, false);
                    buffer.vertex(positionMatrix, (float) (minX - playerPos.x), (float) (minY - playerPos.y), (float) (minZ - playerPos.z)).texture(uv.x, uv.y).color(0xFF0000FF);
                    uv = rotateUV(new Vec2f(1.0f, 0.0f), display.getRotation(), mirror, false);
                    buffer.vertex(positionMatrix, (float) (minX - playerPos.x), (float) (maxY - playerPos.y), (float) (minZ - playerPos.z)).texture(uv.x, uv.y).color(0xFF00FF00);
                    uv = rotateUV(new Vec2f(0.0f, 0.0f), display.getRotation(), mirror, false);
                    buffer.vertex(positionMatrix, (float) (maxX - playerPos.x), (float) (maxY - playerPos.y), (float) (maxZ - playerPos.z)).texture(uv.x, uv.y).color(0xFFFF0000);
                    uv = rotateUV(new Vec2f(0.0f, 1.0f), display.getRotation(), mirror, false);
                    buffer.vertex(positionMatrix, (float) (maxX - playerPos.x), (float) (minY - playerPos.y), (float) (maxZ - playerPos.z)).texture(uv.x, uv.y).color(0xFFFF00FF);
                }
            }


            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    public static Vec2f rotateUV(Vec2f uv, int rotation, boolean mirrorHorizontally, boolean mirrorVertically) {
        // Rotate UV coordinates
        Vec2f rotatedUV = rotateUV(uv, rotation);

        // Mirror if needed
        float u = rotatedUV.x;
        float v = rotatedUV.y;

        if (mirrorHorizontally) {
            u = 1.0f - u;
        }
        if (mirrorVertically) {
            v = 1.0f - v;
        }

        return new Vec2f(u, v);
    }

    public static Vec2f rotateUV(Vec2f uv, int rotation) {
        // Normalize rotation to a range of 0 to 3
        rotation = ((rotation % 4) + 4) % 4;

        // Extract original U and V values
        float u = uv.x;
        float v = uv.y;

        // Rotate based on the step
        switch (rotation) {
            case 0: // 0 degrees (no rotation)
                return new Vec2f(u, v);
            case 1: // 90 degrees clockwise
                return new Vec2f(1.0f - v, u);
            case 2: // 180 degrees
                return new Vec2f(1.0f - u, 1.0f - v);
            case 3: // 270 degrees clockwise (or 90 degrees counterclockwise)
                return new Vec2f(v, 1.0f - u);
            default:
                throw new IllegalArgumentException("Invalid rotation value: " + rotation);
        }
    }
}
