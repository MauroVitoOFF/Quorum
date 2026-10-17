#!/usr/bin/env python3
"""Genera il modello della pistola d'ordinanza (ispirata a una Glock 17) e il suo rig.

Fonte unica di verita' per geometria e gerarchia dei bone. Produce:
  - models/item/police_pistol.json            modello completo (display + groups per Blockbench)
  - models/item/police_pistol_parts/<bone>.json   un modello per ogni bone con cubi propri
  - rigs/police_pistol.json                   gerarchia, pivot e note per l'animazione

I modelli "parts" usano le stesse coordinate assolute del modello completo: disegnarli tutti
con trasformazione identita' ricostruisce la pistola; per animare si ruota/trasla il singolo
bone attorno al suo pivot (e quello dei figli lo segue, vedi "parent" nel rig).

Scala: 1 unita' di Blockbench ~ 16 mm (Glock 17: 204 mm ~ 12.75 unita'). Densita' texture
costante: 2 pixel per unita'; il lato dell'atlas e' la piu' piccola potenza di 2 che basta.

Uso (dalla radice del repo): python tools/police_pistol_model.py
Poi:                         python tools/police_pistol_texture.py
"""
import json
import sys
from pathlib import Path

ASSETS = Path("qrm-neoforge/src/main/resources/assets/qrm")
PPU = 2          # pixel per unita'
INS = 0.02       # rientranza per evitare facce coincidenti (z-fighting)
GRIP_PITCH = (-15, [8, 8, 10.5])
TRIGGER_PITCH = (15, [8, 7, 8.75])

# (bone, nome, from, to, rotazione X opzionale)
CUBES = [
    # ------------------------------------------------------------ carrello
    ("slide", "slide_lower",      [7, 9, 2.5],       [9, 9.5, 14],      None),
    ("slide", "slide_front",      [7, 9.5, 2.5],     [9, 10.5, 7.5],    None),
    ("slide", "slide_rear",       [7, 9.5, 10.5],    [9, 10.5, 14],     None),
    ("slide", "slide_wall_left",  [7, 9.5, 7.5],     [7.5, 10.5, 10.5], None),
    ("slide", "slide_top",        [7.5, 10.5, 2.5],  [8.5, 11, 14],     None),
    ("slide", "rear_plate",       [7.5, 9.5, 14],    [8.5, 10.5, 14.5], None),
    ("slide", "extractor",        [9, 9.5, 10.5],    [9.5, 10, 11.5],   None),
    ("port_cover", "port_plug",  [7.5, 9.5, 7.5],   [9, 10.5, 10.5],   None),
    ("sights", "front_sight",     [7.75, 11, 3],     [8.25, 11.5, 4],   None),
    ("sights", "rear_sight_l",    [7, 10.5, 13],     [7.5, 11.5, 14],   None),
    ("sights", "rear_sight_r",    [8.5, 10.5, 13],   [9, 11.5, 14],     None),
    # --------------------------------------------------------------- canna
    ("barrel", "barrel_tube",     [7.25, 9.1, 2],    [8.75, 10.4, 7.45], None),
    ("barrel", "barrel_hood",     [7.55, 10, 7.55],  [8.45, 10.45, 10.45], None),
    # ------------------------------------------------- cartuccia in camera
    ("chambered_round", "round_case",   [7.75, 9.6, 8.75], [8.25, 10.1, 10.25], None),
    ("chambered_round", "round_bullet", [7.75, 9.6, 8],    [8.25, 10.1, 8.75],  None),
    ("ejected_casing", "casing_ejected", [7.75, 7.75, 7.75], [8.25, 8.25, 9.25], None),
    # --------------------------------------------------------------- fusto
    ("body", "frame_dustcover",   [6.75, 8, 3],      [9.25, 9, 7],      None),
    ("body", "frame_mid",         [6.75, 7, 7],      [9.25, 9, 9.5],    None),
    ("body", "frame_rear",        [6.75, 8, 9.5],    [9.25, 9, 14],     None),
    # ----------------------------------------------------------- impugnatura
    ("grip", "grip_main",         [6.75 + INS, 3.5, 9.5], [9.25 - INS, 8, 12.5], GRIP_PITCH),
    ("grip", "grip_backstrap",    [7.25, 4.5, 12.5], [8.75, 7.5, 13],   GRIP_PITCH),
    ("grip", "grip_ridge_1",      [7, 4.5, 9],       [9, 5, 9.5],       GRIP_PITCH),
    ("grip", "grip_ridge_2",      [7, 5.5, 9],       [9, 6, 9.5],       GRIP_PITCH),
    ("grip", "grip_ridge_3",      [7, 6.5, 9],       [9, 7, 9.5],       GRIP_PITCH),
    # ------------------------------------------------------------ ponticello
    ("trigger_guard", "guard_post",   [7.25, 5.5, 6.5], [8.75, 8, 7.5],    None),
    ("trigger_guard", "guard_bottom", [7.25, 5, 6.5],   [8.75, 5.5, 10.5], None),
    # ------------------------------------------------------------- grilletto
    ("trigger", "trigger_blade",  [7.75, 5.75, 8.5], [8.25, 7, 9],     TRIGGER_PITCH),
    ("trigger_safety", "trigger_safety", [7.75, 6.25, 8], [8.25, 6.75, 8.5], TRIGGER_PITCH),
    ("trigger_bar", "trigger_bar", [7.75, 6.5, 9],   [8.25, 7, 10],    None),
    # ------------------------------------------------------------ caricatore
    ("magazine", "magazine_body", [6.75 + 3 * INS, 3, 9.5 + 3 * INS], [9.25 - 3 * INS, 8.5, 12.5 - 3 * INS], GRIP_PITCH),
    ("magazine", "magazine_base", [6.75, 2.5, 9.5],  [9.25, 3, 13],     GRIP_PITCH),
]

