package fr.eternom.eterChat.module.gui;

import fr.eternom.eterLib.helper.message.Messages;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fenêtres natives de Minecraft (Dialogs, client 1.21.6+) du menu /chat : saisir un pseudo à ignorer,
 * confirmer qu'on ne l'ignore plus. Réponses traitées sur le thread principal ; « Annuler » appelle onCancel.
 */
class ChatDialogs {

    private static final String NAME = "name";
    private static final ClickCallback.Options ONE_USE = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(5))
            .build();

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
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(base)
                .type(DialogType.confirmation(
                        button(player, confirmKey, onConfirm),
                        button(player, "dialog.cancel", response -> onCancel.run())))));
    }

    private ActionButton button(Player player, String key, Consumer<DialogResponseView> onClick) {
        return ActionButton.builder(messages.get(player, key))
                .action(DialogAction.customClick(
                        // Le clic arrive du réseau : on repasse sur le thread principal
                        (response, audience) -> Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                onClick.accept(response);
                            }
                        }),
                        ONE_USE))
                .build();
    }
}
