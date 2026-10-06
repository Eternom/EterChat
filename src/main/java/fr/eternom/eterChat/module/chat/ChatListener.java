package fr.eternom.eterChat.module.chat;

import fr.eternom.eterChat.module.preference.ChatPreferences;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Remplace le chat de Minecraft par celui d'EterChat.
 * Priorité HIGH et ignoreCancelled : un plugin de modération qui annule le message avant (mute, anti-spam) a le dernier mot.
 */
public class ChatListener implements Listener {

    private final JavaPlugin plugin;
    private final ChatService chat;
    private final ChatPreferences preferences;

    public ChatListener(JavaPlugin plugin, ChatService chat, ChatPreferences preferences) {
        this.plugin = plugin;
        this.chat = chat;
        this.preferences = preferences;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        // Le message est mis en forme sur le thread principal (grade, objet en main)
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            boolean staff = preferences.get(player.getUniqueId()).staffChannel() && player.hasPermission(ChatService.STAFF_PERMISSION);
            chat.chat(player, text, staff);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        chat.forget(event.getPlayer().getUniqueId());
    }
}
