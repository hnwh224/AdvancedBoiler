package xyz.hnwh.advancedboiler.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sun.jna.Pointer;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.audiochannel.ClientLocationalAudioChannel;
import net.minecraft.util.math.Vec3d;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.log.LogEventListener;
import uk.co.caprica.vlcj.log.LogLevel;
import uk.co.caprica.vlcj.log.NativeLog;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.callback.AudioCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL12.*;

public class DisplayRenderer {

    private String streamUrl;
    private final int[] textureID = new int[1];
    private Thread renderThread;
    private int blockWidth = 1;
    private int blockHeight = 1;
    private int width;
    private int height;
    private ByteBuffer byteBuffer;
    private ByteBuffer debugScreenByteBuffer;
    private static final MediaPlayerFactory factory = new MediaPlayerFactory();
    private EmbeddedMediaPlayer mediaPlayer;
    private List<ClientLocationalAudioChannel> audioChannels = new ArrayList<>();
    public boolean mediaPlaybackStarted = false;
    private NativeLog nativeLog;
    public LinkedList<String> logLines = new LinkedList<>();

    public DisplayRenderer(String streamUrl, List<Vec3d> speakers, int blockWidth, int blockHeight) {
        this.streamUrl = streamUrl;
        this.blockWidth = blockWidth;
        this.blockHeight = blockHeight;
        textureID[0] = glGenTextures();
        RenderSystem.bindTexture(textureID[0]);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        RenderSystem.bindTexture(0);

        width = 1920;
        height = 1080;

        for (Vec3d speaker : speakers) {
            Position position = SimpleVoiceChatPlugin.clientApi.createPosition(
                    speaker.x,
                    speaker.y,
                    speaker.z
            );
            ClientLocationalAudioChannel audioChannel = SimpleVoiceChatPlugin.clientApi.createLocationalAudioChannel(UUID.randomUUID(), position);

            audioChannels.add(audioChannel);
        }



        byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        debugScreenByteBuffer = ByteBuffer.allocateDirect(width * height * 4);

        AdvancedboilerClient.CONFIG.subscribeToDebug(b -> {
            renderPlaceholderImage();
        });
    }

    public void startPlayback() {
        renderThread = new Thread(() -> {
            mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

            nativeLog = factory.application().newLog();
            nativeLog.setLevel(LogLevel.DEBUG);
            nativeLog.addLogListener(new LogEventListener() {
                @Override
                public void log(LogLevel logLevel, String module, String file, Integer line, String name, String header, Integer id, String message) {
                    addLogLine("[VLC] [%-10s] (%-20s): %s".formatted(logLevel.name().strip(), name, message));
                }
            });

            mediaPlayer.audio().callback("", 48000, 1, new BoilerVlcAudioCallback());
            mediaPlayer.videoSurface().set(factory.videoSurfaces().newVideoSurface(new BoilerVlcBufferFormatCallback(), new BoilerVlcRenderCallback(), true));
            mediaPlayer.media().play(streamUrl);
        });
        renderThread.start();
    }

    public int getTextureID() {
        return textureID[0];
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getBlockWidth() {
        return blockWidth;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public void stop() {
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }

        if(renderThread != null) {
            renderThread.interrupt();
        }
    }

    protected void cleanup() {
        stop();

        if (textureID[0] != 0) {
            glDeleteTextures(textureID[0]);
            textureID[0] = 0;
        }
    }

    public void paint() {
        if(!mediaPlaybackStarted) {
            byteBuffer = debugScreenByteBuffer;
        }
        onPaint(byteBuffer, width, height);
    }

    public void play(String url) {
        mediaPlayer.media().play(url);
    }

    protected void onPaint(ByteBuffer buffer, int width, int height) {
        //System.out.println("drawing");
        if (textureID[0] == 0) return;

        RenderSystem.bindTexture(textureID[0]);
        RenderSystem.pixelStore(GL_UNPACK_ROW_LENGTH, width);
        RenderSystem.pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
        RenderSystem.pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, byteBuffer);

    }

    protected void onPaint(ByteBuffer buffer, int x, int y, int width, int height) {
        glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, width, height, GL_BGRA,
                GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
    }

    private ByteBuffer cacheBuffer;

