package fr.eternom.eterChat.module.preference;

import fr.eternom.eterChat.module.preference.ChatPreferences.Settings;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.task.Tasks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** /notifications et /socialspy : active ou coupe le réglage, gardé pour tous les serveurs. */
public class ToggleCommand implements CommandExecutor {

    public enum Kind { NOTIFICATIONS, SOCIAL_SPY }

    private final JavaPlugin plugin;
    private final ChatPreferences preferences;
    private final Messages messages;
    private final Kind kind;

    public ToggleCommand(JavaPlugin plugin, ChatPreferences preferences, Messages messages, Kind kind) {
        this.plugin = plugin;
        this.preferences = preferences;
        this.messages = messages;
        this.kind = kind;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        UUID uuid = player.getUniqueId();
        Settings current = preferences.get(uuid);
        boolean enabled = switch (kind) {
            case NOTIFICATIONS -> {
                preferences.set(uuid, new Settings(current.staffChannel(), !current.notifications(), current.socialSpy()));
                yield !current.notifications();
            }
            case SOCIAL_SPY -> {
                preferences.set(uuid, new Settings(current.staffChannel(), current.notifications(), !current.socialSpy()));
                yield !current.socialSpy();
            }
        };
        String key = kind == Kind.NOTIFICATIONS ? "notifications" : "socialspy";
        messages.send(player, key + (enabled ? ".on" : ".off"));
        Tasks.async(plugin, () -> preferences.save(uuid), "Réglage de chat non enregistré pour " + player.getName());
        return true;
    }
}
