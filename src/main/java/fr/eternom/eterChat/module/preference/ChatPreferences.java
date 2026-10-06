package fr.eternom.eterChat.module.preference;

import fr.eternom.eterLib.helper.sql.Column;
import fr.eternom.eterLib.helper.sql.Database;
import fr.eternom.eterLib.helper.sql.Row;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Réglages de chat de chaque joueur, enregistrés en base pour le suivre sur tous les serveurs :
 * - eterchat_players : canal staff actif, notifications (son et alerte des mentions et messages privés), espion des messages privés ;
 * - eterchat_ignores : joueurs ignorés (owner ignore target).
 * Gardés en mémoire pour les joueurs connectés ; tant qu'ils ne sont pas lus, les valeurs par défaut s'appliquent.
 * Les méthodes qui touchent à la base sont bloquantes : hors du thread principal.
 */
public class ChatPreferences {

    private static final String PLAYERS = "players";
    private static final String IGNORES = "ignores";

    public record Settings(boolean staffChannel, boolean notifications, boolean socialSpy) {

        static final Settings DEFAULT = new Settings(false, true, false);
    }

    private final Database database;
    private final Map<UUID, Settings> settings = new ConcurrentHashMap<>();
    /** Joueur -> joueurs qu'il ignore (uuid -> pseudo, pour la liste). */
    private final Map<UUID, Map<UUID, String>> ignores = new ConcurrentHashMap<>();

    public ChatPreferences(Database database) {
        this.database = database;
        database.createTable(PLAYERS,
                Column.of("uuid", Column.Type.UUID).primaryKey(),
                Column.of("staff_channel", Column.Type.BOOLEAN).notNull(),
                Column.of("notifications", Column.Type.BOOLEAN).notNull(),
                Column.of("social_spy", Column.Type.BOOLEAN).notNull());
        database.createTable(IGNORES,
                Column.of("owner", Column.Type.UUID).primaryKey(),
                Column.of("target", Column.Type.UUID).primaryKey(),
                Column.of("target_name", Column.Type.STRING).length(16).notNull());
    }

    public Settings get(UUID player) {
        return settings.getOrDefault(player, Settings.DEFAULT);
    }

    /** Thread principal : change tout de suite en mémoire ; l'écriture en base se fait ensuite (save, en async). */
    public void set(UUID player, Settings value) {
        settings.put(player, value);
    }

    public boolean isIgnoring(UUID player, UUID sender) {
        Map<UUID, String> ignored = ignores.get(player);
        return ignored != null && ignored.containsKey(sender);
    }

    /** Pseudos ignorés par le joueur. */
    public Iterable<String> ignoredNames(UUID player) {
        return ignores.getOrDefault(player, Map.of()).values();
    }

    /** Bloquant (base) : à l'arrivée du joueur. */
    public void load(UUID player) {
        Settings loaded = database.getFirst(PLAYERS, Map.of("uuid", player))
                .map(row -> new Settings(row.getBoolean("staff_channel"), row.getBoolean("notifications"), row.getBoolean("social_spy")))
                .orElse(Settings.DEFAULT);
        Map<UUID, String> ignored = new ConcurrentHashMap<>();
        for (Row row : database.get(IGNORES, Map.of("owner", player))) {
            ignored.put(row.getUUID("target"), row.getString("target_name"));
        }
        settings.put(player, loaded);
        ignores.put(player, ignored);
    }

    /** Bloquant (base). */
    public void save(UUID player) {
        Settings value = get(player);
        database.set(PLAYERS, Map.of("uuid", player, "staff_channel", value.staffChannel(), "notifications", value.notifications(),
                "social_spy", value.socialSpy()), "uuid");
    }

    /**
     * Bloquant (base) : ignore target, ou ne l'ignore plus s'il l'était déjà.
     * @return true si target est maintenant ignoré
     */
    public boolean toggleIgnore(UUID player, UUID target, String targetName) {
        Map<UUID, String> ignored = ignores.computeIfAbsent(player, key -> new ConcurrentHashMap<>());
        if (ignored.remove(target) != null) {
            database.delete(IGNORES, Map.of("owner", player, "target", target));
            return false;
        }
        database.set(IGNORES, Map.of("owner", player, "target", target, "target_name", targetName), "owner", "target");
        ignored.put(target, targetName);
        return true;
    }

    public void forget(UUID player) {
        settings.remove(player);
        ignores.remove(player);
    }
}
