package Color_yr.AllMusic.Hud;

import com.google.gson.Gson;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class Hud {
    public static String Info = "";
    public static String List = "";
    public static String Lyric = "";
    public static SaveOBJ save;
    public static final Object lock = new Object();

    private static MatrixStack stack = new MatrixStack();

    public static void Set(String data) {
        synchronized (lock) {
            save = new Gson().fromJson(data, SaveOBJ.class);
        }
    }

    public static void update() {
        FontRenderer hud = Minecraft.getInstance().fontRenderer;
        if (save == null)
            return;
        synchronized (lock) {
            if (save.isEnableInfo() && !Info.isEmpty()) {
                int offset = 0;
                String[] temp = Info.split("\n");
                for (String item : temp) {
                    hud.func_238405_a_(stack, item, (float) save.getInfo().getX(),
                            (float) save.getInfo().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
            if (save.isEnableList() && !List.isEmpty()) {
                String[] temp = List.split("\n");
                int offset = 0;
                for (String item : temp) {
                    hud.func_238405_a_(stack, item, (float) save.getList().getX(),
                            (float) save.getList().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
            if (save.isEnableLyric() && !Lyric.isEmpty()) {
                String[] temp = Lyric.split("\n");
                int offset = 0;
                for (String item : temp) {
                    hud.func_238405_a_(stack, item, (float) save.getLyric().getX(),
                            (float) save.getLyric().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
        }
    }
}
