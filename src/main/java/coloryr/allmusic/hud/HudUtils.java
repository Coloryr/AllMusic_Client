package coloryr.allmusic.hud;

import coloryr.allmusic.AllMusic;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
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
    private final HttpClient client;
    private HttpGet get;
    private InputStream inputStream;

    public HudUtils() {
        Thread thread = new Thread(this::run);
        thread.setName("allmusic_pic");
        thread.start();
        client = HttpClientBuilder.create().useSystemProperties().build();
    }

    public void close() {
        haveImg = false;
        Info = List = Lyric = "";
        getClose();
    }

    private void getClose() {
        if (get != null && !get.isAborted()) {
            get.abort();
            get = null;
        }
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPic(String picUrl) {
        try {
            getClose();
            get = new HttpGet(picUrl);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();
            BufferedImage image = ImageIO.read(inputStream);
            int[] pixels = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
            getClose();
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

            AllMusic.runMain(() -> {
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
                    AllMusic.drawText(item, (float) save.getInfo().getX(), (float) save.getInfo().getY() + offset);
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
