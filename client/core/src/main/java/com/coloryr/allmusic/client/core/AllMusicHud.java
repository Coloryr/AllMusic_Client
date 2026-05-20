package com.coloryr.allmusic.client.core;

import com.coloryr.allmusic.client.core.render.PictureFrameBuffer;
import com.coloryr.allmusic.client.core.render.TextFrameBuffer;
import com.coloryr.allmusic.codec.HudPosType;
import com.coloryr.allmusic.codec.HudPosObj;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private String lyricTran = "";
    private String lyricKtv = "";

    public float lyricState = 0;

    private HudPosObj save;

    /**
     * 图片渲染
     */
    private final PictureFrameBuffer picRender;

    /**
     * 文字渲染
     */
    private final TextFrameBuffer infoRender;
    private final TextFrameBuffer listRender;
    private final TextFrameBuffer lyricRender;
    private final TextFrameBuffer lyricTranRender;
    private final TextFrameBuffer lyricKtvRender;

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
     * 是否需要更新材质
     */
    private boolean imageNeedUpload;
    /**
     * 是否需要文字
     */
    private boolean infoNeedUpdate;
    private boolean listNeedUpdate;
    private boolean lyricNeedUpdate;

    public AllMusicHud(int size) {
        this.size = size;

        Thread thread = new Thread(this::run);
        thread.setName("allmusic_pic");
        thread.start();

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::picRotateTick, 0, 1, TimeUnit.MILLISECONDS);

        picRender = AllMusicCore.bridge.makePictureRender(size);

        infoRender = AllMusicCore.bridge.makeTextRender();
        listRender = AllMusicCore.bridge.makeTextRender();
        lyricRender = AllMusicCore.bridge.makeTextRender();
        lyricTranRender = AllMusicCore.bridge.makeTextRender();
        lyricKtvRender = AllMusicCore.bridge.makeTextRender();
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
        if (count < save.pic.speed) {
            count++;
            return;
        }
        count = 0;
        ang++;
        ang = ang % 360;

        infoRender.tick();
    }

    /**
     * 清理显示
     */
    public void close() {
        clear();
        save = null;
    }

    public void clear() {
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

            BufferedImage formatAvatarImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = formatAvatarImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int border = (int) (size * 0.11);
            Ellipse2D.Double shape = new Ellipse2D.Double(border, border, size - border * 2, size - border * 2);
            graphics.setClip(shape);
            graphics.drawImage(image, border, border, size - border * 2, size - border * 2, null);
            graphics.dispose();
            graphics = formatAvatarImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        infoNeedUpdate = true;
        listNeedUpdate = true;
        lyricNeedUpdate = true;
    }

    /**
     * 显示更新
     */
    public void update() {
        if (save == null) return;

        // 需要更新文字渲染
        if (infoNeedUpdate) {
            if (!info.isEmpty()) {
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
            } else {
                infoRender.use();
                infoRender.unUse();
            }

            infoNeedUpdate = false;
        }

        if (listNeedUpdate) {
            if (!list.isEmpty()) {
                int offset = 0;
                String[] temp = list.split("\n");

                int height = AllMusicCore.bridge.getFontHeight();
                int allHeight = (height + 10) * temp.length;
                int allWidth = 0;

                for (String item : temp) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    allWidth = Math.max(allWidth, AllMusicCore.bridge.getTextWidth(item));
                }

                listRender.resize(allWidth, allHeight);
                listRender.use();
                for (String item : temp) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    listRender.drawText(item, offset, save.list.color, save.list.shadow);
                    offset += 10;
                }
                listRender.unUse();

            } else {
                listRender.use();
                listRender.unUse();
            }

            listNeedUpdate = false;
        }

        if (lyricNeedUpdate) {
            int offset = 0;
            if (!lyric.isEmpty()) {
                String[] temp = lyric.split("\n");

                int height = AllMusicCore.bridge.getFontHeight();
                int allHeight = (height + 10) * temp.length;
                int allWidth = 0;

                for (String item : temp) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    allWidth = Math.max(allWidth, AllMusicCore.bridge.getTextWidth(item));
                }

                lyricRender.resize(allWidth, allHeight);
                lyricRender.use();
                for (String item : temp) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    lyricRender.drawText(item, offset, save.lyric.color, save.lyric.shadow);
                    offset += 10;
                }
                lyricRender.unUse();
            } else {
                lyricRender.use();
                lyricRender.unUse();
            }

            if (!lyricTran.isEmpty()) {
                offset = 0;
                String[] temp = lyricTran.split("\n");

                int height = AllMusicCore.bridge.getFontHeight();
                int allHeight = (height + 10) * temp.length;
                int allWidth = 0;

                for (String item : temp) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    allWidth = Math.max(allWidth, AllMusicCore.bridge.getTextWidth(item));
                }

                lyricTranRender.resize(allWidth, allHeight);
                lyricTranRender.use();
                for (String item : temp) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    lyricTranRender.drawText(item, offset, save.lyric.color, save.lyric.shadow);
                    offset += 10;
                }
                lyricTranRender.unUse();
            } else {
                lyricTranRender.use();
                lyricTranRender.unUse();
            }

            if (!lyricKtv.isEmpty()) {
                offset = 0;
                String[] temp = lyricKtv.split("\n");

                int height = AllMusicCore.bridge.getFontHeight();
                int allHeight = (height + 10) * temp.length;
                int allWidth = 0;

                for (String item : temp) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    allWidth = Math.max(allWidth, AllMusicCore.bridge.getTextWidth(item));
                }

                lyricKtvRender.resize(allWidth, allHeight);
                lyricKtvRender.use();
                for (String item : temp) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    lyricKtvRender.drawText(item, offset, save.lyric.color, save.lyric.shadow);
                    offset += 10;
                }
                lyricKtvRender.unUse();
            } else {
                lyricKtvRender.use();
                lyricKtvRender.unUse();
            }

            listNeedUpdate = false;
        }

        if (save.info.enable) {
            infoRender.draw(save.info.alpha, save.info.x, save.info.y, 1000);
        }
        if (save.list.enable) {
            listRender.draw(save.list.alpha, save.list.x, save.list.y, 1000);
        }
        if (save.lyric.enable) {
            lyricRender.draw(save.lyric.alpha, save.lyric.x, save.lyric.y, 1000);
            lyricTranRender.draw(save.lyric.alpha, save.lyric.x, save.lyric.y + 10, 1000);
            lyricKtvRender.drawWithState(save.lyric.alpha, save.lyric.x, save.lyric.y, 1000, lyricState);
        }

        //需要更新材质
        if (imageNeedUpload) {
            imageNeedUpload = false;
            updateTexture();
        }
        //绘制图片
        if (save.pic.enable && haveImg) {
            drawPic(save.pic.size, save.pic.x, save.pic.y, save.pic.pos, ang);
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
    private void drawPic(int size, int x, int y, HudPosType dir, int ang) {
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

        picRender.drawPic(save.pic.rotate, size, x1, y1, ang, save.pic.alpha);
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
    private void drawText(TextFrameBuffer fb, int width, int height, int x, int y, HudPosType dir, float alpha) {
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

        listNeedUpdate = true;
    }

    public void setLyric(String lyric, String tlyric, String ktv) {
        this.lyric = lyric;
        this.lyricTran = tlyric;
        this.lyricKtv = ktv;

        lyricState = 0;

        lyricNeedUpdate = true;
    }
}
