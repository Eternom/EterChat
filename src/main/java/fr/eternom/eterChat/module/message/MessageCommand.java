package fr.eternom.eterChat.module.message;

import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.module.player.OnlineNames;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** /msg <joueur> <message> et /r <message>. */
public class MessageCommand implements TabExecutor {

    private final PrivateMessages privateMessages;
    private final OnlineNames names;
    private final Messages messages;
    private final boolean networked;
    private final boolean reply;

    /** @param networked false sans Redis : seuls les joueurs de ce serveur sont proposés
     *  @param reply true pour /r (pas de pseudo : le dernier correspondant) */
    public MessageCommand(PrivateMessages privateMessages, OnlineNames names, Messages messages, boolean networked, boolean reply) {
        this.privateMessages = privateMessages;
        this.names = names;
        this.messages = messages;
        this.networked = networked;
        this.reply = reply;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        if (reply) {
            if (args.length == 0) {
                messages.send(player, "private.reply-usage");
                return true;
            }
            privateMessages.reply(player, String.join(" ", args));
            return true;
        }
        if (args.length < 2) {
            messages.send(player, "private.usage");
            return true;
        }
        privateMessages.send(player, args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return !reply && args.length == 1 ? names.complete(args[0], networked) : List.of();
    }
}
