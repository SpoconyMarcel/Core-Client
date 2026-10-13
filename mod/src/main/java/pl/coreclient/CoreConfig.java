package pl.coreclient;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Czyta config/coreclient.json (zapisywany przez launcher) oraz config/coreclient-hud.json
 * (zmiany modułów zrobione w grze menu pod prawym Shiftem; mają pierwszeństwo).
 */
public final class CoreConfig {
    public static final Map<String, Boolean> MODS = new HashMap<>();
    public static final Map<String, String> BINDS = new HashMap<>();
    public static String versionText = "Core Client";

    public static boolean mod(String name) { return MODS.getOrDefault(name, false); }
    public static String bind(String name) { return BINDS.getOrDefault(name, ""); }

    private static Path file(String n) { return FabricLoader.getInstance().getConfigDir().resolve(n); }

    public static void load() {
        for (String m : new String[]{"FPS", "CPS", "Koordynaty", "Zoom", "Full Vision"}) MODS.put(m, true);
        BINDS.put("Menu Core", "RShift"); BINDS.put("Zoom", "C"); BINDS.put("Full Vision", "V"); BINDS.put("Toggle Sprint", "G");

        Path p = file("coreclient.json");
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                if (o.has("mods")) o.getAsJsonObject("mods").entrySet().forEach(e -> MODS.put(e.getKey(), e.getValue().getAsBoolean()));
                if (o.has("binds")) o.getAsJsonObject("binds").entrySet().forEach(e -> BINDS.put(e.getKey(), e.getValue().getAsString()));
                if (o.has("versionText") && !o.get("versionText").getAsString().isBlank()) versionText = o.get("versionText").getAsString();
            } catch (Exception ignored) { }
        }
        Path h = file("coreclient-hud.json");
        if (Files.exists(h)) {
            try (Reader r = Files.newBufferedReader(h)) {
                JsonParser.parseReader(r).getAsJsonObject().entrySet().forEach(e -> MODS.put(e.getKey(), e.getValue().getAsBoolean()));
            } catch (Exception ignored) { }
        }
    }

    /** Zapisuje moduły wybrane w grze. */
    public static void save() {
        try {
            JsonObject o = new JsonObject();
            MODS.forEach((k, v) -> o.addProperty(k, v));
            Files.writeString(file("coreclient-hud.json"), new GsonBuilder().setPrettyPrinting().create().toJson(o));
        } catch (Exception ignored) { }
    }
}
