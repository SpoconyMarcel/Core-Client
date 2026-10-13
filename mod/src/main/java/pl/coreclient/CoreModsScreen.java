package pl.coreclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Menu modułów otwierane w grze (domyślnie prawy Shift): włączanie i wyłączanie elementów HUD. */
public class CoreModsScreen extends Screen {
    private static final String[] NAMES = {"Koordynaty", "FPS", "CPS", "Keystrokes", "Armor Status", "Zoom", "Toggle Sprint", "Full Vision"};

    public CoreModsScreen() { super(Text.literal("Core Client")); }

    private static Text label(String n) { return Text.literal(n + ": " + (CoreConfig.mod(n) ? "ON" : "OFF")); }

    @Override
    protected void init() {
        int w = 150, h = 20, gap = 6, x0 = width / 2 - w - gap / 2, y0 = height / 2 - 62;
        for (int i = 0; i < NAMES.length; i++) {
            final String n = NAMES[i];
            int x = x0 + (i % 2) * (w + gap), y = y0 + (i / 2) * (h + gap);
            addDrawableChild(ButtonWidget.builder(label(n), b -> {
                boolean on = !CoreConfig.mod(n);
                CoreConfig.MODS.put(n, on);
                if (n.equals("Full Vision") && on) CoreClientMod.fullVision = true;
                CoreConfig.save();
                b.setMessage(label(n));
            }).dimensions(x, y, w, h).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(width / 2 - 75, y0 + 4 * (h + gap) + 8, 150, h).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        String t = "Core Client: moduły";
        ctx.drawTextWithShadow(textRenderer, t, width / 2 - textRenderer.getWidth(t) / 2, height / 2 - 90, 0xFFFF8A1F);
    }
}