# bone -> (parent, pivot, descrizione dell'uso previsto)
BONES = {
    "root": (None, [8, 6, 11], "pivot alla mano (centro dell'impugnatura): rinculo, ondeggio, pose del braccio"),
    "body": ("root", [8, 8, 8], "fusto: fisso rispetto alla mano"),
    "grip": ("body", [8, 8, 10.5], "impugnatura"),
    "trigger_guard": ("body", [8, 6, 8], "ponticello"),
    "trigger": ("root", [8, 7, 8.75], "grilletto: ruota su X attorno al perno in alto"),
    "trigger_safety": ("trigger", [8, 6.75, 8.25], "sicura nel grilletto: ruota con il grilletto"),
    "trigger_bar": ("root", [8, 6.75, 9], "barra del grilletto: trasla su Z con il grilletto"),
    "slide": ("root", [8, 10, 8], "carrello: trasla su Z (corsa ~ 2.5 unita')"),
    "port_cover": ("slide", [7.5, 10, 9], "tappo della finestra di espulsione: a riposo il carrello e' pieno; "
                  "scala X a 0 quando il carrello e' arretrato oltre 0.3 unita' e mostra la camera"),
    "sights": ("slide", [8, 11, 8], "mire: seguono il carrello"),
    "barrel": ("root", [8, 10, 3.5], "canna: si inclina attorno al nasello anteriore (culatta in basso) durante il rinculo"),
    "chambered_round": ("root", [8, 9.75, 9], "cartuccia in camera: puo' inclinarsi (mancata alimentazione)"),
    "ejected_casing": ("root", [8, 8, 8.5], "bossolo espulso: a riposo nascosto nel fusto; esce dal lato +X attraverso il carrello (che e' pieno)"),
    "magazine": ("root", [8, 8, 10.5], "caricatore: scorre lungo l'asse dell'impugnatura (-15 deg su X)"),
}
# la cartuccia/bossolo e la barra non hanno cubi di "fusto" o carrello
EXTRA_NOTES = {
    "slide": "carrello pieno a riposo (tappo sulla finestra); arretrando il tappo sparisce e si vede la camera",
}


def px(v):
    return int(round(v * PPU))


def face_rects(frm, to):
    w, h, d = (px(to[i] - frm[i]) for i in range(3))
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}


