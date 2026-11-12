package fr.eternom.eterChat.module.chat;

import fr.eternom.eterClan.api.ClanApi;
import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * /clanchat <message> (alias /c) : un message aux membres de son clan, sur tout le réseau. Le clan vient de l'API
 * d'EterClan (lecture en base, hors du thread principal). EterModeration le traite comme un message privé : muet
 * bloqué, filtre, journal.
 */
public class ClanChatCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ChatService chat;
    private final Messages messages;

    public ClanChatCommand(JavaPlugin plugin, ChatService chat, Messages messages) {
        this.plugin = plugin;
        this.chat = chat;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        if (args.length == 0) {
            messages.send(player, "clan-chat.usage");
            return true;
        }
        Optional<ClanApi> api = Bukkit.getPluginManager().isPluginEnabled("EterClan") ? ClanApi.get() : Optional.empty();
        if (api.isEmpty()) {
            messages.send(player, "clan-chat.unavailable");
            return true;
        }
        String text = String.join(" ", args);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<ClanApi.ClanInfo> clan;
            try {
                clan = api.get().clanOf(player.getUniqueId());
            } catch (RuntimeException e) {
                Bukkit.getScheduler().runTask(plugin, () -> messages.send(player, "clan-chat.unavailable"));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                clan.ifPresentOrElse(info -> chat.clan(player, text, info.tag(), info.members()),
                        () -> messages.send(player, "clan-chat.none"));
            });
        });
        return true;
    }
}
