package fr.eternom.eterChat.module.preference;

import fr.eternom.eterChat.module.preference.ChatPreferences.Setting;
import fr.eternom.eterChat.module.preference.ChatPreferences.Settings;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.task.Tasks;
import fr.eternom.eterLib.module.player.PlayerDirectory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Changer un réglage ou la liste des ignorés, avec le message au joueur et l'écriture en base :
 * la même chose depuis une commande ou depuis le menu /chat. Thread principal.
 */
public class PreferenceActions {

    /** Message à envoyer (clé de langue) et pseudo à y mettre. */
    private record Outcome(String key, String name) {
    }

    private final JavaPlugin plugin;
    private final ChatPreferences preferences;
    private final PlayerDirectory directory;
    private final Messages messages;

    public PreferenceActions(JavaPlugin plugin, ChatPreferences preferences, PlayerDirectory directory, Messages messages) {
        this.plugin = plugin;
        this.preferences = preferences;
        this.directory = directory;
        this.messages = messages;
    }

    /** @return true si le réglage est maintenant activé */
    public boolean toggle(Player player, Setting setting) {
        UUID uuid = player.getUniqueId();
        Settings next = preferences.get(uuid).toggle(setting);
        preferences.set(uuid, next);
        boolean enabled = next.is(setting);
        messages.send(player, messageKey(setting) + (enabled ? ".enabled" : ".disabled"));
        Tasks.async(plugin, () -> preferences.save(uuid), "Réglage de chat non enregistré pour " + player.getName());
        return enabled;
    }

    /** /ignore : ignore name, ou ne l'ignore plus s'il l'était déjà. */
    public void toggleIgnore(Player player, String name, Runnable then) {
        changeIgnore(player, name, true, then);
    }

    /** Menu : ignore name (rien ne change s'il l'est déjà). */
    public void ignore(Player player, String name, Runnable then) {
        changeIgnore(player, name, false, then);
    }

    /**
     * name : n'importe quel joueur déjà venu sur le réseau, même hors ligne.
     * then est lancé une fois la base à jour (ex : rouvrir le menu).
     */
    private void changeIgnore(Player player, String name, boolean toggle, Runnable then) {
        UUID uuid = player.getUniqueId();
        Tasks.async(plugin, player, () -> directory.find(name).map(target -> {
                    if (target.uuid().equals(uuid)) {
                        return new Outcome("ignore.self", target.name());
                    }
                    if (preferences.isIgnoring(uuid, target.uuid())) {
                        if (!toggle) {
                            return new Outcome("ignore.already", target.name());
                        }
                        preferences.unignore(uuid, target.uuid());
                        return new Outcome("ignore.removed", target.name());
                    }
                    preferences.ignore(uuid, target.uuid(), target.name());
                    return new Outcome("ignore.added", target.name());
                }),
                result -> {
                    Outcome outcome = result.orElse(new Outcome("ignore.unknown", name));
                    messages.send(player, outcome.key(), "player", outcome.name());
                    then.run();
                },
                () -> messages.send(player, "ignore.error"));
    }

    public void unignore(Player player, UUID target, String name, Runnable then) {
        UUID uuid = player.getUniqueId();
        Tasks.async(plugin, player, () -> {
                    preferences.unignore(uuid, target);
                    return name;
                },
                removed -> {
                    messages.send(player, "ignore.removed", "player", removed);
                    then.run();
                },
                () -> messages.send(player, "ignore.error"));
    }

    private static String messageKey(Setting setting) {
        return switch (setting) {
            case STAFF_CHANNEL -> "staff";
            case NOTIFICATIONS -> "notifications";
            case SOCIAL_SPY -> "socialspy";
            case PRIVATE_MESSAGES -> "private-messages";
        };
    }
}
