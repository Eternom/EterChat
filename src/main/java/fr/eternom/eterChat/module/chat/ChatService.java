package fr.eternom.eterChat.module.chat;

import fr.eternom.eterChat.module.chat.ChatMessage.Type;
import fr.eternom.eterLib.module.rank.Ranks;
import fr.eternom.eterLib.module.rank.Ranks.Rank;
import fr.eternom.eterChat.module.preference.ChatPreferences;
import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.cache.RedisCache;
import fr.eternom.eterLib.helper.cache.NetworkBus;
import fr.eternom.eterLib.helper.message.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Envoi et réception des messages du chat (global, staff, privés) sur tout le réseau.
 *
 * Chaque message est d'abord distribué sur ce serveur, puis envoyé aux autres par le bus réseau d'EterLib (canal
 * "eterchat", type "chat" ; le serveur d'origine ignore le sien). Redis en panne : le chat continue sur chaque serveur.
 */
public class ChatService {

    public static final String STAFF_PERMISSION = "eterchat.staff";
    public static final String SPY_PERMISSION = "eterchat.socialspy";

    private static final String CHAT = "chat";
    private static final Duration REPLY_TTL = Duration.ofHours(1);
    private static final long WARNING_INTERVAL_MILLIS = 60_000;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final ChatFormatter formatter;
    private final Ranks ranks;
    private final ChatPreferences preferences;
    private final NetworkBus bus;
    private final RedisCache redis;
    private final String serverName;
    private final String serverDisplayName;

    /** Dernier correspondant de chaque joueur connecté ici, pour /r ; recopié dans Redis pour suivre le joueur. */
    private final Map<UUID, String> replies = new ConcurrentHashMap<>();
    private volatile long lastWarning;

    public ChatService(JavaPlugin plugin, Messages messages, ChatFormatter formatter, Ranks ranks, ChatPreferences preferences,
                       NetworkBus bus, RedisCache redis, String serverName, String serverDisplayName) {
        this.plugin = plugin;
        this.messages = messages;
        this.formatter = formatter;
        this.ranks = ranks;
        this.preferences = preferences;
        this.bus = bus;
        this.redis = redis;
        this.serverName = serverName;
        this.serverDisplayName = serverDisplayName;
    }

    public void start() {
        bus.on(CHAT, data -> deliver(ChatMessage.fromJson(data)));
    }

    /** Thread principal : message de chat d'un joueur, vers le global ou le staff. */
    public void chat(Player player, String text, boolean staff) {
        send(create(player, staff ? Type.STAFF : Type.GLOBAL, text, null, null));
    }

    /** Thread principal : met en forme le texte et le grade de l'expéditeur. */
    public ChatMessage create(Player sender, Type type, String text, UUID target, String targetName) {
        // Le grade affiché d'EterLib : LuckPerms, avec le badge (tag de clan...) à la place du préfixe
        Rank rank = ranks.of(sender);
        return new ChatMessage(type, serverName, serverDisplayName, sender.getUniqueId(), sender.getName(),
                rank.prefix(), rank.suffix(), formatter.body(sender, text), target, targetName, null);
    }

    /** Thread principal : un message au clan (tag), pour ses membres sur tout le réseau. */
    public void clan(Player sender, String text, String tag, List<UUID> members) {
        Rank rank = ranks.of(sender);
        send(new ChatMessage(Type.CLAN, serverName, serverDisplayName, sender.getUniqueId(), sender.getName(),
                rank.prefix(), rank.suffix(), formatter.body(sender, text), null, tag, members));
    }

    /** Thread principal : distribue ici, puis aux autres serveurs. */
    public void send(ChatMessage message) {
        send(message, () -> {
        });
    }

    /** Idem ; onFailure est lancé sur le thread principal si Redis n'a pas pu transmettre le message. */
    public void send(ChatMessage message, Runnable onFailure) {
        deliver(message);
        bus.publish(CHAT, message.toJson(), onFailure);
    }

    /** Le bus réseau d'EterChat (notify : prévenir un joueur où qu'il soit, et le garder dans sa boîte /inbox). */
    public fr.eternom.eterLib.helper.cache.NetworkBus bus() {
        return bus;
    }

    public String serverName() {
        return serverName;
    }

    public Component line(CommandSender receiver, String key, ChatMessage message) {
        return formatter.line(receiver, key, message);
    }

    /** Thread principal : retient le correspondant de player pour /r. */
    public void rememberReply(UUID player, String name) {
        replies.put(player, name);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                redis.set(replyKey(player), name, REPLY_TTL);
            } catch (RuntimeException e) {
                warn("Correspondant de /r non enregistré dans Redis", e);
            }
        });
    }

    /** Bloquant (Redis) : dernier correspondant, même s'il date d'un autre serveur. */
    public Optional<String> findReply(UUID player) {
        String local = replies.get(player);
        if (local != null) {
            return Optional.ofNullable(local);
        }
        return redis.get(replyKey(player));
    }

    public void forget(UUID player) {
        replies.remove(player);
    }

    /** Thread principal : affiche le message aux destinataires présents sur ce serveur. */
    private void deliver(ChatMessage message) {
        switch (message.type()) {
            case GLOBAL -> deliverGlobal(message);
            case STAFF -> deliverStaff(message);
            case PRIVATE -> deliverPrivate(message);
            case CLAN -> deliverClan(message);
        }
    }

    /** Chat global : à tous les joueurs d'ici, sauf ceux qui ignorent l'auteur. */
    private void deliverGlobal(ChatMessage message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean self = uuid.equals(message.sender());
            if (!self && preferences.isIgnoring(uuid, message.sender())) {
                continue;
            }
            player.sendMessage(line(player, "chat.global", message));
            if (!self && ChatFormatter.mentions(message, player.getName()) && preferences.get(uuid).notifications()) {
                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
                messages.actionBar(player, "chat.mentioned", "player", message.senderName());
            }
        }
        Bukkit.getConsoleSender().sendMessage(line(Bukkit.getConsoleSender(), "chat.global", message));
    }

    private void deliverStaff(ChatMessage message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(STAFF_PERMISSION)) {
                player.sendMessage(line(player, "chat.staff", message));
            }
        }
        Bukkit.getConsoleSender().sendMessage(line(Bukkit.getConsoleSender(), "chat.staff", message));
    }

    /** Chat de clan : aux membres présents ici. La console du serveur d'origine en garde une trace. */
    private void deliverClan(ChatMessage message) {
        if (message.members() == null) {
            return;
        }
        for (UUID member : message.members()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                player.sendMessage(line(player, "chat.clan", message));
            }
        }
        if (message.origin().equals(serverName)) {
            Bukkit.getConsoleSender().sendMessage(line(Bukkit.getConsoleSender(), "chat.clan", message));
        }
    }

    /** Au destinataire s'il est ici (et ne l'ignore pas), et aux espions d'ici. L'expéditeur a déjà sa copie. */
    private void deliverPrivate(ChatMessage message) {
        Player target = Bukkit.getPlayer(message.target());
        if (target != null && !preferences.isIgnoring(target.getUniqueId(), message.sender())) {
            target.sendMessage(line(target, "private.received", message));
            rememberReply(target.getUniqueId(), message.senderName());
            if (preferences.get(target.getUniqueId()).notifications()) {
                target.playSound(target, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.6f);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!uuid.equals(message.sender()) && !uuid.equals(message.target())
                    && preferences.get(uuid).socialSpy() && player.hasPermission(SPY_PERMISSION)) {
                player.sendMessage(line(player, "private.spy", message));
            }
        }
        if (message.origin().equals(serverName)) {
            Bukkit.getConsoleSender().sendMessage(line(Bukkit.getConsoleSender(), "private.spy", message));
        }
    }

    /** Une panne de Redis ne doit pas remplir la console : un avertissement par minute au plus. */
    private void warn(String context, RuntimeException e) {
        long now = System.currentTimeMillis();
        if (now - lastWarning > WARNING_INTERVAL_MILLIS) {
            lastWarning = now;
            plugin.getLogger().log(Level.WARNING, context + " : " + e.getMessage());
        }
    }

    private static String replyKey(UUID player) {
        return "chat:reply:" + player;
    }
}
