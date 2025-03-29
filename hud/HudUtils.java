package com.coloryr.allmusic.client.hud;

import com.coloryr.allmusic.client.AllMusic;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
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

public class HudUtils {
    public static final ComType[] types = ComType.values();
    public static ConfigObj config;

//    public static final int GL_CLAMP_TO_EDGE = 0x812F;
//    public static final int
//            GL_TEXTURE_MIN_LOD    = 0x813A,
//            GL_TEXTURE_MAX_LOD    = 0x813B,
//            GL_TEXTURE_BASE_LEVEL = 0x813C,
//            GL_TEXTURE_MAX_LEVEL  = 0x813D;

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_AREA_AVERAGING);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        outputImage.getGraphics()
                .drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

    private final Queue<String> urlList = new ConcurrentLinkedDeque<>();
    private final Semaphore semaphore = new Semaphore(0);
    private final HttpClient client;
    private final ByteBuffer byteBuffer;

    public String info = "";
    public String list = "";
    public String lyric = "";
    public SaveOBJ save;

    private boolean haveImg;
    private boolean thisRoute;
    private int textureID = -1;
    private HttpGet get;
    private InputStream inputStream;
    private int ang = 0;
    private int count = 0;
    private boolean display;
    private boolean needUpload;
    private final Path path;

    public HudUtils(Path path) {
        this.path = path;
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
            config.picSize = 200;
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

        byteBuffer = ByteBuffer.allocateDirect(config.picSize * config.picSize * 4);

        AllMusic.runMain(this::texInit);

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::time1, 0, 1, TimeUnit.MILLISECONDS);
    }

    private void texInit() {
        textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, config.picSize,
                config.picSize, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
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

    private void loadPic(String picUrl) {
        haveImg = false;
        try {
            getClose();

            while (save == null || needUpload) {
                Thread.sleep(200);
            }
            if (!save.pic.enable) {
                return;
            }

            get = new HttpGet(picUrl);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();
            BufferedImage image = resizeImage(ImageIO.read(inputStream), config.picSize, config.picSize);
            int[] pixels = new int[config.picSize * config.picSize];

            //图片旋转
            if (save.pic.shadow) {
                // 透明底的图片
                BufferedImage formatAvatarImage = new BufferedImage(config.picSize, config.picSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = formatAvatarImage.createGraphics();
                // 把图片切成一个园
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 留一个像素的空白区域，这个很重要，画圆的时候把这个覆盖
                int border = (int) (config.picSize * 0.11);
                // 图片是一个圆型
                Ellipse2D.Double shape = new Ellipse2D.Double(border, border, config.picSize - border * 2, config.picSize - border * 2);
                // 需要保留的区域
                graphics.setClip(shape);
                graphics.drawImage(image, border, border, config.picSize - border * 2, config.picSize - border * 2, null);
                graphics.dispose();
                // 在圆图外面再画一个圆
                // 新创建一个graphics，这样画的圆不会有锯齿
                graphics = formatAvatarImage.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 画笔是4.5个像素，BasicStroke的使用可以查看下面的参考文档
                // 使画笔时基本会像外延伸一定像素，具体可以自己使用的时候测试
                int border1;

                border1 = (int) (config.picSize * 0.08);
                BasicStroke s = new BasicStroke(border1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                graphics.setStroke(s);
                graphics.setColor(Color.decode("#121212"));
                graphics.drawOval(border1, border1, config.picSize - border1 * 2, config.picSize - border1 * 2);

                border1 = (int) (config.picSize * 0.05);
                float si = (float) (border1 / 6);
                s = new BasicStroke(si, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                graphics.setStroke(s);
                graphics.setColor(Color.decode("#181818"));
                graphics.drawOval(border1, border1, config.picSize - border1 * 2, config.picSize - border1 * 2);

                border1 = (int) (config.picSize * 0.065);
                graphics.drawOval(border1, border1, config.picSize - border1 * 2, config.picSize - border1 * 2);

                border1 = (int) (config.picSize * 0.08);
                graphics.drawOval(border1, border1, config.picSize - border1 * 2, config.picSize - border1 * 2);

                border1 = (int) (config.picSize * 0.095);
                graphics.drawOval(border1, border1, config.picSize - border1 * 2, config.picSize - border1 * 2);

                graphics.dispose();

//                ImageIO.write(formatAvatarImage, "png", new File(path.toFile(), "output.png"));

                formatAvatarImage.getRGB(0, 0, config.picSize,
                        config.picSize, pixels, 0, config.picSize);
                getClose();
                thisRoute = true;
            } else {
                image.getRGB(0, 0, config.picSize, config.picSize, pixels, 0, config.picSize);
                getClose();
                thisRoute = false;
            }

            synchronized (byteBuffer) {
                for (int h = 0; h < config.picSize; h++) {
                    for (int w = 0; w < config.picSize; w++) {
                        int pixel = pixels[h * config.picSize + w];

                        byteBuffer.put((byte) ((pixel >> 16) & 0xFF));
                        byteBuffer.put((byte) ((pixel >> 8) & 0xFF));
                        byteBuffer.put((byte) (pixel & 0xFF));
                        byteBuffer.put((byte) ((pixel >> 24) & 0xFF));
                    }
                }

                byteBuffer.flip();
            }

            needUpload = true;
        } catch (Exception e) {
            e.printStackTrace();
            AllMusic.sendMessage("[AllMusic客户端]图片解析错误");
            haveImg = false;
        }
    }

    private void upload() {
        synchronized (byteBuffer) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, config.picSize,
                    config.picSize, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        haveImg = true;
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
        save = new Gson().fromJson(data, SaveOBJ.class);
    }

    public void update() {
        SaveOBJ save = this.save;
        if (save == null) return;
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
                drawText(item, save.lyric.x, save.lyric.y + offset, save.lyric.dir,
                        save.lyric.color, save.lyric.shadow);
                offset += 10;
            }
        }
        if (needUpload) {
            needUpload = false;
            if (display) {
                display = false;
                needUpload = true;
                return;
            }
            upload();
        }
        if (save.pic.enable && haveImg) {
            display = true;
            drawPic(textureID, save.pic.color, save.pic.x, save.pic.y, save.pic.dir, ang);
            display = false;
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
