package fr.eternom.eterChat.listeners;

import fr.eternom.eterChat.Main;
import fr.eternom.eterChat.module.chat.StaffChatCommand;
import fr.eternom.eterChat.module.gui.ChatMenuCommand;
import fr.eternom.eterChat.module.message.MessageCommand;
import fr.eternom.eterChat.module.preference.ChatPreferences.Setting;
import fr.eternom.eterChat.module.preference.IgnoreCommand;
import fr.eternom.eterChat.module.preference.ToggleCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Objects;

public class Commands {

    public Commands(Main main) {
        register(main, "msg", new MessageCommand(main.getPrivateMessages(), main.getNames(), main.getMessages(), false));
        register(main, "reply", new MessageCommand(main.getPrivateMessages(), main.getNames(), main.getMessages(), true));
        register(main, "ignore", new IgnoreCommand(main.getActions(), main.getPreferences(), main.getNames(), main.getMessages()));
        register(main, "msgtoggle", new ToggleCommand(main.getActions(), main.getMessages(), Setting.PRIVATE_MESSAGES));
        register(main, "notifications", new ToggleCommand(main.getActions(), main.getMessages(), Setting.NOTIFICATIONS));
        register(main, "socialspy", new ToggleCommand(main.getActions(), main.getMessages(), Setting.SOCIAL_SPY));
        register(main, "staffchat", new StaffChatCommand(main.getChat(), main.getActions(), main.getMessages()));
        register(main, "chat", new ChatMenuCommand(main.getGui(), main.getMessages()));
    }

    private void register(Main main, String name, CommandExecutor executor) {
        PluginCommand command = Objects.requireNonNull(main.getCommand(name), "Commande absente du plugin.yml : " + name);
        command.setExecutor(executor);
        command.setTabCompleter(executor instanceof TabCompleter completer ? completer : (sender, cmd, label, args) -> List.of());
    }
}
