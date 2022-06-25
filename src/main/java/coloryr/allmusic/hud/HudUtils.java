package coloryr.allmusic.hud;

import coloryr.allmusic.AllMusic;
import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

public class HudUtils {
    public String Info = "";
    public String List = "";
    public String Lyric = "";
    public SaveOBJ save;
    private ByteBuffer byteBuffer;
    private int textureID = -1;
    public boolean haveImg;
    public final Object lock = new Object();
    private final Queue<String> urlList = new ConcurrentLinkedDeque<>();
    private final Semaphore semaphore = new Semaphore(0);

    public HudUtils() {
        ;
        Thread thread = new Thread(this::run);
        thread.setName("allmusic_pic");
        thread.start();
    }

    public void stop() {
        haveImg = false;
        Info = List = Lyric = "";
    }

    private void loadPic(String picUrl) {
        try {
            URL url = new URL(picUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(4 * 1000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52");
            connection.setRequestProperty("Host", "music.163.com");
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            BufferedImage image = ImageIO.read(inputStream);
            int[] pixels = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
            byteBuffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

            for (int h = 0; h < image.getHeight(); h++) {
                for (int w = 0; w < image.getWidth(); w++) {
                    int pixel = pixels[h * image.getWidth() + w];

                    byteBuffer.put((byte) ((pixel >> 16) & 0xFF));
                    byteBuffer.put((byte) ((pixel >> 8) & 0xFF));
                    byteBuffer.put((byte) (pixel & 0xFF));
                    byteBuffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }

            byteBuffer.flip();
            inputStream.close();
            Minecraft.getInstance().execute(() -> {
                if (textureID == -1) {
                    textureID = GL11.glGenTextures();
                }
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);

                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST);
                haveImg = true;
            });
        } catch (Exception e) {
            e.printStackTrace();
            AllMusic.sendMessage("[AllMusic客户端]图片解析错误");
            haveImg = false;
        }
    }

    private void run() {
        while (true) {
            try {
                semaphore.acquire();
                while (!urlList.isEmpty()) {
                    String picUrl = urlList.poll();
                    if (picUrl != null) {
                        loadPic(picUrl);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setImg(String picUrl) {
        urlList.add(picUrl);
        semaphore.release();
    }

    public void setPos(String data) {
        synchronized (lock) {
            save = new Gson().fromJson(data, SaveOBJ.class);
        }
    }

    public void update() {
        if (save == null)
            return;
        synchronized (lock) {
            if (save.isEnableInfo() && !Info.isEmpty()) {
                int offset = 0;
                String[] temp = Info.split("\n");
                for (String item : temp) {
                    AllMusic.drawText(item, (float) save.getLyric().getX(), (float) save.getLyric().getY() + offset);
                    offset += 10;
                }
            }
            if (save.isEnableList() && !List.isEmpty()) {
                String[] temp = List.split("\n");
                int offset = 0;
                for (String item : temp) {
                    AllMusic.drawText(item, (float) save.getList().getX(), (float) save.getList().getY() + offset);
                    offset += 10;
                }
            }
            if (save.isEnableLyric() && !Lyric.isEmpty()) {
                String[] temp = Lyric.split("\n");
                int offset = 0;
                for (String item : temp) {
                    AllMusic.drawText(item, (float) save.getLyric().getX(), (float) save.getLyric().getY() + offset);
                    offset += 10;
                }
            }
            if (save.isEnablePic() && haveImg) {
                AllMusic.drawPic(textureID, save.getPicSize(), save.getPic().getX(), save.getPic().getY());
            }
        }
    }
}
