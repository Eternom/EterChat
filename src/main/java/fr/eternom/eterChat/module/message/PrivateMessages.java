package fr.eternom.eterChat.module.message;

import fr.eternom.eterChat.module.chat.ChatMessage;
import fr.eternom.eterChat.module.chat.ChatService;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.task.Tasks;
import fr.eternom.eterLib.module.player.PlayerDirectory;
import fr.eternom.eterLib.module.player.PlayerDirectory.NetworkPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Messages privés (/msg, /r), vers un joueur de ce serveur ou, avec Redis, de n'importe quel serveur du réseau.
 * L'expéditeur voit sa copie tout de suite ; le destinataire la reçoit sur son serveur (ChatService).
 */
public class PrivateMessages {

    private final JavaPlugin plugin;
    private final ChatService chat;
    private final PlayerDirectory directory;
    private final Messages messages;

    public PrivateMessages(JavaPlugin plugin, ChatService chat, PlayerDirectory directory, Messages messages) {
        this.plugin = plugin;
        this.chat = chat;
        this.directory = directory;
        this.messages = messages;
    }

    /** Thread principal. */
    public void send(Player sender, String targetName, String text) {
        Player local = Bukkit.getPlayerExact(targetName);
        if (local != null) {
            deliver(sender, local.getUniqueId(), local.getName(), text);
            return;
        }
        if (!chat.isNetworked()) {
            messages.send(sender, "private.offline", "player", targetName);
            return;
        }
        Tasks.async(plugin, sender, () -> directory.find(targetName).filter(NetworkPlayer::isOnline),
                found -> found.ifPresentOrElse(
                        target -> deliver(sender, target.uuid(), target.name(), text),
                        () -> messages.send(sender, "private.offline", "player", targetName)),
                () -> messages.send(sender, "private.error"));
    }

    /** Thread principal : répond au dernier correspondant. */
    public void reply(Player sender, String text) {
        UUID uuid = sender.getUniqueId();
        Tasks.async(plugin, sender, () -> chat.findReply(uuid),
                (Optional<String> name) -> name.ifPresentOrElse(
                        target -> send(sender, target, text),
                        () -> messages.send(sender, "private.no-reply")),
                () -> messages.send(sender, "private.error"));
    }

    private void deliver(Player sender, UUID target, String targetName, String text) {
        if (target.equals(sender.getUniqueId())) {
            messages.send(sender, "private.self");
            return;
        }
        ChatMessage message = chat.create(sender, ChatMessage.Type.PRIVATE, text, target, targetName);
        sender.sendMessage(chat.line(sender, "private.sent", message));
        chat.rememberReply(sender.getUniqueId(), targetName);
        boolean local = Bukkit.getPlayer(target) != null;
        chat.send(message, () -> {
            // Destinataire sur un autre serveur et Redis en panne : le message n'est pas arrivé
            if (!local && sender.isOnline()) {
                messages.send(sender, "private.failed", "player", targetName);
            }
        });
    }
}
