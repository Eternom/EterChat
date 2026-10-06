package fr.eternom.eterChat.listeners;

import fr.eternom.eterChat.Main;
import fr.eternom.eterChat.module.chat.ChatListener;
import fr.eternom.eterChat.module.preference.PreferenceListener;
import org.bukkit.event.Listener;

public class Events {

    public Events(Main main) {
        register(main, new ChatListener(main, main.getChat(), main.getPreferences()));
        register(main, new PreferenceListener(main, main.getPreferences()));
    }

    private void register(Main main, Listener listener) {
        main.getServer().getPluginManager().registerEvents(listener, main);
    }
}
