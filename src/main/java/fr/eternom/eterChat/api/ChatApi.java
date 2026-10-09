package fr.eternom.eterChat.api;

import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.UUID;

/**
 * Ce qu'EterChat offre aux autres plugins : les réglages de chat d'un joueur (ignorés, messages privés). Personne
 * d'autre ne lit les tables eterchat_* : on demande ici.
 * <pre>
 *     // compileOnly("com.github.Eternom:EterChat:&lt;tag&gt;") ; plugin.yml : softdepend: [EterChat]
 * </pre>
 */
public interface ChatApi {

    /** L'API d'EterChat si le plugin tourne sur ce serveur. */
    static Optional<ChatApi> get() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(ChatApi.class));
    }

    /** player ignore other (chat et messages privés). Bloquant (base) : hors du thread principal. */
    boolean isIgnoring(UUID player, UUID other);

    /** Le joueur accepte les messages privés (pas de /msgtoggle). Bloquant (base). */
    boolean acceptsPrivateMessages(UUID player);
}
