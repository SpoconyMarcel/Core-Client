package pl.coreclient;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Zamiana nazw klawiszy z launchera na kody GLFW i odczyt stanu klawiszy. */
public final class KeyUtil {
    public static int code(String n) {
        if (n == null || n.isEmpty()) return -1;
        if (n.length() == 1) {
            char c = n.charAt(0);
            if (c == ' ') return GLFW.GLFW_KEY_SPACE;
            if (Character.isLetterOrDigit(c) && c < 128) return Character.toUpperCase(c);
            return -1;
        }
        switch (n) {
            case "Alt": return GLFW.GLFW_KEY_LEFT_ALT;
            case "Shift": return GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RShift": return GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "Control": return GLFW.GLFW_KEY_LEFT_CONTROL;
            case "Tab": return GLFW.GLFW_KEY_TAB;
            case "CapsLock": return GLFW.GLFW_KEY_CAPS_LOCK;
            default:
        }
        if (n.matches("F([1-9]|1[0-2])")) return GLFW.GLFW_KEY_F1 + Integer.parseInt(n.substring(1)) - 1;
        return -1;
    }

    public static boolean down(int code) {
        if (code < 0) return false;
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(h, code) == GLFW.GLFW_PRESS;
    }

    public static boolean mouse(int button) {
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetMouseButton(h, button) == GLFW.GLFW_PRESS;
    }
}
