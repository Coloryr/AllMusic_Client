package com.coloryr.allmusic.client.core;

import com.coloryr.allmusic.client.core.render.PictureFrameBuffer;
import com.coloryr.allmusic.client.core.render.TextFrameBuffer;
import com.coloryr.allmusic.codec.HudDirType;
import com.coloryr.allmusic.codec.HudPosObj;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
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
     * 图片buffer
     */
    private byte[] sourceImage;
    /**
     * 图片buffer
     */
    private byte[] rotateImage;

    /**
     * 图片大小
     */
    private final int size;

    //显示信息
    private String info = "";
    private String list = "";
    private String lyric = "";
    private String lyricKtv = "";

    public float lyric_state = 0;

    public HudPosObj save;

    /**
     * 图片渲染
     */
    private final PictureFrameBuffer picRender;

    /**
     * 文字渲染
     */
    private final TextFrameBuffer infoRender;
//    private final TextFrameBuffer list_buffer;
//
//    private final TextFrameBuffer lyric_buffer;
//    private final TextFrameBuffer lyric_tran_buffer;

    /**
     * 是否有图片
     */
    private boolean haveImg;

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
    private boolean imageNeedUpload;
    /**
     * 是否需要文字
     */
    private boolean infoNeedUpdate;

    public AllMusicHud(int size) {
        this.size = size;

        Thread thread = new Thread(this::run);
        thread.setName("allmusic_pic");
        thread.start();

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::picRotateTick, 0, 1, TimeUnit.MILLISECONDS);

        picRender = AllMusicCore.bridge.makePictureRender(size);

        infoRender = AllMusicCore.bridge.makeTextRender();
//        list_buffer = AllMusicCore.bridge.makeTextRender();
//
//        lyric_buffer = AllMusicCore.bridge.makeTextRender();
//        lyric_tran_buffer = AllMusicCore.bridge.makeTextRender();
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_AREA_AVERAGING);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        outputImage.getGraphics()
                .drawImage(resultingImage, 0, 0, null);
        return outputImage;
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
    }

    /**
     * 加载图片
     *
     * @param picUrl 加载地址
     */
    private void loadPic(String picUrl) {
        haveImg = false;
        try {
            while (save == null || imageNeedUpload) {
                Thread.sleep(200);
            }
            if (!save.pic.enable) {
                return;
            }

            HttpGet request = new HttpGet(picUrl);
            BufferedImage image = AllMusicCore.client.execute(request, (response -> {
                InputStream inputStream = response.getEntity().getContent();
                return resizeImage(ImageIO.read(inputStream), size, size);
            }));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            sourceImage = outputStream.toByteArray();

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

            outputStream = new ByteArrayOutputStream();
            ImageIO.write(formatAvatarImage, "png", outputStream);
            rotateImage = outputStream.toByteArray();

            request.clear();
            imageNeedUpload = true;
        } catch (Exception e) {
            e.printStackTrace();
            AllMusicCore.bridge.sendMessage("图片解析错误");
            haveImg = false;
        }
    }

    /**
     * 更新材质
     */
    private void updateTexture() {
        picRender.updatePic(sourceImage, rotateImage);
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
     *
     * @param picUrl 图片链接
     */
    public void setImg(String picUrl) {
        urlList.add(picUrl);
        semaphore.release();
    }

    /**
     * 设置位置信息
     *
     * @param save 位置信息
     */
    public void setPos(HudPosObj save) {
        this.save = save;
    }

    /**
     * 显示更新
     */
    public void update() {
        //复制一份防止突然替换
        HudPosObj save = this.save;
        if (save == null) return;
        if (infoNeedUpdate && !info.isEmpty()) {
            int offset = 0;
            String[] temp = info.split("\n");

            int height = AllMusicCore.bridge.getFontHeight();
            int allHeight = (height + 10) * temp.length;
            int allWidth = 0;

            for (String item : temp) {
                if (item.isEmpty()) {
                    continue;
                }
                allWidth = Math.max(allWidth, AllMusicCore.bridge.getTextWidth(item));
            }

            infoRender.resize(allWidth, allHeight);
            infoRender.use();
            for (String item : temp) {
                if (item.isEmpty()) {
                    continue;
                }
                infoRender.drawText(item, offset, save.info.color, save.info.shadow);
                offset += 10;
            }
            infoRender.unUse();
            infoNeedUpdate = false;
        }

        if (save.info.enable) {
            infoRender.draw(save.info.alpha, save.info.x, save.info.y, 0, 0);
        }
//        if (save.list.enable && !list.isEmpty()) {
//            String[] temp = list.split("\n");
//            int offset = 0;
//            for (String item : temp) {
//                if (item.isEmpty()) {
//                    offset += 10;
//                    continue;
//                }
//                int width = AllMusicCore.bridge.getTextWidth(item);
//                int height = AllMusicCore.bridge.getFontHeight();
//
//                list_buffer.resize(width, height);
//                list_buffer.use();
//
//                AllMusicCore.bridge.drawText(item, save.list.color, save.list.shadow);
//
//                drawText(list_buffer, width, height, save.list.x, save.list.y + offset,
//                        save.list.dir, save.list.alpha);
//
//                list_buffer.unUse();
//
//                offset += 10;
//            }
//        }
//        if (save.lyric.enable && !lyric.isEmpty()) {
//            String[] temp = lyric.split("\n");
//            int offset = 0;
//            for (String item : temp) {
//                if (item.isEmpty()) {
//                    offset += 10;
//                    continue;
//                }
//                int width = AllMusicCore.bridge.getTextWidth(item);
//                int height = AllMusicCore.bridge.getFontHeight();
//
//                lyric_buffer.resize(width, height);
//                lyric_buffer.use();
//
//                AllMusicCore.bridge.drawText(item, save.lyric.color, save.lyric.shadow);
//
//                drawText(lyric_buffer, width, height, save.lyric.x, save.lyric.y + offset,
//                        save.lyric.dir, save.lyric.alpha);
//
//                lyric_buffer.unUse();
//
//                offset += 10;
//            }
//        }
        //需要更新材质
        if (imageNeedUpload) {
            imageNeedUpload = false;
            //是否正在渲染中，多线程渲染需要做这个判断
            if (display) {
                display = false;
                imageNeedUpload = true;
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
     *
     * @param size 渲染大小
     * @param x    X坐标
     * @param y    Y坐标
     * @param dir  对齐方式
     * @param ang  旋转角度
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

        picRender.drawPic(save.pic.shadow, size, x1, y1, ang, save.pic.alpha);
    }

    /**
     * 显示文字内容
     *
     * @param fb   内容
     * @param x      X坐标
     * @param y      Y坐标
     * @param dir    对齐方式
     * @param alpha 透明度
     */
    private void drawText(TextFrameBuffer fb, String text, int width, int height, int x, int y, HudDirType dir, float alpha) {
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

//        fb.draw(text, alpha, x1, y1, width, height);
    }

    public void setInfo(String info) {
        this.info = info;

        infoNeedUpdate = true;
    }

    public void setList(String list) {
        this.list = list;
    }

    public void setLyric(String lyric) {
        this.lyric = lyric;
    }

    public void setLyricKtv(String ktv) {
        this.lyricKtv = ktv;
    }
}
