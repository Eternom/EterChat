package fr.eternom.eterChat.module.chat;

import fr.eternom.eterLib.helper.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Mise en forme du chat :
 * - côté expéditeur, le texte du joueur (couleurs si permis, [item], @mentions), une seule fois ;
 * - côté destinataire, la ligne complète dans le format de sa langue (lang/ > chat.*, private.*).
 */
public class ChatFormatter {

    public static final String COLOR_PERMISSION = "eterchat.color";
    public static final String ITEM_PERMISSION = "eterchat.item";

    private static final Pattern MENTION = Pattern.compile("@[A-Za-z0-9_]{3,16}");
    private static final String ITEM_TAG = "[item]";

    /** Balises permises aux joueurs qui ont eterchat.color : couleurs et styles seulement (pas de clic, survol...). */
    private static final MiniMessage PLAYER_TAGS = MiniMessage.builder()
            .tags(TagResolver.resolver(StandardTags.color(), StandardTags.decorations(), StandardTags.gradient(),
                    StandardTags.rainbow(), StandardTags.reset()))
            .build();

    private final Messages messages;

    public ChatFormatter(Messages messages) {
        this.messages = messages;
    }

    /** Thread principal (lit l'objet en main). Le texte n'est jamais interprété sans eterchat.color. */
    public Component body(Player player, String text) {
        Component body = player.hasPermission(COLOR_PERMISSION) ? PLAYER_TAGS.deserialize(text) : Component.text(text);
        ItemStack item = player.getInventory().getItemInMainHand();
        if (player.hasPermission(ITEM_PERMISSION) && !item.isEmpty()) {
            // displayName() : nom entre crochets, avec l'infobulle de l'objet au survol
            body = body.replaceText(builder -> builder.matchLiteral(ITEM_TAG).once().replacement(item.displayName()));
        }
        CommandSender console = Bukkit.getConsoleSender();
        return body.replaceText(builder -> builder.match(MENTION)
                .replacement((match, original) -> messages.get(console, "chat.mention", "mention", match.group())));
    }

    /** La ligne telle que receiver la voit. key : chat.global, chat.staff, private.received... */
    public Component line(CommandSender receiver, String key, ChatMessage message) {
        String raw = messages.raw(receiver, key);
        if (raw == null) {
            return Component.text(key);
        }
        TagResolver tags = TagResolver.resolver(
                Placeholder.component("prefix", message.prefix()),
                Placeholder.component("suffix", message.suffix()),
                Placeholder.component("name", nameCard(receiver, message.senderName(), message.server())),
                Placeholder.component("message", message.body()),
                Placeholder.unparsed("server", message.server()),
                Placeholder.unparsed("target", message.targetName() == null ? "" : message.targetName()));
        return messages.render(raw, tags);
    }

    /** true si le texte contient @name (sans tenir compte des majuscules). */
    public static boolean mentions(ChatMessage message, String name) {
        String plain = PlainTextComponentSerializer.plainText().serialize(message.body()).toLowerCase(Locale.ROOT);
        String mention = "@" + name.toLowerCase(Locale.ROOT);
        int index = plain.indexOf(mention);
        while (index >= 0) {
            int end = index + mention.length();
            if (end == plain.length() || !isNameChar(plain.charAt(end))) {
                return true;
            }
            index = plain.indexOf(mention, end);
        }
        return false;
    }

    /** Pseudo avec sa fiche au survol (serveur) ; un clic prépare /msg. */
    private Component nameCard(CommandSender receiver, String name, String server) {
        return Component.text(name)
                .hoverEvent(HoverEvent.showText(messages.get(receiver, "chat.hover", "name", name, "server", server)))
                .clickEvent(ClickEvent.suggestCommand("/msg " + name + " "));
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
