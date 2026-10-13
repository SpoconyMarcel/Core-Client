package pl.coreclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;

public class CoreClientMod implements ClientModInitializer {
    static boolean fullVision, sprintToggle, zooming, addedNightVision;
    static int origFov = 70;
    private static final boolean[] prev = new boolean[3];

    @Override
    public void onInitializeClient() {
        CoreConfig.load();
        fullVision = CoreConfig.mod("Full Vision");
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("coreclient", "hud"), CoreHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private boolean edge(int i, boolean now) {
        boolean r = now && !prev[i];
        prev[i] = now;
        return r;
    }

    private void tick(MinecraftClient c) {
        if (c.currentScreen instanceof TitleScreen) { c.setScreen(new CoreTitleScreen()); return; }
        if (c.player == null) return;
        boolean free = c.currentScreen == null;

        if (edge(2, free && KeyUtil.down(KeyUtil.code(CoreConfig.bind("Menu Core"))))) { c.setScreen(new CoreModsScreen()); return; }

        if (CoreConfig.mod("Full Vision") && edge(0, free && KeyUtil.down(KeyUtil.code(CoreConfig.bind("Full Vision"))))) fullVision = !fullVision;
        if (CoreConfig.mod("Toggle Sprint") && edge(1, free && KeyUtil.down(KeyUtil.code(CoreConfig.bind("Toggle Sprint"))))) sprintToggle = !sprintToggle;

        // Full Vision: lokalny Night Vision bez ikony i cząsteczek
        if (CoreConfig.mod("Full Vision") && fullVision) {
            if (!c.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                c.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
            }
            addedNightVision = true;
        } else if (addedNightVision) {
            c.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            addedNightVision = false;
        }

        // Zoom: trzymaj klawisz
        boolean zk = free && CoreConfig.mod("Zoom") && KeyUtil.down(KeyUtil.code(CoreConfig.bind("Zoom")));
        SimpleOption<Integer> fov = c.options.getFov();
        if (zk && !zooming) { origFov = fov.getValue(); fov.setValue(30); zooming = true; }
        else if (!zk && zooming) { fov.setValue(origFov); zooming = false; }

        // Toggle Sprint
        if (CoreConfig.mod("Toggle Sprint") && sprintToggle && c.options.forwardKey.isPressed() && !c.player.isSneaking()) {
            c.player.setSprinting(true);
        }
    }
}
