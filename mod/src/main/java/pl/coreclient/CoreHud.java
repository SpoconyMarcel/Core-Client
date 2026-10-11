package pl.coreclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class CoreHud {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
    private static boolean lp, rp;

    private static void poll(MinecraftClient c) {
        long now = System.currentTimeMillis();
        boolean free = c.currentScreen == null;
        boolean l = free && KeyUtil.mouse(0), r = free && KeyUtil.mouse(1);
        if (l && !lp) LEFT.add(now);
        if (r && !rp) RIGHT.add(now);
        lp = l; rp = r;
        while (!LEFT.isEmpty() && now - LEFT.peek() > 1000) LEFT.poll();
        while (!RIGHT.isEmpty() && now - RIGHT.peek() > 1000) RIGHT.poll();
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.options.hudHidden) return;
        poll(c);
        var tr = c.textRenderer;

        List<String> lines = new ArrayList<>();
        if (CoreConfig.mod("Koordynaty")) {
            String f = c.player.getHorizontalFacing().asString();
            lines.add(String.format("XYZ: %.0f / %.0f / %.0f", c.player.getX(), c.player.getY(), c.player.getZ()));
            lines.add(f.substring(0, 1).toUpperCase() + f.substring(1));
        }
        if (CoreConfig.mod("FPS")) lines.add("FPS: " + c.getCurrentFps());
        if (CoreConfig.mod("CPS")) lines.add("CPS: " + LEFT.size() + " | " + RIGHT.size());
        if (CoreConfig.mod("Full Vision") && CoreClientMod.fullVision) lines.add("Full Vision");
        if (CoreConfig.mod("Toggle Sprint") && CoreClientMod.sprintToggle) lines.add("Sprint [toggled]");

        if (!lines.isEmpty()) {
            int w = 0;
            for (String s : lines) w = Math.max(w, tr.getWidth(s));
            int x = 8, y = 8;
            ctx.fill(x - 4, y - 4, x + w + 4, y + lines.size() * 10 + 2, 0x90000000);
            ctx.fill(x - 4, y - 4, x - 2, y + lines.size() * 10 + 2, 0xFFFF8A1F);
            for (int i = 0; i < lines.size(); i++) ctx.drawTextWithShadow(tr, lines.get(i), x, y + i * 10, 0xFFFFFFFF);
        }

        if (CoreConfig.mod("Keystrokes")) {
            int bx = ctx.getScaledWindowWidth() - 84, by = ctx.getScaledWindowHeight() - 76;
            key(ctx, tr, "W", bx + 26, by, c.options.forwardKey.isPressed());
            key(ctx, tr, "A", bx, by + 26, c.options.leftKey.isPressed());
            key(ctx, tr, "S", bx + 26, by + 26, c.options.backKey.isPressed());
            key(ctx, tr, "D", bx + 52, by + 26, c.options.rightKey.isPressed());
        }

        if (CoreConfig.mod("Armor Status")) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int y = ctx.getScaledWindowHeight() - 92;
            for (EquipmentSlot sl : slots) {
                ItemStack st = c.player.getEquippedStack(sl);
                if (st.isEmpty()) continue;
                ctx.drawItem(st, 8, y);
                if (st.getMaxDamage() > 0) ctx.drawTextWithShadow(tr, String.valueOf(st.getMaxDamage() - st.getDamage()), 28, y + 4, 0xFFFFFFFF);
                y += 20;
            }
        }
    }

    private static void key(DrawContext ctx, net.minecraft.client.font.TextRenderer tr, String s, int x, int y, boolean on) {
        ctx.fill(x, y, x + 22, y + 22, on ? 0xB0FF8A1F : 0x90000000);
        ctx.drawTextWithShadow(tr, s, x + 11 - tr.getWidth(s) / 2, y + 7, 0xFFFFFFFF);
    }
}
