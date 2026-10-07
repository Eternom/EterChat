package fr.eternom.eterChat.module.gui;

import fr.eternom.eterLib.helper.gui.Dialogs;
import fr.eternom.eterLib.helper.message.Messages;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fenêtres natives de Minecraft (Dialogs, client 1.21.6+) du menu /chat : saisir un pseudo à ignorer,
 * confirmer qu'on ne l'ignore plus. Boutons et thread principal : Dialogs d'EterLib ; « Annuler » appelle onCancel.
 */
class ChatDialogs {

    private static final String NAME = "name";

    private final JavaPlugin plugin;
    private final Messages messages;

    ChatDialogs(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    void askPlayer(Player player, Consumer<String> onConfirm, Runnable onCancel) {
        DialogBase base = DialogBase.builder(messages.get(player, "dialog.ignore.title"))
                .body(List.of(DialogBody.plainMessage(messages.get(player, "dialog.ignore.body"))))
                .inputs(List.of(DialogInput.text(NAME, messages.get(player, "dialog.ignore.name")).maxLength(16).build()))
                .build();
        show(player, base, "dialog.ignore.confirm", response -> {
            String name = response.getText(NAME);
            if (name == null || name.isBlank()) {
                onCancel.run();
            } else {
                onConfirm.accept(name.trim());
            }
        }, onCancel);
    }

    void confirmUnignore(Player player, String name, Runnable onConfirm, Runnable onCancel) {
        DialogBase base = DialogBase.builder(messages.get(player, "dialog.unignore.title"))
                .body(List.of(DialogBody.plainMessage(messages.get(player, "dialog.unignore.body", "player", name))))
                .build();
        show(player, base, "dialog.unignore.confirm", response -> onConfirm.run(), onCancel);
    }

    private void show(Player player, DialogBase base, String confirmKey, Consumer<DialogResponseView> onConfirm, Runnable onCancel) {
        Dialogs.show(plugin, player, base, messages.get(player, confirmKey), messages.get(player, "dialog.cancel"), onConfirm, onCancel);
    }
}
