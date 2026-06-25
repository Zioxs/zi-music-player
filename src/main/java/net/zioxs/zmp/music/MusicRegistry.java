package net.zioxs.zmp.music;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class MusicRegistry {

    private static final List<MusicEntry> ALL = new ArrayList<>();

    static {
        // We will no longer statically initialize hardcoded tracks!
    }

    public static void loadFromResourcePack(net.minecraft.server.packs.resources.ResourceManager manager) {
        ALL.clear();

        try {
            List<net.minecraft.server.packs.resources.Resource> resources = manager.getResourceStack(Identifier.fromNamespaceAndPath("minecraft", "sounds.json"));
            for (net.minecraft.server.packs.resources.Resource res : resources) {
                try (java.io.InputStream is = res.open(); java.io.InputStreamReader reader = new java.io.InputStreamReader(is)) {
                    com.google.gson.JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    for (String eventName : root.keySet()) {
                        if (eventName.startsWith("music.") || eventName.startsWith("music_disc.")) {
                            MusicCategory category = eventName.startsWith("music_disc.") ? MusicCategory.DISCS : MusicCategory.AMBIENT;
                            
                            com.google.gson.JsonElement soundsEl = root.getAsJsonObject(eventName).get("sounds");
                            if (soundsEl != null && soundsEl.isJsonArray()) {
                                for (com.google.gson.JsonElement soundEl : soundsEl.getAsJsonArray()) {
                                    String rawPath;
                                    if (soundEl.isJsonObject()) {
                                        com.google.gson.JsonObject soundObj = soundEl.getAsJsonObject();
                                        if (soundObj.has("type") && soundObj.get("type").getAsString().equals("event")) {
                                            continue;
                                        }
                                        rawPath = soundObj.get("name").getAsString();
                                    } else {
                                        rawPath = soundEl.getAsString();
                                    }
                                    
                                    // Sometimes an event string is given without type="event" in broken packs.
                                    // A valid raw path for music always has at least one slash (e.g. music/game/calm1).
                                    if (!rawPath.contains("/")) continue;
                                    
                                    String id = extractId(rawPath);
                                    String title = generateTitle(id);
                                    String artist = generateArtist(id, rawPath);
                                    
                                    ALL.add(new MusicEntry(id, title, artist, "--:--", 0, rawPath, Identifier.fromNamespaceAndPath("minecraft", eventName), category));
                                }
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        List<MusicEntry> unique = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (MusicEntry e : ALL) {
            if (seen.add(e.id())) {
                unique.add(e);
            }
        }
        ALL.clear();
        ALL.addAll(unique);

        calculateDurationsAsync(manager);
    }
    
    private static String extractId(String rawPath) {
        int slash = rawPath.lastIndexOf('/');
        return slash == -1 ? rawPath : rawPath.substring(slash + 1);
    }
    
    private static String generateTitle(String id) {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        String res = sb.toString().trim();
        res = res.replaceAll("(\\D)(\\d+)", "$1 $2");
        if (res.equalsIgnoreCase("draiton")) return "Dreiton"; // Fix typo in vanilla ID
        return res;
    }
    
    private static String generateArtist(String id, String rawPath) {
        if (id.equals("5")) return "Samuel Åberg";
        if (id.matches("calm.*|hal.*|nuance.*|piano.*|axolotl|dragon_fish|shuniji|aria_math|biome_fest|blind_spots|draiton|haunt_muskie|taswell|mutation|moog_city_2|beginning_2|floating_trees|concrete_halls|dead_voxel|warmth|ballad_of_the_cats|alpha|boss|end|13|cat|blocks|chirp|far|mall|mellohi|stal|strad|ward|11|wait")) return "C418";
        if (id.matches("comforting_memories|floating_dream|wending|komorebi|pokopoko|yakusoku")) return "Kumi Tanioka";
        if (id.matches("aerie|ancestry|deeper|eld_unknown|labyrinthine|stand_tall|left_to_bloom|one_more_day|infinite_amethyst|firebugs|endless|otherside|pigstep|creator.*|chrysopoeia|so_below|rubedo")) return "Lena Raine";
        if (id.matches("crescent_dunes|echo_in_the_wind|a_familiar_room|bromeliad|featherfall|watcher|puzzlebox|relic|precipice")) return "Aaron Cherof";
        if (id.matches("broken_clocks|lilypad|fireflies|os_piano|below_and_above|tears")) return "Amos Roddy";
        return "Minecraft";
    }
    
    private static void calculateDurationsAsync(net.minecraft.server.packs.resources.ResourceManager manager) {
        java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
            for (int i = 0; i < ALL.size(); i++) {
                MusicEntry e = ALL.get(i);
                try {
                    Identifier oggId = Identifier.fromNamespaceAndPath("minecraft", "sounds/" + e.rawPath() + ".ogg");
                    var resOpt = manager.getResource(oggId);
                    if (resOpt.isPresent()) {
                        byte[] data = resOpt.get().open().readAllBytes();
                        int duration = getOggDuration(data);
                        if (duration > 0) {
                            String durStr = String.format("%d:%02d", duration / 60, duration % 60);
                            MusicEntry updated = new MusicEntry(e.id(), e.title(), e.artist(), durStr, duration, e.rawPath(), e.soundLocation(), e.category());
                            ALL.set(i, updated);
                        }
                    }
                } catch (Exception ex) {}
            }
        });
    }

    private static int getOggDuration(byte[] data) {
        try {
            if (data.length < 50) return 0;
            
            int vorbisOffset = -1;
            for (int i = 0; i < 100 && i < data.length - 15; i++) {
                if (data[i] == 1 && data[i+1] == 'v' && data[i+2] == 'o' && data[i+3] == 'r' 
                    && data[i+4] == 'b' && data[i+5] == 'i' && data[i+6] == 's') {
                    vorbisOffset = i;
                    break;
                }
            }
            
            int sampleRate = 44100;
            if (vorbisOffset != -1) {
                sampleRate = java.nio.ByteBuffer.wrap(data, vorbisOffset + 12, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            }
            if (sampleRate <= 0) sampleRate = 44100;
            
            long granulePos = 0;
            for (int i = data.length - 4; i >= Math.max(0, data.length - 65536); i--) {
                if (data[i] == 'O' && data[i+1] == 'g' && data[i+2] == 'g' && data[i+3] == 'S') {
                    long pos = java.nio.ByteBuffer.wrap(data, i + 6, 8).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
                    if (pos != -1) {
                        granulePos = pos;
                        break;
                    }
                }
            }
            return (int) (granulePos / sampleRate);
        } catch (Exception e) {
            return 0;
        }
    }


    public static List<MusicEntry> getByCategory(MusicCategory category) {
        List<MusicEntry> base = (category == MusicCategory.PLAYLIST) ? PlaylistManager.getPlaylistEntries() : ALL.stream().filter(e -> e.category() == category).toList();
        return base.stream()
                .sorted((a, b) -> a.title().compareToIgnoreCase(b.title()))
                .toList();
    }

    public static List<MusicEntry> search(String query, MusicCategory category) {
        List<MusicEntry> base = (category == MusicCategory.PLAYLIST) ? PlaylistManager.getPlaylistEntries() : ALL.stream().filter(e -> e.category() == category).toList();
        if (query == null || query.isBlank()) {
            return base.stream().sorted((a, b) -> a.title().compareToIgnoreCase(b.title())).toList();
        }
        
        String lower = query.toLowerCase().trim();
        return base.stream()
                .filter(e -> e.title().toLowerCase().contains(lower)
                        || e.artist().toLowerCase().contains(lower))
                .sorted((a, b) -> a.title().compareToIgnoreCase(b.title()))
                .toList();
    }

    public static MusicEntry findByRawPath(String rawPath) {
        for (MusicEntry entry : ALL) {
            if (entry.rawPath().equals(rawPath)) return entry;
        }
        return null;
    }

    public static List<MusicEntry> getAll() {
        return ALL;
    }
}
