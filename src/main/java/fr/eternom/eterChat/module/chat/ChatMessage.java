package fr.eternom.eterChat.module.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.List;
import java.util.UUID;

/**
 * Un message du chat, tel qu'il circule entre les serveurs (JSON sur Redis).
 * Le texte du joueur (body) et son grade sont déjà mis en forme par le serveur d'origine ;
 * chaque serveur n'a plus qu'à les placer dans le format de la langue de chaque destinataire.
 *
 * @param origin     server-name du serveur d'origine (il ignore son propre message en retour de Redis)
 * @param server     nom affiché du serveur d'origine
 * @param target     destinataire d'un message privé, null sinon
 * @param targetName pseudo du destinataire d'un message privé (tag du clan pour le chat de clan), null sinon
 * @param members    chat de clan : les membres qui le reçoivent, null sinon
 */
public record ChatMessage(Type type, String origin, String server, UUID sender, String senderName,
                          Component prefix, Component suffix, Component body, UUID target, String targetName,
                          List<UUID> members) {

    public enum Type { GLOBAL, STAFF, PRIVATE, CLAN }

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.addProperty("origin", origin);
        json.addProperty("server", server);
        json.addProperty("sender", sender.toString());
        json.addProperty("sender_name", senderName);
        json.add("prefix", GSON.serializeToTree(prefix));
        json.add("suffix", GSON.serializeToTree(suffix));
        json.add("body", GSON.serializeToTree(body));
        if (target != null) {
            json.addProperty("target", target.toString());
            json.addProperty("target_name", targetName);
        } else if (targetName != null) {
            json.addProperty("target_name", targetName);
        }
        if (members != null) {
            JsonArray list = new JsonArray();
            members.forEach(member -> list.add(member.toString()));
            json.add("members", list);
        }
        return json;
    }

    public static ChatMessage fromJson(JsonObject json) {
        boolean hasTarget = json.has("target");
        return new ChatMessage(
                Type.valueOf(json.get("type").getAsString()),
                json.get("origin").getAsString(),
                json.get("server").getAsString(),
                UUID.fromString(json.get("sender").getAsString()),
                json.get("sender_name").getAsString(),
                GSON.deserializeFromTree(json.get("prefix")),
                GSON.deserializeFromTree(json.get("suffix")),
                GSON.deserializeFromTree(json.get("body")),
                hasTarget ? UUID.fromString(json.get("target").getAsString()) : null,
                json.has("target_name") ? json.get("target_name").getAsString() : null,
                json.has("members") ? json.getAsJsonArray("members").asList().stream()
                        .map(element -> UUID.fromString(element.getAsString())).toList() : null);
    }
}
