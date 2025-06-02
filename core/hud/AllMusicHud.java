package com.coloryr.allmusic.client.core.hud;

import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.objs.SaveOBJ;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * AllMusic信息显示
 */
public class AllMusicHud {
    /**
     * 更新图片的链接
     */
    private final Queue<String> urlList = new ConcurrentLinkedDeque<>();
    /**
     * 更新锁，更新在子线程进行
     */
    private final Semaphore semaphore = new Semaphore(0);
    /**
     * 获取Http客户端
     */
    private final HttpClient client;
    /**
     * 图片buffer
     */
    private final ByteBuffer byteBuffer;

    //显示信息
    public String info = "";
    public String list = "";
    public String lyric = "";
    public SaveOBJ save;

    /**
     * 是否有图片
     */
    private boolean haveImg;
    /**
     * 是否为旋转图片
     */
    private boolean thisRoute;

    //请求
    private HttpGet get;
    private InputStream inputStream;

    /**
     * 旋转角度
     */
    private int ang = 0;
    private int count = 0;

    /**
     * 是否显示
     */
    private boolean display;
    /**
     * 是否需要更新材质
     */
    private boolean needUpload;

    /**
     * 游戏内贴图
     */
    private final Object texture;
    /**
     * 图片大小
     */
    private final int size;
    
    public AllMusicHud(int size) {
        this.size = size;
        
        Thread thread = new Thread(this::run);
        thread.setName("allmusic_pic");
        thread.start();
        client = HttpClientBuilder.create().useSystemProperties().build();
        byteBuffer = ByteBuffer.allocateDirect(size * size * 4);
        texture = AllMusicCore.bridge.genTexture(size);
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::picRotateTick, 0, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * 图片旋转计数器
     */
    private void picRotateTick() {
        if (save == null) return;
        if (count < save.picRotateSpeed) {
            count++;
            return;
        }
        count = 0;
        ang++;
        ang = ang % 360;
    }

    /**
     * 清理显示
     */
    public void close() {
        haveImg = false;
        info = list = lyric = "";
        getClose();
    }

    /**
     * 网络请求取消
     */
    private void getClose() {
        try {
            if (get != null && !get.isAborted()) {
                get.abort();
                get = null;
            }
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载图片
     * @param picUrl 加载地址
     */
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
            BufferedImage image = resizeImage(ImageIO.read(inputStream), size, size);
            int[] pixels = new int[size * size];

            //图片旋转
            if (save.pic.shadow) {
                // 透明底的图片
                BufferedImage formatAvatarImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = formatAvatarImage.createGraphics();
                // 把图片切成一个园
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 留一个像素的空白区域，这个很重要，画圆的时候把这个覆盖
                int border = (int) (size * 0.11);
                // 图片是一个圆型
                Ellipse2D.Double shape = new Ellipse2D.Double(border, border, size - border * 2, size - border * 2);
                // 需要保留的区域
                graphics.setClip(shape);
                graphics.drawImage(image, border, border, size - border * 2, size - border * 2, null);
                graphics.dispose();
                // 在圆图外面再画一个圆
                // 新创建一个graphics，这样画的圆不会有锯齿
                graphics = formatAvatarImage.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 画笔是4.5个像素，BasicStroke的使用可以查看下面的参考文档
                // 使画笔时基本会像外延伸一定像素，具体可以自己使用的时候测试
                int border1;

                border1 = (int) (size * 0.08);
                BasicStroke s = new BasicStroke(border1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                graphics.setStroke(s);
                graphics.setColor(Color.decode("#121212"));
                graphics.drawOval(border1, border1, size - border1 * 2, size - border1 * 2);

                border1 = (int) (size * 0.05);
                float si = (float) (border1 / 6);
                s = new BasicStroke(si, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                graphics.setStroke(s);
                graphics.setColor(Color.decode("#181818"));
                graphics.drawOval(border1, border1, size - border1 * 2, size - border1 * 2);

                border1 = (int) (size * 0.065);
                graphics.drawOval(border1, border1, size - border1 * 2, size - border1 * 2);

                border1 = (int) (size * 0.08);
                graphics.drawOval(border1, border1, size - border1 * 2, size - border1 * 2);

                border1 = (int) (size * 0.095);
                graphics.drawOval(border1, border1, size - border1 * 2, size - border1 * 2);

                graphics.dispose();

//                ImageIO.write(formatAvatarImage, "png", new File(path.toFile(), "output.png"));

                formatAvatarImage.getRGB(0, 0, size,
                        size, pixels, 0, size);
                getClose();
                thisRoute = true;
            } else {
                image.getRGB(0, 0, size, size, pixels, 0, size);
                getClose();
                thisRoute = false;
            }

            synchronized (byteBuffer) {
                for (int h = 0; h < size; h++) {
                    for (int w = 0; w < size; w++) {
                        int pixel = pixels[h * size + w];

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
            AllMusicCore.bridge.sendMessage("[AllMusic客户端]图片解析错误");
            haveImg = false;
        }
    }

    /**
     * 更新材质
     */
    private void updateTexture() {
        synchronized (byteBuffer) {
            AllMusicCore.bridge.updateTexture(texture, size, byteBuffer);
        }
        haveImg = true;
    }

    /**
     * 网络线程
     */
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

    /**
     * 设置下一张图片
     * @param picUrl 图片链接
     */
    public void setImg(String picUrl) {
        urlList.add(picUrl);
        semaphore.release();
    }

    /**
     * 设置位置信息
     * @param save 位置信息
     */
    public void setPos(SaveOBJ save) {
        this.save = save;
    }

    /**
     * 显示更新
     */
    public void update() {
        //复制一份防止突然替换
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
        //需要更新材质
        if (needUpload) {
            needUpload = false;
            //是否正在渲染中，多线程渲染需要做这个判断
            if (display) {
                display = false;
                needUpload = true;
                return;
            }
            updateTexture();
        }
        //绘制图片
        if (save.pic.enable && haveImg) {
            display = true;
            drawPic(save.pic.color, save.pic.x, save.pic.y, save.pic.dir, ang);
            display = false;
        }
    }

    /**
     * 绘制图片
     * @param size 渲染大小
     * @param x X坐标
     * @param y Y坐标
     * @param dir 对齐方式
     * @param ang 旋转角度
     */
    private void drawPic(int size, int x, int y, HudDirType dir, int ang) {
        if (dir == null) {
            return;
        }

        int screenWidth = AllMusicCore.bridge.getScreenWidth();
        int screenHeight = AllMusicCore.bridge.getScreenHeight();

        //默认左上角
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

        AllMusicCore.bridge.drawPic(texture, size, x1, y1, (save.pic.shadow && thisRoute) ? ang : 0);
    }

    /**
     * 显示文字内容
     * @param item 内容
     * @param x X坐标
     * @param y Y坐标
     * @param dir 对齐方式
     * @param color 显示颜色
     * @param shadow 是否带阴影
     */
    private void drawText(String item, int x, int y, HudDirType dir, int color, boolean shadow) {
        int width = AllMusicCore.bridge.getTextWidth(item);
        int height = AllMusicCore.bridge.getFontHeight();

        int screenWidth = AllMusicCore.bridge.getScreenWidth();
        int screenHeight = AllMusicCore.bridge.getScreenHeight();

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

        AllMusicCore.bridge.drawText(item, x1, y1, color, shadow);
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_AREA_AVERAGING);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        outputImage.getGraphics()
                .drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }
}
