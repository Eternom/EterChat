package fr.eternom.eterChat.module.message;

import fr.eternom.eterChat.module.chat.ChatMessage;
import fr.eternom.eterChat.module.chat.ChatService;
import fr.eternom.eterChat.module.preference.ChatPreferences;
import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.task.Tasks;
import fr.eternom.eterLib.module.player.PlayerDirectory;
import fr.eternom.eterLib.module.player.PlayerDirectory.NetworkPlayer;
import fr.eternom.eterLib.module.vanish.Vanish;
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

    /** Destinataire trouvé, s'il accepte le message, et le serveur où il est. */
    private record Recipient(UUID uuid, String name, boolean accepts, String server) {
    }

    /** Un pas entre deux courriers d'un même joueur (anti-spam des boîtes). */
    private static final long MAIL_COOLDOWN_MILLIS = 10_000;
    private final java.util.Map<UUID, Long> lastMail = new java.util.concurrent.ConcurrentHashMap<>();

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
        send(sender, targetName, text, false);
    }

    /**
     * reply : une réponse (/r), qui atteint aussi un invisible (le staff qui a écrit).
     */
    private void send(Player sender, String targetName, String text, boolean reply) {
        boolean bypass = sender.hasPermission(BYPASS_PERMISSION);
        // Un invisible (vanish du staff) est hors ligne pour qui ne le voit pas, sauf pour lui répondre
        Vanish vanish = EterLib.get().getVanish();
        Player local = Bukkit.getPlayerExact(targetName);
        if (local != null && (reply || vanish.canSee(sender, local.getUniqueId()))) {
            UUID uuid = local.getUniqueId();
            deliver(sender, new Recipient(uuid, local.getName(), bypass || preferences.get(uuid).privateMessages(),
                    chat.serverName()), text, reply);
            return;
        }
        // Destinataire sur un autre serveur : ses réglages sont lus en base. Hors ligne (ou invisible pour l'expéditeur) :
        // un courrier (server null), s'il accepte les messages privés et n'ignore pas l'expéditeur
        UUID senderId = sender.getUniqueId();
        Tasks.async(plugin, sender, () -> directory.find(targetName).map(target -> {
                    boolean accepts = bypass || preferences.read(target.uuid()).privateMessages();
                    if (target.isOnline() && (reply || vanish.canSee(sender, target.uuid()))) {
                        return new Recipient(target.uuid(), target.name(), accepts, target.server());
                    }
                    return new Recipient(target.uuid(), target.name(),
                            accepts && !preferences.readIgnoring(target.uuid(), senderId), null);
                }),
                found -> found.ifPresentOrElse(
                        recipient -> {
                            if (recipient.server() == null) {
                                mail(sender, recipient, text);
                            } else {
                                deliver(sender, recipient, text, reply);
                            }
                        },
                        () -> messages.send(sender, "private.offline", "player", targetName)),
                () -> messages.send(sender, "private.error"));
    }

    /** Thread principal : un courrier pour un joueur hors ligne, gardé dans sa boîte (/inbox) et annoncé à sa connexion. */
    private void mail(Player sender, Recipient recipient, String text) {
        if (recipient.uuid().equals(sender.getUniqueId())) {
            messages.send(sender, "private.self");
            return;
        }
        if (!recipient.accepts()) {
            messages.send(sender, "private.disabled", "player", recipient.name());
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastMail.get(sender.getUniqueId());
        if (last != null && now - last < MAIL_COOLDOWN_MILLIS) {
            messages.send(sender, "private.mail-wait", "seconds",
                    String.valueOf((MAIL_COOLDOWN_MILLIS - (now - last) + 999) / 1000));
            return;
        }
        lastMail.put(sender.getUniqueId(), now);
        chat.bus().notify(recipient.uuid(), "private.mail", true, "player", sender.getName(), "message", text);
        messages.send(sender, "private.mail-sent", "player", recipient.name(), "message", text);
    }

    /** Thread principal : répond au dernier correspondant. */
    public void reply(Player sender, String text) {
        UUID uuid = sender.getUniqueId();
        Tasks.async(plugin, sender, () -> chat.findReply(uuid),
                (Optional<String> name) -> name.ifPresentOrElse(
                        target -> send(sender, target, text, true),
                        () -> messages.send(sender, "private.no-reply")),
                () -> messages.send(sender, "private.error"));
    }

    private void deliver(Player sender, Recipient recipient, String text, boolean reply) {
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
