package fr.eternom.eterChat.module.chat;

import fr.eternom.eterChat.module.preference.ChatPreferences.Setting;
import fr.eternom.eterChat.module.preference.PreferenceActions;
import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /staffchat <message> : un message au staff ; /staffchat seul : tout le chat part au staff (ou revient au global). */
public class StaffChatCommand implements CommandExecutor {

    private final ChatService chat;
    private final PreferenceActions actions;
    private final Messages messages;

    public StaffChatCommand(ChatService chat, PreferenceActions actions, Messages messages) {
        this.chat = chat;
        this.actions = actions;
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
        } else {
            actions.toggle(player, Setting.STAFF_CHANNEL);
        }
        return true;
    }
}