def main():
    out_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else ASSETS
    display = json.loads((out_dir / "models/item/police_pistol.json").read_text(encoding="utf-8"))["display"] \
        if (out_dir / "models/item/police_pistol.json").exists() else {}

    rects = []
    for i, (_, _, f, t, _) in enumerate(CUBES):
        for face, (fw, fh) in face_rects(f, t).items():
            rects.append((i, face, fw, fh))

    def pack(tex):
        x = y = rowh = 0
        pos = {}
        for k in sorted(range(len(rects)), key=lambda k: (-rects[k][3], -rects[k][2])):
            i, face, fw, fh = rects[k]
            if x + fw > tex:
                x = 0; y += rowh; rowh = 0
            pos[(i, face)] = (x, y, fw, fh)
            x += fw; rowh = max(rowh, fh)
        return pos if y + rowh <= tex else None

    tex = 64
    while (pos := pack(tex)) is None:
        tex *= 2
    print(f"{len(CUBES)} cubi, atlas {tex}x{tex}", file=sys.stderr)

    k = 16 / tex
    elements = []
    for i, (bone, name, f, t, r) in enumerate(CUBES):
        faces = {}
        for face in ("north", "east", "south", "west", "up", "down"):
            a, b, fw, fh = pos[(i, face)]
            faces[face] = {"uv": [a * k, b * k, (a + fw) * k, (b + fh) * k], "texture": "#0"}
        el = {"name": name, "from": f, "to": t}
        if r:
            el["rotation"] = {"angle": r[0], "axis": "x", "origin": r[1]}
        el["faces"] = faces
        elements.append(el)

    index = {name: i for i, (_, name, *_) in enumerate(CUBES)}
    direct = {b: [i for i, c in enumerate(CUBES) if c[0] == b] for b in BONES}
    for c in CUBES:
        assert c[0] in BONES, c[0]

    def group(bone):
        parent, pivot, _ = BONES[bone]
        children = list(direct[bone]) + [group(b) for b, v in BONES.items() if v[0] == bone]
        return {"name": bone, "origin": pivot, "color": 0, "children": children}

    textures = {"0": "qrm:item/police_pistol", "particle": "qrm:item/police_pistol"}
    full = {
        "credit": "Quorum - police pistol (QRM original)",
        "texture_size": [tex, tex],
        "textures": textures,
        "elements": elements,
        "display": display,
        "groups": [group("root")],
    }
    (out_dir / "models/item").mkdir(parents=True, exist_ok=True)
    (out_dir / "models/item/police_pistol.json").write_text(json.dumps(full, indent=2), encoding="utf-8")

    parts_dir = out_dir / "models/item/police_pistol_parts"
    parts_dir.mkdir(parents=True, exist_ok=True)
    for old in parts_dir.glob("*.json"):
        old.unlink()
    for bone, ids in direct.items():
        if not ids:
            continue
        part = {"texture_size": [tex, tex], "textures": textures, "elements": [elements[i] for i in ids]}
        (parts_dir / f"{bone}.json").write_text(json.dumps(part, indent=2), encoding="utf-8")

    rig = {
        "_doc": "Rig della pistola. Ogni bone ha un modello in models/item/police_pistol_parts/<bone>.json "
                "(solo se ha cubi propri). Le coordinate sono assolute (unita' Blockbench, 16 = 1 blocco): "
                "per animare un bone ruota/trasla attorno a 'pivot'; i figli ereditano la trasformazione. "
                "Il muso punta verso -Z, l'alto e' +Y, il bossolo viene espulso dal lato +X; a riposo il carrello e' pieno; con il carrello indietro la finestra si apre sul lato +X.",
        "model": "qrm:item/police_pistol",
        "parts_prefix": "qrm:item/police_pistol_parts/",
        "bones": {
            b: {"parent": p, "pivot": pv, "has_model": bool(direct[b]), "note": note}
            for b, (p, pv, note) in BONES.items()
        },
        "baked_rotations": "grip e magazine sono gia' ruotati di -15 deg su X, il grilletto di +15 deg: "
                           "i cubi lo contengono, non applicarlo di nuovo",
        "magazine_axis": [0, -0.966, 0.259],  # direzione di estrazione (verso il basso lungo l'impugnatura)
        "suggested_travel": {
            "slide.z": [0, 2.5],
            "trigger.rot_x": [0, -15],
            "magazine.along_axis": [0, 6],
        },
    }
    (out_dir / "rigs").mkdir(parents=True, exist_ok=True)
    (out_dir / "rigs/police_pistol.json").write_text(json.dumps(rig, indent=2), encoding="utf-8")
    print("ok")


if __name__ == "__main__":
    main()
