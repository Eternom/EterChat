package fr.eternom.eterChat.module.chat;

import fr.eternom.eterChat.module.preference.ChatPreferences;
import fr.eternom.eterChat.module.preference.ChatPreferences.Settings;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.task.Tasks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** /staffchat <message> : un message au staff ; /staffchat seul : tout le chat part au staff (ou revient au global). */
public class StaffChatCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ChatService chat;
    private final ChatPreferences preferences;
    private final Messages messages;

    public StaffChatCommand(JavaPlugin plugin, ChatService chat, ChatPreferences preferences, Messages messages) {
        this.plugin = plugin;
        this.chat = chat;
        this.preferences = preferences;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        if (args.length > 0) {
            chat.chat(player, String.join(" ", args), true);
            return true;
        }
        UUID uuid = player.getUniqueId();
        Settings current = preferences.get(uuid);
        preferences.set(uuid, new Settings(!current.staffChannel(), current.notifications(), current.socialSpy()));
        messages.send(player, current.staffChannel() ? "staff.off" : "staff.on");
        Tasks.async(plugin, () -> preferences.save(uuid), "Canal de chat non enregistré pour " + player.getName());
        return true;
    }
}
