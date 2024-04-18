package com.coloryr.allmusic.client.hud;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.coloryr.allmusic.client.AllMusic;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HudUtils {

    public static final ComType[] types = ComType.values();
    public String info = "";
    public String list = "";
    public String lyric = "";
    public SaveOBJ save;
    private ByteBuffer byteBuffer;
    private int textureID = -1;
    public boolean haveImg;
    public final Object lock = new Object();
    private final Queue<String> urlList = new ConcurrentLinkedDeque<>();
    private final Semaphore semaphore = new Semaphore(0);
    private final HttpClient client;
    public static ConfigObj config;
    private HttpGet get;
    private InputStream inputStream;
    public boolean thisRoute;
    private int ang = 0;
    private int count = 0;

    public HudUtils(Path path) {
        Thread thread = new Thread(this::run);
        thread.setName("allmusic_pic");
        thread.start();
        client = HttpClientBuilder.create()
            .useSystemProperties()
            .build();
        File configFile = new File(path.toFile(), "allmusic.json");
        if (configFile.exists()) {
            try {
                InputStreamReader reader = new InputStreamReader(
                    Files.newInputStream(configFile.toPath()),
                    StandardCharsets.UTF_8);
                BufferedReader bf = new BufferedReader(reader);
                config = new Gson().fromJson(bf, ConfigObj.class);
                bf.close();
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (config == null) {
            config = new ConfigObj();
            config.picSize = 500;
            config.queueSize = 100;
            config.exitSize = 50;
            try {
                String data = new GsonBuilder().setPrettyPrinting()
                    .create()
                    .toJson(config);
                FileOutputStream out = new FileOutputStream(configFile);
                OutputStreamWriter write = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                write.write(data);
                write.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::time1, 0, 1, TimeUnit.MILLISECONDS);
    }

    private void time1() {
        if (save == null) return;
        if (count < save.picRotateSpeed) {
            count++;
            return;
        }
        count = 0;
        ang++;
        ang = ang % 360;
    }

    public void close() {
        haveImg = false;
        info = list = lyric = "";
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

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_AREA_AVERAGING);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics()
            .drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

    private void loadPic(String picUrl) {
        try {
            getClose();
            get = new HttpGet(picUrl);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();
            BufferedImage image = resizeImage(ImageIO.read(inputStream), config.picSize, config.picSize);
            int[] pixels = new int[image.getWidth() * image.getHeight()];
            byteBuffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

            int width = image.getWidth();
            while (save == null) {
                Thread.sleep(200);
            }
            if (save.pic.shadow) {
                // 透明底的图片
                BufferedImage formatAvatarImage = new BufferedImage(width, width, BufferedImage.TYPE_4BYTE_ABGR);
                Graphics2D graphics = formatAvatarImage.createGraphics();
                // 把图片切成一个园
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 留一个像素的空白区域，这个很重要，画圆的时候把这个覆盖
                int border = (int) (width * 0.11);
                // 图片是一个圆型
                Ellipse2D.Double shape = new Ellipse2D.Double(border, border, width - border * 2, width - border * 2);
                // 需要保留的区域
                graphics.setClip(shape);
                graphics.drawImage(image, border, border, width - border * 2, width - border * 2, null);
                graphics.dispose();
                // 在圆图外面再画一个圆
                // 新创建一个graphics，这样画的圆不会有锯齿
                graphics = formatAvatarImage.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 画笔是4.5个像素，BasicStroke的使用可以查看下面的参考文档
                // 使画笔时基本会像外延伸一定像素，具体可以自己使用的时候测试
                int border1;

                border1 = (int) (width * 0.08);
                BasicStroke s = new BasicStroke(border1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                graphics.setStroke(s);
                graphics.setColor(Color.decode("#121212"));
                graphics.drawOval(border1, border1, width - border1 * 2, width - border1 * 2);

                border1 = (int) (width * 0.05);
                float si = (float) (border1 / 6);
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

                formatAvatarImage.getRGB(
                    0,
                    0,
                    formatAvatarImage.getWidth(),
                    formatAvatarImage.getHeight(),
                    pixels,
                    0,
                    formatAvatarImage.getWidth());
                getClose();
                thisRoute = true;
            } else {
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
                GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    image.getWidth(),
                    image.getHeight(),
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    byteBuffer);
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
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
        if (save == null) return;
        synchronized (lock) {
            if (save.info.enable && !info.isEmpty()) {
                int offset = 0;
                String[] temp = info.split("\n");
                for (String item : temp) {
                    drawText(item, save.info.x, save.info.y + offset, save.info.dir, save.info.color, save.info.shadow);
                    offset += 10;
                }
            }
            if (save.list.enable && !list.isEmpty()) {
                String[] temp = list.split("\n");
                int offset = 0;
                for (String item : temp) {
                    drawText(item, save.list.x, save.list.y + offset, save.list.dir, save.list.color, save.list.shadow);
                    offset += 10;
                }
            }
            if (save.lyric.enable && !lyric.isEmpty()) {
                String[] temp = lyric.split("\n");
                int offset = 0;
                for (String item : temp) {
                    drawText(
                        item,
                        save.lyric.x,
                        save.lyric.y + offset,
                        save.lyric.dir,
                        save.lyric.color,
                        save.lyric.shadow);
                    offset += 10;
                }
            }
            if (save.pic.enable && haveImg) {
                drawPic(textureID, save.pic.color, save.pic.x, save.pic.y, save.pic.dir, ang);
            }
        }
    }

    private void drawPic(int textureID, int size, int x, int y, HudDirType dir, int ang) {
        if (dir == null) {
            return;
        }

        int screenWidth = AllMusic.getScreenWidth();
        int screenHeight = AllMusic.getScreenHeight();

        int x1 = x;
        int y1 = y;

        switch (dir) {
            case TOP_CENTER:
                x1 = screenWidth / 2 - size / 2 + x;
                break;
            case TOP_RIGHT:
                x1 = screenWidth - size - x;
                break;
            case LEFT:
                y1 = screenHeight / 2 - size / 2 + y;
                break;
            case CENTER:
                x1 = screenWidth / 2 - size / 2 + x;
                y1 = screenHeight / 2 - size / 2 + y;
                break;
            case RIGHT:
                x1 = screenWidth - size - x;
                y1 = screenHeight / 2 - size / 2 + y;
                break;
            case BOTTOM_LEFT:
                y1 = screenHeight - size - y;
                break;
            case BOTTOM_CENTER:
                x1 = screenWidth / 2 - size / 2 + x;
                y1 = screenHeight - size - y;
                break;
            case BOTTOM_RIGHT:
                x1 = screenWidth - size - x;
                y1 = screenHeight - size - y;
                break;
        }

        AllMusic.drawPic(textureID, size, x1, y1, (save.pic.shadow && thisRoute) ? ang : 0);
    }

    private void drawText(String item, int x, int y, HudDirType dir, int color, boolean shadow) {
        int width = AllMusic.getTextWidth(item);
        int height = AllMusic.getFontHeight();

        int screenWidth = AllMusic.getScreenWidth();
        int screenHeight = AllMusic.getScreenHeight();

        int x1 = x;
        int y1 = y;

        switch (dir) {
            case TOP_CENTER:
                x1 = screenWidth / 2 - width / 2 + x;
                break;
            case TOP_RIGHT:
                x1 = screenWidth - width - x;
                break;
            case LEFT:
                y1 = screenHeight / 2 - height / 2 + y;
                break;
            case CENTER:
                x1 = screenWidth / 2 - width / 2 + x;
                y1 = screenHeight / 2 - height / 2 + y;
                break;
            case RIGHT:
                x1 = screenWidth - width - x;
                y1 = screenHeight / 2 - height / 2 + y;
                break;
            case BOTTOM_LEFT:
                y1 = screenHeight - height - y;
                break;
            case BOTTOM_CENTER:
                x1 = screenWidth / 2 - width / 2 + x;
                y1 = screenHeight - height - y;
                break;
            case BOTTOM_RIGHT:
                x1 = screenWidth - width - x;
                y1 = screenHeight - height - y;
                break;
        }

        AllMusic.drawText(item, x1, y1, color, shadow);
    }
}
