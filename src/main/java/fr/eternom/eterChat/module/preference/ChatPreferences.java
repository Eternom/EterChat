package fr.eternom.eterChat.module.preference;

import fr.eternom.eterLib.helper.sql.Column;
import fr.eternom.eterLib.helper.sql.Database;
import fr.eternom.eterLib.helper.sql.Row;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Réglages de chat de chaque joueur, enregistrés en base pour le suivre sur tous les serveurs :
 * - eterchat_players : canal staff actif, notifications (son et alerte des mentions et messages privés),
 *   espion des messages privés, messages privés acceptés ;
 * - eterchat_ignores : joueurs ignorés (owner ignore target).
 * Gardés en mémoire pour les joueurs connectés ; tant qu'ils ne sont pas lus, les valeurs par défaut s'appliquent.
 * Les méthodes qui touchent à la base sont bloquantes : hors du thread principal.
 */
public class ChatPreferences {

    private static final String PLAYERS = "players";
    private static final String IGNORES = "ignores";

    public enum Setting { STAFF_CHANNEL, NOTIFICATIONS, SOCIAL_SPY, PRIVATE_MESSAGES }

    public record Settings(boolean staffChannel, boolean notifications, boolean socialSpy, boolean privateMessages) {

        static final Settings DEFAULT = new Settings(false, true, false, true);

        public boolean is(Setting setting) {
            return switch (setting) {
                case STAFF_CHANNEL -> staffChannel;
                case NOTIFICATIONS -> notifications;
                case SOCIAL_SPY -> socialSpy;
                case PRIVATE_MESSAGES -> privateMessages;
            };
        }

        public Settings toggle(Setting setting) {
            return new Settings(
                    setting == Setting.STAFF_CHANNEL ? !staffChannel : staffChannel,
                    setting == Setting.NOTIFICATIONS ? !notifications : notifications,
                    setting == Setting.SOCIAL_SPY ? !socialSpy : socialSpy,
                    setting == Setting.PRIVATE_MESSAGES ? !privateMessages : privateMessages);
        }
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
                Column.of("social_spy", Column.Type.BOOLEAN).notNull(),
                Column.of("private_messages", Column.Type.BOOLEAN).notNull());
        // Ajoutée en 1.1.0 : NULL pour les joueurs déjà enregistrés = messages privés acceptés
        database.addColumn(PLAYERS, Column.of("private_messages", Column.Type.BOOLEAN));
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

    /** Bloquant (base) : réglages d'un joueur qui peut être sur un autre serveur. */
    public Settings read(UUID player) {
        return database.getFirst(PLAYERS, Map.of("uuid", player)).map(ChatPreferences::toSettings).orElse(Settings.DEFAULT);
    }

    public boolean isIgnoring(UUID player, UUID sender) {
        Map<UUID, String> ignored = ignores.get(player);
        return ignored != null && ignored.containsKey(sender);
    }

    /** Joueurs ignorés par le joueur : uuid -> pseudo. */
    public Map<UUID, String> ignored(UUID player) {
        return Map.copyOf(ignores.getOrDefault(player, Map.of()));
    }

    /** Bloquant (base) : à l'arrivée du joueur. */
    public void load(UUID player) {
        Map<UUID, String> ignored = new ConcurrentHashMap<>();
        for (Row row : database.get(IGNORES, Map.of("owner", player))) {
            ignored.put(row.getUUID("target"), row.getString("target_name"));
        }
        settings.put(player, read(player));
        ignores.put(player, ignored);
    }

    /** Bloquant (base). */
    public void save(UUID player) {
        Settings value = get(player);
        database.set(PLAYERS, Map.of("uuid", player, "staff_channel", value.staffChannel(), "notifications", value.notifications(),
                "social_spy", value.socialSpy(), "private_messages", value.privateMessages()), "uuid");
    }

    /** Bloquant (base). */
    public void ignore(UUID player, UUID target, String targetName) {
        database.set(IGNORES, Map.of("owner", player, "target", target, "target_name", targetName), "owner", "target");
        ignores.computeIfAbsent(player, key -> new ConcurrentHashMap<>()).put(target, targetName);
    }

    /** Bloquant (base). */
    public void unignore(UUID player, UUID target) {
        database.delete(IGNORES, Map.of("owner", player, "target", target));
        Map<UUID, String> ignored = ignores.get(player);
        if (ignored != null) {
            ignored.remove(target);
        }
    }

    public void forget(UUID player) {
        settings.remove(player);
        ignores.remove(player);
    }

    private static Settings toSettings(Row row) {
        return new Settings(row.getBoolean("staff_channel"), row.getBoolean("notifications"), row.getBoolean("social_spy"),
                row.get("private_messages") == null || row.getBoolean("private_messages"));
    }
}
