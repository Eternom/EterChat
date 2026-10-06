package fr.eternom.eterChat.module.message;

import fr.eternom.eterLib.module.player.PlayerDirectory;
import fr.eternom.eterLib.module.player.PlayerDirectory.NetworkPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Pseudos proposés avec Tab (/msg, /ignore) : les joueurs d'ici, plus ceux des autres serveurs si le chat est
 * relié au réseau. La liste réseau est relue en tâche de fond ({@link #refresh}) : Tab ne touche jamais à la base.
 */
public class PlayerNames {

    private final PlayerDirectory directory; // null : seulement les joueurs d'ici
    private volatile List<String> network = List.of();

    public PlayerNames(PlayerDirectory directory) {
        this.directory = directory;
    }

    /** Bloquant (base) : tâche de fond régulière. */
    public void refresh() {
        if (directory != null) {
            network = directory.listOnline().stream().map(NetworkPlayer::name).toList();
        }
    }

    public List<String> complete(String start) {
        String prefix = start.toLowerCase(Locale.ROOT);
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Stream.concat(Bukkit.getOnlinePlayers().stream().map(Player::getName), network.stream())
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .forEach(names::add);
        return List.copyOf(names);
    }
}
