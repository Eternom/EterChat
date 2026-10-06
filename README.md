# EterChat

Le chat du réseau : un canal **global** et un canal **staff** partagés entre tous les serveurs, des **messages privés**
d'un serveur à l'autre, des mentions et `[item]`. Ce plugin ne fait pas de modération (anti-spam, mute…) : ce sera
un plugin à part. Document développeur, à tenir à jour avec le code.

## Prérequis

- **EterLib 1.4.0+** (`depend`). `server-display-name` d'EterLib est le nom de serveur montré dans le chat.
- **Redis facultatif** : avec lui, le chat et les messages privés traversent les serveurs ; sans lui, chaque serveur a
  son propre chat et les messages privés ne vont qu'aux joueurs du même serveur.
- **LuckPerms facultatif** : préfixe et suffixe du grade (codes `&` ou MiniMessage).

## Fonctionnement

`ChatListener` annule l'événement de chat de Paper (priorité `HIGH`, `ignoreCancelled` : un futur plugin de modération
qui l'annule avant garde le dernier mot) et passe le texte à `ChatService` :

1. **Côté expéditeur**, une seule fois : grade LuckPerms et texte mis en forme (`ChatFormatter#body`). Le texte n'est
   interprété en MiniMessage qu'avec `eterchat.color`, et seulement les couleurs et les styles (pas de clic ni de survol).
   `[item]` devient l'objet en main avec son infobulle, `@Pseudo` est surligné.
2. **Distribution sur ce serveur d'abord**, puis publication en JSON (`ChatMessage`) sur le canal Redis `eterchat`
   (`RedisMessenger` d'EterLib). Les autres serveurs le distribuent à leurs joueurs ; l'origine ignore le sien en retour.
   **Redis en panne = le chat continue sur chaque serveur** : un avertissement par minute au plus dans la console,
   et l'expéditeur d'un message privé vers un autre serveur est prévenu qu'il n'est pas arrivé.
3. **Côté destinataire**, la ligne est composée dans **sa** langue (`lang/` > `chat.global`, `chat.staff`, `private.*`).
   Le pseudo porte une fiche au survol (serveur) et un clic prépare `/msg`.

Messages privés : le destinataire est cherché sur ce serveur, sinon dans `eter_players` (présence réseau). Le serveur
du destinataire lui affiche le message et retient l'expéditeur pour `/r` (copié dans Redis, `chat:reply:<uuid>`,
1 h, pour suivre le joueur d'un serveur à l'autre). Les espions (`/socialspy`) de chaque serveur le voient aussi ;
la console du serveur d'origine le journalise.

## Commandes et permissions

| Commande | Rôle | Permission |
|---|---|---|
| `/msg <joueur> <message>` (`tell`, `w`, `m`, `whisper`, `pm`) | Message privé, n'importe quel serveur | — |
| `/r <message>` | Répondre au dernier correspondant | — |
| `/ignore [joueur]` | Ignorer / ne plus ignorer (chat global et privés) ; seul : la liste | — |
| `/notifications` (`notifs`) | Couper ou remettre le son et l'alerte des mentions et messages privés | — |
| `/staffchat [message]` (`sc`) | Un message au staff ; seul : tout son chat part au staff | `eterchat.staff` |
| `/socialspy` (`spy`) | Voir les messages privés de tout le réseau | `eterchat.socialspy` |

Autres : `eterchat.color` (couleurs dans ses messages, personne par défaut), `eterchat.item` (`[item]`, tout le monde).
`eterchat.admin` regroupe tout (op par défaut).

## Données

- `eterchat_players` : `uuid`, `staff_channel`, `notifications`, `social_spy` (lus à l'arrivée, gardés en mémoire).
- `eterchat_ignores` : `owner`, `target`, `target_name`. Le canal staff ne s'ignore pas.
