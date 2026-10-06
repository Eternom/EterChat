package fr.eternom.eterChat.module.preference;

import fr.eternom.eterLib.helper.task.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Lit les réglages de chat d'un joueur à son arrivée, les oublie à son départ. */
public class PreferenceListener implements Listener {

    private final JavaPlugin plugin;
    private final ChatPreferences preferences;

    public PreferenceListener(JavaPlugin plugin, ChatPreferences preferences) {
        this.plugin = plugin;
        this.preferences = preferences;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Tasks.async(plugin, () -> {
            preferences.load(player.getUniqueId());
            if (!player.isOnline()) {
                preferences.forget(player.getUniqueId()); // parti pendant la lecture
            }
        }, "Réglages de chat illisibles pour " + player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        preferences.forget(event.getPlayer().getUniqueId());
    }
}
