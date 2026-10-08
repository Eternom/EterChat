package fr.eternom.eterChat;

import fr.eternom.eterChat.listeners.Commands;
import fr.eternom.eterChat.listeners.Events;
import fr.eternom.eterChat.module.chat.ChatFormatter;
import fr.eternom.eterChat.module.chat.ChatService;
import fr.eternom.eterChat.module.chat.Ranks;
import fr.eternom.eterChat.module.gui.ChatGui;
import fr.eternom.eterChat.module.message.PrivateMessages;
import fr.eternom.eterChat.module.preference.ChatPreferences;
import fr.eternom.eterChat.module.preference.PreferenceActions;
import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    /** Version minimale d'EterLib : textes communs et outils partagés (Frame, Money, NetworkBus) depuis 1.6.0. */
    private static final String REQUIRED_ETERLIB = "1.9.1";

    /** Préfixe des tables d'EterChat dans la base commune : eterchat_players, eterchat_ignores. */
    private static final String TABLE_PREFIX = "eterchat_";

    private Messages messages;
    private ChatPreferences preferences;
    private ChatService chat;
    private PrivateMessages privateMessages;
    private PreferenceActions actions;
    private ChatGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
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
                lib.network(this, "eterchat", messages), lib.getRedis(), lib.getServerName(), lib.getServerDisplayName());
        chat.start();
        privateMessages = new PrivateMessages(this, chat, preferences, lib.getPlayers(), messages);
        actions = new PreferenceActions(this, preferences, lib.getPlayers(), messages);
        gui = new ChatGui(this, preferences, actions, messages, lib.backButton(getConfig().getString("menus.chat.back-command", "")));

        new Commands(this);
        new Events(this);
        getLogger().info("Chat relié à tout le réseau" + (ranks.isAvailable() ? ", grades LuckPerms" : ", sans LuckPerms"));
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

    public PreferenceActions getActions() {
        return actions;
    }

    public ChatGui getGui() {
        return gui;
    }
}
