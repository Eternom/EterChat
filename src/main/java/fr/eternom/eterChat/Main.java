package fr.eternom.eterChat;

import fr.eternom.eterChat.listeners.Commands;
import fr.eternom.eterChat.listeners.Events;
import fr.eternom.eterChat.module.chat.ChatFormatter;
import fr.eternom.eterChat.module.chat.ChatService;
import fr.eternom.eterChat.module.chat.Ranks;
import fr.eternom.eterChat.module.message.PlayerNames;
import fr.eternom.eterChat.module.message.PrivateMessages;
import fr.eternom.eterChat.module.preference.ChatPreferences;
import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    /** Version minimale d'EterLib : RedisMessenger, listOnline et server-display-name arrivent en 1.4.0. */
    private static final String REQUIRED_ETERLIB = "1.4.0";

    /** Préfixe des tables d'EterChat dans la base commune : eterchat_players, eterchat_ignores. */
    private static final String TABLE_PREFIX = "eterchat_";
    private static final long NAMES_REFRESH_TICKS = 10 * 20;

    private Messages messages;
    private ChatPreferences preferences;
    private ChatService chat;
    private PrivateMessages privateMessages;
    private PlayerNames names;

    @Override
    public void onEnable() {
        // En premier : vérifie la version d'EterLib (un EterLib < 1.3.0 n'a pas requireVersion, d'où le catch)
        try {
            if (!EterLib.requireVersion(this, REQUIRED_ETERLIB)) {
                return;
            }
        } catch (LinkageError tooOld) {
            getLogger().severe("EterLib " + REQUIRED_ETERLIB + " ou plus récent est nécessaire.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        EterLib lib = EterLib.get();
        messages = lib.messages(this, "en_us", "fr_fr");
        preferences = new ChatPreferences(lib.database(TABLE_PREFIX));

        Ranks ranks = Ranks.load();
        chat = new ChatService(this, messages, new ChatFormatter(messages), ranks, preferences,
                lib.getMessenger(), lib.getRedis(), lib.getServerName(), lib.getServerDisplayName());
        chat.start();
        privateMessages = new PrivateMessages(this, chat, lib.getPlayers(), messages);
        names = new PlayerNames(chat.isNetworked() ? lib.getPlayers() : null);

        new Commands(this);
        new Events(this);

        if (chat.isNetworked()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, names::refresh, 20, NAMES_REFRESH_TICKS);
        }
        getLogger().info("Chat " + (chat.isNetworked() ? "relié à tout le réseau (Redis)" : "limité à ce serveur (Redis désactivé)")
                + (ranks.isAvailable() ? ", grades LuckPerms" : ", sans LuckPerms"));
    }

    public Messages getMessages() {
        return messages;
    }

    public ChatPreferences getPreferences() {
        return preferences;
    }

    public ChatService getChat() {
        return chat;
    }

    public PrivateMessages getPrivateMessages() {
        return privateMessages;
    }

    public PlayerNames getNames() {
        return names;
    }
}
