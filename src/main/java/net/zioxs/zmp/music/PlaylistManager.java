package net.zioxs.zmp.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PlaylistManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("zmp_playlist.json");

    private static final Set<String> savedIds = new LinkedHashSet<>();

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            List<String> ids = GSON.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            if (ids != null) {
                savedIds.clear();
                savedIds.addAll(ids);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(new ArrayList<>(savedIds), writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void toggle(String id) {
        if (savedIds.contains(id)) {
            savedIds.remove(id);
        } else {
            savedIds.add(id);
        }
        save();
    }

    public static boolean contains(String id) {
        return savedIds.contains(id);
    }

    public static List<MusicEntry> getPlaylistEntries() {
        List<MusicEntry> list = new ArrayList<>();
        List<MusicEntry> all = MusicRegistry.getAll();
        for (String id : savedIds) {
            for (MusicEntry e : all) {
                if (e.id().equals(id)) {
                    list.add(e);
                    break;
                }
            }
        }
        return list;
    }
}
