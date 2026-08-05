# TeamMaker — migrazione da Android a web app React

Documento di riferimento per il porting dell'app Android (`AppAndroid/`, Java +
Firebase Realtime Database) verso una web app React in `Web/`.

L'app Android **resta in produzione** per tutta la durata della migrazione.

---

## 1. Decisioni prese

| Ambito | Scelta | Motivo |
|---|---|---|
| Database in sviluppo | `teammakerStaging/` | Nessun rischio per i dati reali; il root è già previsto (commentato) in `Constants.java` |
| Stack | Vite + React + TypeScript | Build veloce, tipizzazione indispensabile per rispettare lo schema del DB |
| Stato | Zustand | Sostituisce lo stato globale statico `Constants` |
| Styling | Tailwind CSS | I colori di `colors.xml` diventano token nel config; ottimo per il mobile-first |
| Design | Fedele ma ripulito | Stessi colori, tema scuro e struttura delle schermate; spaziature e tipografia adattate al web |
| Test | Vitest | Solo su `domain/`, confrontando i risultati con il Java originale |
| Algoritmo squadre | Web Worker | Su Android gira su N thread fino a 5 minuti: nel browser bloccherebbe la UI |

---

## 2. Il contratto con il database

Le due app condividono lo stesso Realtime Database. La web app **deve** rispettare
lo schema esistente, altrimenti l'app Android si rompe.

```
<root>/                            root = "teammakerStaging/" in sviluppo, "teammaker/" in produzione
  players/<key>/
    name, surname, nickname, gender, is_active (bool)
    stats/ { height, agility, fallacy, pass, reception, attack,
             wall, serve, game-vision, team-play, mentality, bonus }
  tournaments/<key>/
    is_valid  -> STRINGA "true" | "false"
    name, nBracket (int), date -> STRINGA "dd/MM/yyyy"
    teams/<key>/   { bracket: "A", player1, player2, ... playerN }   chiavi numerate
    matches/<key>/ { day, time, team1, team2, points1, points2, type }
                     day / points1 / points2 -> STRINGHE
                     time -> "9:30" (ora NON zero-padded, minuti sì)
                     type -> "GIRONE " | "BRACKET A" | ...
  live_match/
    active (bool), tournament_key, match_position (int),
    team1_name, team2_name, team1_players, team2_players,
    points1, points2, sets1, sets2 (int), timestamp (ms)
```

### Trappole da gestire nei mapper

- **Numeri salvati come stringhe**: `day`, `points1`, `points2`. Vanno convertiti in
  lettura e ri-serializzati in scrittura.
- **`is_valid` è la stringa `"true"`**, non un booleano.
- **Date italiane** `dd/MM/yyyy`, non ISO.
- **I giocatori di una squadra** sono chiavi `player1..playerN`, non un array.
- **Un solo torneo attivo** alla volta (`is_valid === "true"`): attivarne uno
  disattiva il precedente.
- **`time` non è zero-padded sull'ora**: il confronto fra orari come stringhe è
  inaffidabile, va parsato in minuti.

Tutto questo va isolato in `src/data/mappers.ts` e in nessun altro punto del codice.

---

## 3. Architettura di `Web/`

```
Web/
├── index.html · package.json · vite.config.ts · tsconfig.json
├── tailwind.config.ts · .env.example
└── src/
    ├── domain/          logica pura — NON importa React né Firebase
    │   ├── models/          Player, Team, Match, Tournament
    │   ├── constants.ts     stats, statsMax, statsStep, valueHeight
    │   ├── scoring.ts       getVote, generateTable
    │   ├── teamGenerator.ts algoritmo 2 (serpentina) + 5 (ricerca vincolata)
    │   └── scheduler.ts     round robin, gironi, slot orari
    ├── data/            Firebase — NON importa React
    │   ├── firebase.ts · mappers.ts
    │   └── playersRepo · tournamentsRepo · matchesRepo · liveRepo
    ├── store/           Zustand (sostituisce Constants)
    ├── hooks/           usePlayers, useTournament, useLiveMatch
    ├── workers/         teamGenerator.worker.ts
    ├── features/        una cartella per schermata — l'unica con JSX
    │   └── home · generate · teams · tournament · scorecard · live · admin
    ├── components/ui/   Button, Modal, SearchBox, PlayerCard, ListCard
    ├── styles/          token estratti da colors.xml
    └── i18n/            it/en dai 172 string resource
```

**La regola che tiene tutto in piedi**: `domain/` non conosce né React né Firebase.
È testabile e verificabile riga per riga contro il Java originale.

---

## 4. Logica da portare (il cuore)

| Cosa | Origine |
|---|---|
| `getVote()` — somma delle 12 statistiche | `Player.java:62` |
| Algoritmo 2 — serpentina greedy | `TeamGeneratorUtility.java:41` |
| Algoritmo 5 — bilancia voti **e** distribuzione femmine (scarto ≤ 1), 50k tentativi, tolleranza adattiva | `TeamGeneratorUtility.java:55` |
| Round robin circle method con BYE, gironi multipli, interleaving, slot orari su fasce giorno/ora | `TournamentScheduleUtility.java` |
| Classifica: 3 punti se vittoria netta (a 15 o 25), altrimenti 2–1 | `Utility.java:15` |

---

## 5. Fasi

| # | Contenuto | Scritture sul DB |
|---|---|---|
| 0 | Scaffolding, Firebase in sola lettura, design token | no |
| 1 | Tutto `domain/` + test | no |
| 2 | Home + lista giocatori | no |
| 3 | Genera squadre (selezione + worker) + vista Squadre | no |
| 4 | Torneo: classifica, gironi, calendario | no |
| 5 | Segnapunti + Live | sì (staging) |
| 6 | Admin: CRUD giocatori/tornei/partite + auth vera | sì (staging) |
| 7 | PWA, i18n, deploy su Firebase Hosting | produzione |

Fino alla fase 4 la web app è in sola lettura: nessun rischio per i dati né per
l'app Android.

---

## 6. Debiti dell'app Android da non replicare

1. **Stato globale statico** `Constants` mutato da utility, adapter e activity;
   la UI si aggiorna con `notifyDataSetChanged()` sparsi e con
   `activity.recreate()` (`MatchUtility.java:42`).
2. **Riferimenti statici alle Activity** (`ActivityAdmin.activityAdmin`,
   `TournamentActivityManageMatches.tournamentBracketAdminAdapter`): leak e NPE.
3. **Password admin in chiaro** e `logged = true` con `//TODO PRIMA DELLA RELEASE`
   (`Constants.java:22`). Sul web il bundle JS è leggibile: serve Firebase Auth
   con regole sul database, non un confronto lato client.
4. **Logica di business mescolata** con Firebase e con la UI nello stesso file.
5. **Nessun test.**

### Bug noti nel codice Android

- `TournamentUtility.java:103` itera i giocatori di una squadra con
  `getChildrenCount()`, che conta anche il nodo `bracket`: se `bracket` manca si
  perde silenziosamente un giocatore.
- `Match.compareTo` e il comparator inline in `MatchUtility` usano criteri di
  ordinamento diversi (uno ordina l'orario in senso opposto).
- `MatchUtility.addNewMatch` chiama adapter statici senza controllo di null.

Nella versione web vanno corretti, non riprodotti.
