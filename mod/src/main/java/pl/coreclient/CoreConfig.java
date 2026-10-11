package pl.coreclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Czyta config/coreclient.json zapisywany przez launcher. */
public final class CoreConfig {
    public static final Map<String, Boolean> MODS = new HashMap<>();
    public static final Map<String, String> BINDS = new HashMap<>();
    public static String versionText = "Core Client";

    public static boolean mod(String name) { return MODS.getOrDefault(name, false); }
    public static String bind(String name) { return BINDS.getOrDefault(name, ""); }

    public static void load() {
        for (String m : new String[]{"FPS", "CPS", "Koordynaty", "Zoom", "Full Vision"}) MODS.put(m, true);
        BINDS.put("Zoom", "C"); BINDS.put("Full Vision", "V"); BINDS.put("Toggle Sprint", "G");
        Path p = FabricLoader.getInstance().getConfigDir().resolve("coreclient.json");
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            if (o.has("mods")) o.getAsJsonObject("mods").entrySet().forEach(e -> MODS.put(e.getKey(), e.getValue().getAsBoolean()));
            if (o.has("binds")) o.getAsJsonObject("binds").entrySet().forEach(e -> BINDS.put(e.getKey(), e.getValue().getAsString()));
            if (o.has("versionText") && !o.get("versionText").getAsString().isBlank()) versionText = o.get("versionText").getAsString();
        } catch (Exception ignored) { }
    }
}
