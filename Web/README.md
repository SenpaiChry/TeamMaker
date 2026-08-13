# TeamMaker — web app

Porting React dell'app Android in `../AppAndroid`. Il piano completo sta in
[`../MIGRAZIONE.md`](../MIGRAZIONE.md).

## Avvio

```bash
npm install
```

Poi copia `.env.example` in `.env.local` e compila `VITE_FIREBASE_API_KEY` e
`VITE_FIREBASE_APP_ID`. Li ottieni registrando una **app web** nella console
Firebase del progetto `team-maker-d811f` (Impostazioni progetto → Le tue app →
Aggiungi app → Web): la chiave dell'app Android non vale per il web.

```bash
npm run dev
```

## Comandi

| Comando | Cosa fa |
|---|---|
| `npm run dev` | Server di sviluppo |
| `npm test` | Test del dominio |
| `npm run test:watch` | Test in watch mode |
| `npm run typecheck` | Controllo dei tipi |
| `npm run build` | Build di produzione |

## Struttura

```
src/
├── domain/     logica pura — non importa React né Firebase
├── data/       Firebase e mapper — non importa React
├── hooks/      ponte fra data e React
├── features/   una cartella per schermata (l'unica con JSX)
└── styles/     design token estratti da colors.xml
```

**La regola:** `domain/` non conosce né React né Firebase. È l'unica parte
verificabile riga per riga contro il Java originale, ed è coperta dai test.

## Database

In sviluppo si scrive su `teammakerStaging/`, impostato in `.env.local`. Se la
radice è quella di produzione (`teammaker/`, condivisa con l'app Android in uso)
compare una fascia gialla di avviso in cima alla pagina.

⚠️ Lo schema del database è deciso dall'app Android e va rispettato alla lettera:
numeri salvati come stringhe, `is_valid` come stringa `"true"`, date
`dd/MM/yyyy`, giocatori come chiavi `player1..playerN`. Tutte queste conversioni
vivono in `src/data/mappers.ts` e da nessun'altra parte.

## Stato della migrazione

- ✅ **Fase 0** — scaffolding, Firebase in sola lettura, design token
- ✅ **Fase 1** — dominio completo (voto, generatore squadre, calendario, classifica) con i test
- ✅ **Fase 2** — elenco giocatori con ricerca e scheda statistiche
- ✅ **Fase 3** — selezione, generazione squadre su Web Worker, rigenerazione
- ✅ **Fase 4** — torneo: squadre, calendario e classifica (sola lettura)
- ✅ **Fase 5** — segnapunti, diretta e salvataggio del risultato
- ✅ **Fase 6** — gestione giocatori, tornei e calendario
- ✅ **Fase 7** — PWA e pubblicazione su Firebase Hosting

Le traduzioni previste inizialmente sono state saltate: l'app è tutta in
italiano e non c'è un pubblico che ne abbia bisogno.

## Pubblicazione

L'app va su Firebase Hosting, nel progetto `team-maker-d811f`. La versione
pubblicata usa `.env.production`, che punta allo **staging**: per passare ai
dati reali si cambia `VITE_DB_ROOT` in `teammaker/` e si ripubblica.

La prima volta serve autenticarsi (apre il browser sul tuo account Google):

```bash
npx firebase-tools login
```

Poi, da questa cartella:

```bash
npm run deploy
```

L'indirizzo sarà `https://team-maker-d811f.web.app`.

### App installabile

È una PWA: dal telefono, "Aggiungi alla schermata Home" installa l'icona e
l'app si apre a schermo intero. Il guscio dell'app resta in cache, così si apre
anche con la rete incerta; i dati invece non vengono mai messi in cache — un
punteggio vecchio servito dal service worker sarebbe peggio di nessun dato.

## Sicurezza

Lo sbarramento dell'area di gestione è una **comodità, non una protezione**: la
password vive sul Realtime Database al nodo `admin-pw` e viaggia in chiaro nel
traffico di rete. Il vantaggio rispetto a tenerla nel bundle è che si cambia
dalla console Firebase senza ripubblicare il sito. Vale lo stesso per l'app
Android, dove è scritta in chiaro in `Constants.java`.

L'unica protezione reale del database sono le sue **regole**, da configurare
nella console Firebase. Un punto di partenza è in
[`../database.rules.json`](../database.rules.json), con l'avvertenza che
attivarle oggi bloccherebbe anche le scritture dell'app Android, che scrive
senza autenticarsi.
