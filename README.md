# EterChat

Le chat du réseau : un canal **global** et un canal **staff** partagés entre tous les serveurs, des **messages privés**
d'un serveur à l'autre, des mentions et `[item]`. Ce plugin ne fait pas de modération (anti-spam, mute…) : ce sera
un plugin à part. Document développeur, à tenir à jour avec le code.

## Prérequis

- **EterLib 1.9.1+** (`depend`, textes communs, cadre des menus, bus réseau, `Money`). `server-display-name` d'EterLib est le nom de serveur montré dans le chat ; la complétion des pseudos avec Tab vient d'EterLib (`OnlineNames`).
- **Redis** (obligatoire, via EterLib) : le chat et les messages privés traversent les serveurs.
- **LuckPerms facultatif** : préfixe et suffixe du grade (codes `&` ou MiniMessage). Un **badge** (étiquette EterLib
  `badge`, posée par EterClan : le tag du clan) remplace le préfixe du grade.

## Fonctionnement

`ChatListener` annule l'événement de chat de Paper (priorité `HIGH`, `ignoreCancelled` : un futur plugin de modération
qui l'annule avant garde le dernier mot) et passe le texte à `ChatService` :

1. **Côté expéditeur**, une seule fois : grade LuckPerms et texte mis en forme (`ChatFormatter#body`). Le texte n'est
   interprété en MiniMessage qu'avec `eterchat.color`, et seulement les couleurs et les styles (pas de clic ni de survol).
   `[item]` devient l'objet en main avec son infobulle, `@Pseudo` est surligné.
2. **Distribution sur ce serveur d'abord**, puis publication en JSON (`ChatMessage`) sur le canal Redis `eterchat`
   (bus réseau d'EterLib, type `chat`). Les autres serveurs le distribuent à leurs joueurs ; l'origine ignore le sien.
   **Redis en panne = le chat continue sur chaque serveur** : un avertissement par minute au plus dans la console,
   et l'expéditeur d'un message privé vers un autre serveur est prévenu qu'il n'est pas arrivé.
3. **Côté destinataire**, la ligne est composée dans **sa** langue (`lang/` > `chat.global`, `chat.staff`, `private.*`).
   Le pseudo porte une fiche au survol (serveur) et un clic prépare `/msg`.

Messages privés : le destinataire est cherché sur ce serveur, sinon dans `eter_players` (présence réseau). Un
invisible (vanish du staff, EterLib) est « hors ligne » pour qui ne peut pas le voir, sauf pour lui répondre (`/r`). Le serveur
du destinataire lui affiche le message et retient l'expéditeur pour `/r` (copié dans Redis, `chat:reply:<uuid>`,
1 h, pour suivre le joueur d'un serveur à l'autre). Les espions (`/socialspy`) de chaque serveur le voient aussi ;
la console du serveur d'origine le journalise.

**Prison** (EterModeration) : les serveurs dont le nom commence par `prison.server-prefix` (`prison`, le même préfixe
qu'EterVelocityModeration) ont **leur propre chat global** : un prisonnier ne parle qu'aux prisonniers, et le reste du
réseau ne l'entend pas. Le staff avec `eterchat.prisonchat.see` voit les deux, la prison avec l'étiquette `[Prison]`
(`chat.global-prison`). Messages privés : un prisonnier n'écrit qu'aux prisonniers (`private.prison-blocked`), sauf
pour **répondre** (`/r`) au staff qui lui a écrit ; le staff (`eterchat.prisonchat.see`) écrit à tout le monde.

## Commandes et permissions

| Commande | Rôle | Permission |
|---|---|---|
| `/chat` (`chatsettings`) | Menu des réglages (voir plus bas) | — |
| `/msg <joueur> <message>` (`tell`, `w`, `m`, `whisper`, `pm`) | Message privé, n'importe quel serveur | — |
| `/r <message>` | Répondre au dernier correspondant | — |
| `/msgtoggle` | Refuser / accepter de nouveau les messages privés | — |
| `/ignore [joueur]` | Ignorer / ne plus ignorer (chat global et privés) ; seul : la liste | — |
| `/notifications` (`notifs`) | Couper ou remettre le son et l'alerte des mentions et messages privés | — |
| `/staffchat [message]` (`sc`) | Un message au staff ; seul : tout son chat part au staff | `eterchat.staff` |
| `/socialspy` (`spy`) | Voir les messages privés de tout le réseau | `eterchat.socialspy` |

Autres : `eterchat.color` (couleurs dans ses messages, personne par défaut), `eterchat.item` (`[item]`, tout le monde),
`eterchat.bypass.msgtoggle` (écrire à un joueur qui refuse les messages privés : le staff doit pouvoir joindre
n'importe qui). `eterchat.prisonchat.see` (chat de la prison et messages privés avec les prisonniers, staff). `eterchat.admin` regroupe tout (op par défaut).

Messages privés refusés : vérifié **côté expéditeur** (réglage lu en mémoire si le destinataire est ici, en base sinon),
pour lui répondre « X n'accepte pas les messages privés » au lieu d'un envoi dans le vide. Le refus d'un joueur
**ignoré**, lui, reste silencieux.

## Menu /chat

`module/gui` : `ChatMenu` (5 lignes, cadre orange/gris, tête du joueur avec le résumé de ses réglages) : messages privés,
notifications, joueurs ignorés, et selon les permissions canal staff et espion ; un clic bascule le réglage
(`PreferenceActions`, partagé avec les commandes). `IgnoredMenu` : têtes des joueurs ignorés, 28 par page ; un clic
ouvre une confirmation (Dialog) pour ne plus l'ignorer ; « Ignorer un joueur » ouvre un Dialog de saisie du pseudo
(n'importe quel joueur déjà venu sur le réseau, même hors ligne).

## Données

- `eterchat_players` : `uuid`, `staff_channel`, `notifications`, `social_spy`, `private_messages` (lus à l'arrivée,
  gardés en mémoire). `private_messages` ajoutée en 1.1.0 : `NULL` = acceptés.
- `eterchat_ignores` : `owner`, `target`, `target_name`. Le canal staff ne s'ignore pas.
