package fr.eternom.eterChat.module.gui;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Joueurs ignorés, 6 lignes :
 * <pre>
 *  ▣ ▣ ▢ ▢ ▢ ▢ ▢ ▣ ▣
 *  ▣ · · · · · · · ▣     · = têtes des joueurs ignorés, 28 par page
 *  ▢ · · · · · · · ▢
 *  ▢ · · · · · · · ▢
 *  ▣ · · · · · · · ▣
 *  ◀ ▣ ▢ « ▢ + ▢ ▣ ▶     « = retour, + = ignorer un joueur, ◀ ▶ = pages
 * </pre>
 * Clic sur une tête : confirmation, puis le joueur n'est plus ignoré.
 */
class IgnoredMenu implements Menu {

    private record Ignored(UUID uuid, String name) {
    }

    private static final int PREVIOUS = 45;
    private static final int BACK = 48;
    private static final int ADD = 50;
    private static final int NEXT = 53;
    private static final int EMPTY = 22;
    private static final Set<Integer> ACCENT_FRAME = Set.of(0, 1, 7, 8, 9, 17, 36, 44, 46, 52);
    private static final List<Integer> SLOTS = innerSlots();

    private final ChatGui gui;
    private final Messages messages;
    private final Player viewer;
    private final List<Ignored> ignored;
    private final Inventory inventory;
    private final Map<Integer, Ignored> ignoredAtSlot = new HashMap<>();
    private int page;

    IgnoredMenu(ChatGui gui, Player viewer) {
        this.gui = gui;
        this.messages = gui.messages();
        this.viewer = viewer;
        this.ignored = gui.preferences().ignored(viewer.getUniqueId()).entrySet().stream()
                .map(entry -> new Ignored(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
        this.inventory = Bukkit.createInventory(this, 54, text("gui.ignored-menu.title"));
        render();
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        Ignored target = ignoredAtSlot.get(slot);
        if (target != null) {
            Sounds.click(player);
            gui.confirmUnignore(player, target.uuid(), target.name());
        } else if (slot == PREVIOUS && page > 0) {
            page--;
            Sounds.page(player);
            render();
        } else if (slot == NEXT && hasNextPage()) {
            page++;
            Sounds.page(player);
            render();
        } else if (slot == BACK) {
            Sounds.page(player);
            gui.open(player);
        } else if (slot == ADD || slot == EMPTY && ignored.isEmpty()) {
            Sounds.click(player);
            gui.askIgnore(player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void render() {
        inventory.clear();
        ignoredAtSlot.clear();
        ItemStack accent = Items.pane(Material.ORANGE_STAINED_GLASS_PANE);
        ItemStack neutral = Items.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!SLOTS.contains(slot)) {
                inventory.setItem(slot, ACCENT_FRAME.contains(slot) ? accent : neutral);
            }
        }

        int start = page * SLOTS.size();
        for (int i = 0; i < SLOTS.size() && start + i < ignored.size(); i++) {
            Ignored target = ignored.get(start + i);
            ignoredAtSlot.put(SLOTS.get(i), target);
            inventory.setItem(SLOTS.get(i), Items.head(Bukkit.createProfile(target.uuid(), target.name()),
                    text("gui.ignored-menu.player", "player", target.name()), List.of(text("gui.ignored-menu.unignore"))));
        }
        if (ignored.isEmpty()) {
            inventory.setItem(EMPTY, Items.item(Material.TOTEM_OF_UNDYING, text("gui.ignored-menu.empty"),
                    List.of(text("gui.ignored-menu.empty-lore"))));
        }

        if (page > 0) {
            inventory.setItem(PREVIOUS, Items.item(Material.ARROW, text("gui.previous"), List.of(pageLine(page))));
        }
        if (hasNextPage()) {
            inventory.setItem(NEXT, Items.item(Material.ARROW, text("gui.next"), List.of(pageLine(page + 2))));
        }
        inventory.setItem(BACK, Items.item(Material.OAK_DOOR, text("gui.back"), List.of()));
        inventory.setItem(ADD, Items.item(Material.NAME_TAG, text("gui.ignored-menu.add"), List.of(text("gui.ignored-menu.add-lore"))));
    }

    private Component pageLine(int shownPage) {
        return text("gui.page", "page", String.valueOf(shownPage), "pages", String.valueOf(pageCount()));
    }

    private Component text(String key, String... placeholders) {
        return messages.get(viewer, key, placeholders);
    }

    private int pageCount() {
        return Math.max(1, (ignored.size() + SLOTS.size() - 1) / SLOTS.size());
    }

    private boolean hasNextPage() {
        return page + 1 < pageCount();
    }

    /** Lignes 2 à 5, colonnes 2 à 8 : l'intérieur du cadre. */
    private static List<Integer> innerSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int column = 1; column <= 7; column++) {
                slots.add(row * 9 + column);
            }
        }
        return List.copyOf(slots);
    }
}
