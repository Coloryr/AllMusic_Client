package coloryr.allmusic.hud;

import coloryr.allmusic.AllMusic;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
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
    public boolean thisRoute;

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
            byteBuffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

            int width = image.getWidth();
            if(save.EnablePicRotate) {
                // 透明底的图片
                BufferedImage formatAvatarImage = new BufferedImage(width, width, BufferedImage.TYPE_4BYTE_ABGR);
                Graphics2D graphics = formatAvatarImage.createGraphics();
                //把图片切成一个园
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                //留一个像素的空白区域，这个很重要，画圆的时候把这个覆盖
                int border = (int) (width * 0.11);
                //图片是一个圆型
                Ellipse2D.Double shape = new Ellipse2D.Double(border, border, width - border * 2, width - border * 2);
                //需要保留的区域
                graphics.setClip(shape);
                graphics.drawImage(image, border, border, width - border * 2, width - border * 2, null);
                graphics.dispose();
                //在圆图外面再画一个圆
                //新创建一个graphics，这样画的圆不会有锯齿
                graphics = formatAvatarImage.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                //画笔是4.5个像素，BasicStroke的使用可以查看下面的参考文档
                //使画笔时基本会像外延伸一定像素，具体可以自己使用的时候测试
                int border1;

                border1 = (int) (width * 0.08);
                BasicStroke s = new BasicStroke(border1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                graphics.setStroke(s);
                graphics.setColor(Color.decode("#121212"));
                graphics.drawOval(border1, border1, width - border1 * 2, width - border1 * 2);

                border1 = (int) (width * 0.05);
                float si =(float) (border1 / 6);
                s = new BasicStroke(si, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                graphics.setStroke(s);
                graphics.setColor(Color.decode("#181818"));
                graphics.drawOval(border1, border1, width - border1 * 2, width - border1 * 2);

                border1 = (int) (width * 0.065);
                graphics.drawOval(border1, border1, width - border1 * 2, width - border1 * 2);

                border1 = (int) (width * 0.08);
                graphics.drawOval(border1, border1, width - border1 * 2, width - border1 * 2);

                border1 = (int) (width * 0.095);
                graphics.drawOval(border1, border1, width - border1 * 2, width - border1 * 2);

                graphics.dispose();

                formatAvatarImage.getRGB(0, 0, formatAvatarImage.getWidth(), formatAvatarImage.getHeight(), pixels, 0, formatAvatarImage.getWidth());
                getClose();
                thisRoute = true;
            }
            else
            {
                image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
                getClose();
                thisRoute = false;
            }

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
            if (save.EnableInfo && !Info.isEmpty()) {
                int offset = 0;
                String[] temp = Info.split("\n");
                for (String item : temp) {
                    AllMusic.drawText(item, (float) save.Info.x, (float) save.Info.y + offset);
                    offset += 10;
                }
            }
            if (save.EnableList && !List.isEmpty()) {
                String[] temp = List.split("\n");
                int offset = 0;
                for (String item : temp) {
                    AllMusic.drawText(item, (float) save.List.x, (float) save.List.y + offset);
                    offset += 10;
                }
            }
            if (save.EnableLyric && !Lyric.isEmpty()) {
                String[] temp = Lyric.split("\n");
                int offset = 0;
                for (String item : temp) {
                    AllMusic.drawText(item, (float) save.Lyric.x, (float) save.Lyric.y + offset);
                    offset += 10;
                }
            }
            if (save.EnablePic && haveImg) {
                AllMusic.drawPic(textureID, save.PicSize, save.Pic.x, save.Pic.y);
            }
        }
    }
}
