package fr.eternom.eterChat.module.gui;

import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /chat : ouvre le menu des réglages du chat. */
public class ChatMenuCommand implements CommandExecutor {

    private final ChatGui gui;
    private final Messages messages;

    public ChatMenuCommand(ChatGui gui, Messages messages) {
        this.gui = gui;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            gui.open(player);
        } else {
            messages.send(sender, "command.players-only");
        }
        return true;
    }
}
