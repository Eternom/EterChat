package fr.eternom.eterChat.module.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Préfixe et suffixe LuckPerms d'un joueur, pour le chat. Sans LuckPerms, ils sont vides.
 * Même lecture que dans EterTab : codes « & » acceptés, sinon MiniMessage, et une espace ajoutée si besoin.
 */
public class Ranks {

    public record Rank(Component prefix, Component suffix) {

        static final Rank NONE = new Rank(Component.empty(), Component.empty());
    }

    private final LuckPerms luckPerms; // null si LuckPerms n'est pas installé

    private Ranks(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public static Ranks load() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return new Ranks(null);
        }
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        return new Ranks(provider == null ? null : provider.getProvider());
    }

    public boolean isAvailable() {
        return luckPerms != null;
    }

    public Rank of(Player player) {
        if (luckPerms == null) {
            return Rank.NONE;
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return Rank.NONE;
        }
        CachedMetaData meta = user.getCachedData().getMetaData();
        return new Rank(spaced(parse(meta.getPrefix()), true), spaced(parse(meta.getSuffix()), false));
    }

    private static Component spaced(Component part, boolean prefix) {
        String plain = PlainTextComponentSerializer.plainText().serialize(part);
        if (plain.isBlank()) {
            return Component.empty();
        }
        if (prefix) {
            return plain.endsWith(" ") ? part : part.append(Component.space());
        }
        return plain.startsWith(" ") ? part : Component.space().append(part);
    }

    private static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (text.indexOf('&') >= 0 || text.indexOf('§') >= 0) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text.replace('§', '&'));
        }
        return MiniMessage.miniMessage().deserialize(text);
    }
}
