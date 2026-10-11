package pl.coreclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Menu główne Core Client: ciemne tło, logo z pikseli, własny tekst wersji. */
public class CoreTitleScreen extends Screen {
    private static final String[][] GLYPHS = {
        {".XXX", "X...", "X...", "X...", ".XXX"},   // C
        {".XX.", "X..X", "X..X", "X..X", ".XX."},   // O
        {"XXX.", "X..X", "XXX.", "X.X.", "X..X"},   // R
        {"XXXX", "X...", "XXX.", "X...", "XXXX"}    // E
    };

    public CoreTitleScreen() { super(Text.literal("Core Client")); }

    @Override
    protected void init() {
        int w = 200, h = 22, x = width / 2 - w / 2, y = height / 2 - 14;
        addDrawableChild(ButtonWidget.builder(Text.translatable("menu.singleplayer"), b -> client.setScreen(new SelectWorldScreen(this))).dimensions(x, y, w, h).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("menu.multiplayer"), b -> client.setScreen(new MultiplayerScreen(this))).dimensions(x, y + 28, w, h).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("menu.options"), b -> client.setScreen(new OptionsScreen(this, client.options))).dimensions(x, y + 56, w, h).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("menu.quit"), b -> client.scheduleStop()).dimensions(x, y + 90, w, h).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xFF1C0F12, 0xFF0A0B10);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int p = 4, gap = p, lw = 4 * p, total = 4 * lw + 3 * gap;
        int x0 = width / 2 - total / 2, y0 = height / 2 - 14 - 5 * p - 34;
        ctx.fill(x0 - 12, y0 - 10, x0 + total + 12, y0 + 5 * p + 10, 0xFFFF8A1F);
        ctx.fill(x0 - 9, y0 - 7, x0 + total + 9, y0 + 5 * p + 7, 0xFF1C0F12);
        for (int g = 0; g < 4; g++) {
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 4; c++) {
                    if (GLYPHS[g][r].charAt(c) == 'X') {
                        int px = x0 + g * (lw + gap) + c * p, py = y0 + r * p;
                        ctx.fill(px, py, px + p, py + p, 0xFFFF8A1F);
                    }
                }
            }
        }
        String v = CoreConfig.versionText;
        ctx.drawTextWithShadow(textRenderer, v, width / 2 - textRenderer.getWidth(v) / 2, height - 16, 0xFF8A90A5);
    }
}
