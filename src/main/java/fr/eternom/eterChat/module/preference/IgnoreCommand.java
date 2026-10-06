package fr.eternom.eterChat.module.preference;

import fr.eternom.eterChat.module.message.PlayerNames;
import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /ignore <joueur> : ne plus voir ses messages (chat global et privés), ou de nouveau les voir.
 * /ignore seul : la liste. Le joueur ignoré n'en est pas averti. Le canal staff ne s'ignore pas.
 */
public class IgnoreCommand implements TabExecutor {

    private final PreferenceActions actions;
    private final ChatPreferences preferences;
    private final PlayerNames names;
    private final Messages messages;

    public IgnoreCommand(PreferenceActions actions, ChatPreferences preferences, PlayerNames names, Messages messages) {
        this.actions = actions;
        this.preferences = preferences;
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
            String list = String.join(", ", preferences.ignored(player.getUniqueId()).values());
            messages.send(player, list.isEmpty() ? "ignore.empty" : "ignore.list", "players", list);
            return true;
        }
        actions.toggleIgnore(player, args[0], () -> {
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? names.complete(args[0]) : List.of();
    }
}
