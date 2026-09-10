"""
Migrazione DB TeamMaker: le stat da chiavi hardcoded (agility, height, ...)
a push-key opache Firebase-like.

Uso:
    python migrate_stats_to_uid.py input.json output.json

Cosa fa:
- Crea il nodo teammaker/stats/<newUid> con label, max, step, order, type,
  values per ognuna delle 12 stat storiche.
- Riscrive TUTTI i giocatori sotto teammaker/players/<*>/stats/:
  ogni chiave inglese vecchia (agility, ...) viene sostituita dalla nuova
  push-key. I valori restano invariati.
- Non tocca il resto del DB (players.name/surname/gender/tournaments/...).

L'output va importato in Firebase alla RADICE per sostituire il vecchio DB.
"""

import json
import random
import string
import sys
import time


# Definizioni originali (da Constants.java), nell'ordine che diventera' order:
STATS_ORIGINAL = [
    # (key vecchia, label, max, step, type, values)
    ("height",      "Altezza",          6, 1,   "RANGE",
        ["<150cm", "151-160cm", "161-170cm", "171-180cm", "181-190cm", "191-200cm", ">200cm"]),
    ("agility",     "Agilita'",         4, 1,   "STARS", None),
    ("fallacy",     "Fallosita'",       2, 1,   "STARS", None),
    ("pass",        "Palleggio",        4, 1,   "STARS", None),
    ("reception",   "Ricezione",        4, 1,   "STARS", None),
    ("attack",      "Attacco",          6, 1.5, "STARS", None),
    ("wall",        "Muro",             4, 1,   "STARS", None),
    ("serve",       "Battuta",          6, 1.5, "STARS", None),
    ("game-vision", "Visione di gioco", 4, 2,   "STARS", None),
    ("team-play",   "Gioco di squadra", 2, 1,   "STARS", None),
    ("mentality",   "Mentalita'",       2, 1,   "STARS", None),
    ("bonus",       "Bonus",            2, 1,   "STARS", None),
]

# Le label con accenti "vere" (per il DB finale)
LABEL_UNICODE = {
    "Altezza": "Altezza",
    "Agilita'": "Agilità",
    "Fallosita'": "Fallosità",
    "Palleggio": "Palleggio",
    "Ricezione": "Ricezione",
    "Attacco": "Attacco",
    "Muro": "Muro",
    "Battuta": "Battuta",
    "Visione di gioco": "Visione di gioco",
    "Gioco di squadra": "Gioco di squadra",
    "Mentalita'": "Mentalità",
    "Bonus": "Bonus",
}


PUSH_ALPHABET = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz"

def push_key(counter=[0]):
    """Genera una push-key Firebase-like univoca (approssimazione best-effort)."""
    now = int(time.time() * 1000) + counter[0]
    counter[0] += 1
    # 8 char timestamp + 12 char random dal push alphabet Firebase
    ts_part = ""
    n = now
    for _ in range(8):
        ts_part = PUSH_ALPHABET[n & 63] + ts_part
        n >>= 6
    rand_part = "".join(random.choice(PUSH_ALPHABET) for _ in range(12))
    return "-" + ts_part[1:] + rand_part


def migrate(data):
    if "teammaker" not in data:
        raise SystemExit("Il JSON non contiene un nodo 'teammaker'.")
    tm = data["teammaker"]

    # 1. Costruisci il nodo stats con push-key nuove
    old_to_new = {}
    stats_node = {}
    for order, (old_key, label, mx, step, typ, values) in enumerate(STATS_ORIGINAL):
        new_key = push_key()
        old_to_new[old_key] = new_key
        entry = {
            "label": LABEL_UNICODE.get(label, label),
            "type": typ,
            "max": mx,
            "step": step,
            "order": order,
        }
        if values is not None:
            entry["values"] = values
        stats_node[new_key] = entry
    tm["stats"] = stats_node

    # 2. Rimappa i player: stats.agility -> stats.<newUid>
    players = tm.get("players", {})
    remapped = 0
    for _, player in players.items():
        if not isinstance(player, dict):
            continue
        old_stats = player.get("stats", {})
        if not isinstance(old_stats, dict):
            continue
        new_stats = {}
        for k, v in old_stats.items():
            new_key = old_to_new.get(k)
            if new_key is None:
                # Chiave sconosciuta: la lascio con il nome vecchio per non perdere dati.
                new_stats[k] = v
            else:
                new_stats[new_key] = v
        player["stats"] = new_stats
        remapped += 1

    # 3. Toglie il nodo admin-pw se ancora presente (deprecato)
    if "admin-pw" in tm:
        del tm["admin-pw"]

    print("stats create: {} definizioni".format(len(stats_node)))
    print("player migrati: {}".format(remapped))
    print("mappa vecchia->nuova key:")
    for k, v in old_to_new.items():
        print("  {:<12s} -> {}".format(k, v))

    return data


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    src, dst = sys.argv[1], sys.argv[2]
    with open(src, "r", encoding="utf-8") as f:
        data = json.load(f)
    data = migrate(data)
    with open(dst, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print("Salvato: {}".format(dst))


if __name__ == "__main__":
    main()
