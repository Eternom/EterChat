package fr.eternom.eterChat.module.gui;

import fr.eternom.eterChat.module.chat.ChatService;
import fr.eternom.eterChat.module.preference.ChatPreferences.Setting;
import fr.eternom.eterChat.module.preference.ChatPreferences.Settings;
import fr.eternom.eterLib.helper.gui.Items;
import fr.eternom.eterLib.helper.gui.Menu;
import fr.eternom.eterLib.helper.gui.Sounds;
import fr.eternom.eterLib.helper.message.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Menu /chat, 5 lignes :
 * <pre>
 *  ▣ ▣ ▢ ▢ ☺ ▢ ▢ ▣ ▣     ☺ = joueur (résumé de ses réglages)
 *  ▣ · · · · · · · ▣
 *  ▢ · ✉ · ♪ · ☠ · ▢     ✉ = messages privés, ♪ = notifications, ☠ = joueurs ignorés
 *  ▣ · · ★ · ◎ · · ▣     ★ = canal staff, ◎ = espion (selon les permissions)
 *  ▣ ▣ ▢ ▢ ▢ ▢ ▢ ▣ ▣
 * </pre>
 * Un clic sur un réglage l'active ou le coupe ; l'icône brille quand il est activé.
 */
class ChatMenu implements Menu {

    private static final int INFO = 4;
    private static final int PRIVATE_MESSAGES = 20;
    private static final int NOTIFICATIONS = 22;
    private static final int IGNORED = 24;
    private static final int STAFF_CHANNEL = 30;
    private static final int SOCIAL_SPY = 32;
    private static final Set<Integer> ACCENT_FRAME = Set.of(0, 1, 7, 8, 9, 17, 27, 35, 36, 37, 43, 44);

    private final ChatGui gui;
    private final Messages messages;
    private final Player viewer;
    private final Inventory inventory;

    ChatMenu(ChatGui gui, Player viewer) {
        this.gui = gui;
        this.messages = gui.messages();
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 45, text("gui.title"));
        render();
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        Setting setting = switch (slot) {
            case PRIVATE_MESSAGES -> Setting.PRIVATE_MESSAGES;
            case NOTIFICATIONS -> Setting.NOTIFICATIONS;
            case STAFF_CHANNEL -> player.hasPermission(ChatService.STAFF_PERMISSION) ? Setting.STAFF_CHANNEL : null;
            case SOCIAL_SPY -> player.hasPermission(ChatService.SPY_PERMISSION) ? Setting.SOCIAL_SPY : null;
            default -> null;
        };
        if (setting != null) {
            Sounds.click(player);
            gui.toggle(player, setting);
            render();
        } else if (slot == IGNORED) {
            Sounds.page(player);
            gui.openIgnored(player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void render() {
        inventory.clear();
        ItemStack accent = Items.pane(Material.ORANGE_STAINED_GLASS_PANE);
        ItemStack neutral = Items.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == 4 || column == 0 || column == 8) {
                inventory.setItem(slot, ACCENT_FRAME.contains(slot) ? accent : neutral);
            }
        }

        Settings settings = gui.preferences().get(viewer.getUniqueId());
        int ignored = gui.preferences().ignored(viewer.getUniqueId()).size();
        inventory.setItem(INFO, Items.head(viewer.getPlayerProfile(), text("gui.info.name", "player", viewer.getName()), List.of(
                text("gui.info.private-messages").append(state(settings.privateMessages())),
                text("gui.info.notifications").append(state(settings.notifications())),
                text("gui.info.ignored", "count", String.valueOf(ignored)),
                text("gui.info.channel").append(text(settings.staffChannel() ? "gui.channel.staff" : "gui.channel.global")))));

        inventory.setItem(PRIVATE_MESSAGES, toggleItem(Material.WRITABLE_BOOK, "private-messages", settings.privateMessages()));
        inventory.setItem(NOTIFICATIONS, toggleItem(Material.BELL, "notifications", settings.notifications()));
        inventory.setItem(IGNORED, Items.item(Material.BARRIER, text("gui.ignored.name"), List.of(
                text("gui.ignored.lore"),
                Component.empty(),
                text("gui.ignored.count", "count", String.valueOf(ignored)),
                text("gui.ignored.click"))));
        if (viewer.hasPermission(ChatService.STAFF_PERMISSION)) {
            inventory.setItem(STAFF_CHANNEL, toggleItem(Material.NETHER_STAR, "staff", settings.staffChannel()));
        }
        if (viewer.hasPermission(ChatService.SPY_PERMISSION)) {
            inventory.setItem(SOCIAL_SPY, toggleItem(Material.SPYGLASS, "socialspy", settings.socialSpy()));
        }
    }

    /** Réglage activable : nom, description, état actuel ; brillant quand il est activé. */
    private ItemStack toggleItem(Material material, String key, boolean enabled) {
        return Items.item(material, text("gui." + key + ".name"), List.of(
                text("gui." + key + ".lore"),
                Component.empty(),
                text(enabled ? "gui.state.enabled" : "gui.state.disabled"),
                text("gui.click-toggle")), enabled);
    }

    private Component state(boolean enabled) {
        return text(enabled ? "gui.state.short-on" : "gui.state.short-off");
    }

    private Component text(String key, String... placeholders) {
        return messages.get(viewer, key, placeholders);
    }
}