    private final class BoilerVlcRenderCallback implements RenderCallback {

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] byteBuffers, BufferFormat bufferFormat) {
            mediaPlaybackStarted = true;

            cacheBuffer = byteBuffers[0];

            if(AdvancedboilerClient.CONFIG.debug()) {

                for (int i = 0; i < width * height; i++) {
                    int topPixel = debugScreenByteBuffer.getInt(i * 4);

                    int alpha = (topPixel >> 24) & 0xFF;
                    int red = (topPixel >> 16) & 0xFF;
                    int green = (topPixel >> 8) & 0xFF;
                    int blue = topPixel & 0xFF;

                    // Swap red and green
                    int correctedPixel = (blue << 24) | (green << 16) | (red << 8) | alpha;

                    if (alpha != 0) {
                        cacheBuffer.putInt(i * 4, correctedPixel);
                    }
                }
            }

            byteBuffer.put(0, cacheBuffer, 0, width * height * 4);
        }
    }

    private final class BoilerVlcBufferFormatCallback extends BufferFormatCallbackAdapter {

        @Override
        public BufferFormat getBufferFormat(int i, int i1) {

            return new RV32BufferFormat(width, height);
        }
    }

    private Queue<Short> audioBuffer = new LinkedList<>();
    //private int delayInSamples = 9600 * 3;
    private int delayInSamples = 25000;

    private final class BoilerVlcAudioCallback extends AudioCallbackAdapter {

        @Override
        public void play(MediaPlayer mediaPlayer, Pointer samples, int sampleCount, long pts) {
            byte[] audioData = samples.getByteArray(0, sampleCount * 2);
            short[] converted = SimpleVoiceChatPlugin.clientApi.getAudioConverter().bytesToShorts(audioData);

            for (int i = 0; i < converted.length; i++) {
                audioBuffer.offer(converted[i]);
            }

            if (audioBuffer.size() >= delayInSamples) {
                // Create a temporary array to hold the samples to be played
                short[] delayedSamples = new short[sampleCount];

                // Fill the array with delayed samples from the buffer
                for (int i = 0; i < sampleCount; i++) {
                    delayedSamples[i] = audioBuffer.poll(); // Remove the oldest sample
                }

                // Play the delayed samples
                audioChannels.forEach(ch -> ch.play(delayedSamples));
            }
            //audioChannel.play(converted);
        }
    }

    public void renderPlaceholderImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        if(!mediaPlaybackStarted) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, width, height);
        }

        if(AdvancedboilerClient.CONFIG.debug()) {
            g2.setFont(new Font("Courier New", Font.BOLD, 18));

            for(int i = 0; i < logLines.size(); i++) {
                String line = logLines.get(i);
                g2.setColor(Color.WHITE);
                if(line.contains("[INFO") || line.contains("[NOTICE")) g2.setColor(Color.decode("#4287f5"));
                if(line.contains("[ERROR")) g2.setColor(Color.decode("#f54242"));
                if(line.contains("[DEBUG")) g2.setColor(Color.decode("#d742f5"));
                if(line.contains("[WARNING")) g2.setColor(Color.decode("#f59342"));
                g2.drawString(line, 10, 20 * (i + 1));
            }
        }


        if(!mediaPlaybackStarted) {
            g2.setFont(new Font("Arial", Font.BOLD, 38));
            g2.setColor(Color.WHITE);

            centeredString(g2, new Rectangle(width, height - 40), "loading...");
            centeredString(g2, new Rectangle(width, height + 40), "This could take a moment");
        }

        g2.dispose();

        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

        for(int h = 0; h < image.getHeight(); h++) {
            for(int w = 0; w < image.getWidth(); w++) {
                int pixel = pixels[h * image.getWidth() + w];

                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));

            }
        }

        buffer.flip();
        debugScreenByteBuffer = buffer;
    }

    public static void centeredString(Graphics2D g2, Rectangle rect, String text) {
        FontMetrics metrics = g2.getFontMetrics(g2.getFont());
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(text, x, y);
    }

    public void addLogLine(String line) {
        if (logLines.size() >= (int) (height / 20.0)) {
            logLines.removeFirst(); // Remove the oldest line
        }
        logLines.add(line); // Add the new line
        if(!mediaPlaybackStarted) renderPlaceholderImage();
    }
}
