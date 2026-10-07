package fr.eternom.eterChat.module.gui;

import fr.eternom.eterChat.module.preference.ChatPreferences;
import fr.eternom.eterChat.module.preference.ChatPreferences.Setting;
import fr.eternom.eterChat.module.preference.PreferenceActions;
import fr.eternom.eterLib.helper.gui.BackButton;
import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** Ouvre le menu /chat et branche ses boutons sur les réglages et les fenêtres de dialogue. Thread principal. */
public class ChatGui {

    private final ChatPreferences preferences;
    private final PreferenceActions actions;
    private final ChatDialogs dialogs;
    private final Messages messages;
    private final BackButton backButton;

    public ChatGui(JavaPlugin plugin, ChatPreferences preferences, PreferenceActions actions, Messages messages,
                   BackButton backButton) {
        this.preferences = preferences;
        this.actions = actions;
        this.dialogs = new ChatDialogs(plugin, messages);
        this.messages = messages;
        this.backButton = backButton;
    }

    public void open(Player player) {
        player.openInventory(new ChatMenu(this, player).getInventory());
    }

    void openIgnored(Player player) {
        player.openInventory(new IgnoredMenu(this, player).getInventory());
    }

    void toggle(Player player, Setting setting) {
        actions.toggle(player, setting);
    }

    void askIgnore(Player player) {
        Runnable reopen = () -> openIgnored(player);
        player.closeInventory();
        dialogs.askPlayer(player, name -> actions.ignore(player, name, reopen), reopen);
    }

    void confirmUnignore(Player player, UUID target, String name) {
        Runnable reopen = () -> openIgnored(player);
        player.closeInventory();
        dialogs.confirmUnignore(player, name, () -> actions.unignore(player, target, name, reopen), reopen);
    }

    ChatPreferences preferences() {
        return preferences;
    }

    Messages messages() {
        return messages;
    }

    BackButton backButton() {
        return backButton;
    }
}
