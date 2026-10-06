package fr.eternom.eterChat.module.preference;

import fr.eternom.eterChat.module.message.PlayerNames;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.task.Tasks;
import fr.eternom.eterLib.module.player.PlayerDirectory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * /ignore <joueur> : ne plus voir ses messages (chat global et privés), ou de nouveau les voir.
 * /ignore seul : la liste. Le joueur ignoré n'en est pas averti. Le canal staff ne s'ignore pas.
 */
public class IgnoreCommand implements TabExecutor {

    private final JavaPlugin plugin;
    private final ChatPreferences preferences;
    private final PlayerDirectory directory;
    private final PlayerNames names;
    private final Messages messages;

    public IgnoreCommand(JavaPlugin plugin, ChatPreferences preferences, PlayerDirectory directory, PlayerNames names,
                         Messages messages) {
        this.plugin = plugin;
        this.preferences = preferences;
        this.directory = directory;
        this.names = names;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        if (args.length == 0) {
            String list = String.join(", ", preferences.ignoredNames(player.getUniqueId()));
            messages.send(player, list.isEmpty() ? "ignore.empty" : "ignore.list", "players", list);
            return true;
        }
        String name = args[0];
        UUID uuid = player.getUniqueId();
        // find : n'importe quel joueur déjà venu sur le réseau, même hors ligne
        Tasks.async(plugin, player, () -> directory.find(name).map(target -> {
                    if (target.uuid().equals(uuid)) {
                        return "ignore.self";
                    }
                    return preferences.toggleIgnore(uuid, target.uuid(), target.name()) ? "ignore.added" : "ignore.removed";
                }),
                result -> messages.send(player, result.orElse("ignore.unknown"), "player", name),
                () -> messages.send(player, "private.error"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? names.complete(args[0]) : List.of();
    }
}
