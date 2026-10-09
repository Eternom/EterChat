package fr.eternom.eterChat.module.preference;

import fr.eternom.eterChat.api.ChatApi;

import java.util.UUID;

/** L'API d'EterChat (ChatApi) : le plugin lui-même, vu de l'extérieur. */
public class ChatApiService implements ChatApi {

    private final ChatPreferences preferences;

    public ChatApiService(ChatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public boolean isIgnoring(UUID player, UUID other) {
        return preferences.readIgnoring(player, other);
    }

    @Override
    public boolean acceptsPrivateMessages(UUID player) {
        return preferences.read(player).privateMessages();
    }
}
