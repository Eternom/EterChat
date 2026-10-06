package fr.eternom.eterChat.module.message;

import fr.eternom.eterChat.module.chat.ChatMessage;
import fr.eternom.eterChat.module.chat.ChatService;
import fr.eternom.eterChat.module.preference.ChatPreferences;
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
 * Un joueur qui a coupé ses messages privés (/msgtoggle) les refuse, sauf ceux du staff (eterchat.bypass.msgtoggle).
 */
public class PrivateMessages {

    public static final String BYPASS_PERMISSION = "eterchat.bypass.msgtoggle";

    /** Destinataire trouvé, et s'il accepte le message. */
    private record Recipient(UUID uuid, String name, boolean accepts) {
    }

    private final JavaPlugin plugin;
    private final ChatService chat;
    private final ChatPreferences preferences;
    private final PlayerDirectory directory;
    private final Messages messages;

    public PrivateMessages(JavaPlugin plugin, ChatService chat, ChatPreferences preferences, PlayerDirectory directory,
                           Messages messages) {
        this.plugin = plugin;
        this.chat = chat;
        this.preferences = preferences;
        this.directory = directory;
        this.messages = messages;
    }

    /** Thread principal. */
    public void send(Player sender, String targetName, String text) {
        boolean bypass = sender.hasPermission(BYPASS_PERMISSION);
        Player local = Bukkit.getPlayerExact(targetName);
        if (local != null) {
            UUID uuid = local.getUniqueId();
            deliver(sender, new Recipient(uuid, local.getName(), bypass || preferences.get(uuid).privateMessages()), text);
            return;
        }
        if (!chat.isNetworked()) {
            messages.send(sender, "private.offline", "player", targetName);
            return;
        }
        // Destinataire sur un autre serveur : ses réglages sont lus en base
        Tasks.async(plugin, sender, () -> directory.find(targetName).filter(NetworkPlayer::isOnline)
                        .map(target -> new Recipient(target.uuid(), target.name(),
                                bypass || preferences.read(target.uuid()).privateMessages())),
                found -> found.ifPresentOrElse(
                        recipient -> deliver(sender, recipient, text),
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

    private void deliver(Player sender, Recipient recipient, String text) {
        if (recipient.uuid().equals(sender.getUniqueId())) {
            messages.send(sender, "private.self");
            return;
        }
        if (!recipient.accepts()) {
            messages.send(sender, "private.disabled", "player", recipient.name());
            return;
        }
        ChatMessage message = chat.create(sender, ChatMessage.Type.PRIVATE, text, recipient.uuid(), recipient.name());
        sender.sendMessage(chat.line(sender, "private.sent", message));
        chat.rememberReply(sender.getUniqueId(), recipient.name());
        if (!preferences.get(sender.getUniqueId()).privateMessages()) {
            messages.send(sender, "private.own-disabled", "player", recipient.name());
        }
        boolean local = Bukkit.getPlayer(recipient.uuid()) != null;
        chat.send(message, () -> {
            // Destinataire sur un autre serveur et Redis en panne : le message n'est pas arrivé
            if (!local && sender.isOnline()) {
                messages.send(sender, "private.failed", "player", recipient.name());
            }
        });
    }
}
