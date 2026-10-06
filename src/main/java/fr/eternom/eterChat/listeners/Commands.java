package fr.eternom.eterChat.listeners;

import fr.eternom.eterChat.Main;
import fr.eternom.eterChat.module.chat.StaffChatCommand;
import fr.eternom.eterChat.module.message.MessageCommand;
import fr.eternom.eterChat.module.preference.IgnoreCommand;
import fr.eternom.eterChat.module.preference.ToggleCommand;
import fr.eternom.eterLib.EterLib;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Objects;

public class Commands {

    public Commands(Main main) {
        register(main, "msg", new MessageCommand(main.getPrivateMessages(), main.getNames(), main.getMessages(), false));
        register(main, "reply", new MessageCommand(main.getPrivateMessages(), main.getNames(), main.getMessages(), true));
        register(main, "ignore", new IgnoreCommand(main, main.getPreferences(), EterLib.get().getPlayers(), main.getNames(),
                main.getMessages()));
        register(main, "notifications", new ToggleCommand(main, main.getPreferences(), main.getMessages(),
                ToggleCommand.Kind.NOTIFICATIONS));
        register(main, "socialspy", new ToggleCommand(main, main.getPreferences(), main.getMessages(),
                ToggleCommand.Kind.SOCIAL_SPY));
        register(main, "staffchat", new StaffChatCommand(main, main.getChat(), main.getPreferences(), main.getMessages()));
    }

    private void register(Main main, String name, CommandExecutor executor) {
        PluginCommand command = Objects.requireNonNull(main.getCommand(name), "Commande absente du plugin.yml : " + name);
        command.setExecutor(executor);
        command.setTabCompleter(executor instanceof TabCompleter completer ? completer : (sender, cmd, label, args) -> List.of());
    }
}
