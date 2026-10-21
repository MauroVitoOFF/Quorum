#!/usr/bin/env python3
"""Esportatore della pistola d'ordinanza: dal progetto Blockbench agli asset del gioco.

Legge design/police-pistol/police_pistol.bbmodel (modello "free") e scrive in
qrm-police/src/main/resources/assets/qrm_police/:

  models/item/police_pistol.json              modello completo (tutti i cubi)
  models/item/police_pistol_parts/<bone>.json un modello per ogni gruppo con cubi propri
  rigs/police_pistol.json                     gerarchia dei bone (parent, pivot, has_model, note)
  textures/item/police_pistol.png             texture incorporata (base64) nel .bbmodel
  animations/police_pistol.json               animazioni ed eventi (formato qrm.anim/2)

Animazioni: ogni clip del .bbmodel diventa {length, loop, [hold_last], bones, events}. I keyframe
passano con interpolazione linear | step | catmullrom (bezier e' un errore, con il nome della clip).
Blockbench anima con X della posizione e X/Y della rotazione invertiti rispetto al gioco: qui si
rimettono a posto. Gli eventi stanno nell'animatore effetti ("type": "effect"), canale "timeline":
ogni keyframe porta uno script, una istruzione per riga (sound <id>, commit <fase>, muzzle_flash,
eject_shell, camera_kick <pitch> <yaw>, arm <posa>); vedi la costante INSTRUCTIONS.

Il .bbmodel non porta il blocco "display" ne' le note del rig: arrivano da due file
accanto al progetto, design/police-pistol/display.json e design/police-pistol/rig_notes.json.

Uso (dalla radice del repo): python tools/pistol_export.py [progetto.bbmodel] [cartella_asset]
"""
import base64
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DESIGN = ROOT / "design/police-pistol"
DEFAULT_INPUT = DESIGN / "police_pistol.bbmodel"
DEFAULT_OUT = ROOT / "qrm-police/src/main/resources/assets/qrm_police"
NAMESPACE = "qrm_police"
MAX_ANGLE = 45.0
# i modelli di riferimento usano 15 deg (grip, caricatore, grilletto): il passo ammesso e' 7,5, non 22,5
STEP = 7.5
ANIM_FORMAT = "qrm.anim/2"
INTERPOLATIONS = ("linear", "step", "catmullrom")
CHANNELS = ("position", "rotation", "scale")
# Blockbench inverte X della posizione e X/Y della rotazione rispetto al gioco (verificato sulle posizioni mondo)
SIGN = {"position": [-1, 1, 1], "rotation": [-1, -1, 1], "scale": [1, 1, 1]}
INSTRUCTIONS = ("sound", "commit", "muzzle_flash", "eject_shell", "camera_kick", "arm")


def num(v):
    """Numero senza rumore in virgola mobile; interi come interi."""
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def vec(v):
    return [num(c) for c in v]


def rotation_of(el):
    """Rotazione nel formato dei modelli Java (un solo asse, multipli di 7,5 deg, max 45)."""
    r = el.get("rotation")
    if not r or not any(r):
        return None
    axes = [(a, v) for a, v in zip("xyz", r) if v]
    name = el.get("name", "?")
    if len(axes) != 1:
        raise ValueError(f"{name}: rotazione su piu' assi {r}, i modelli Java ne ammettono uno solo")
    axis, angle = axes[0]
    if abs(angle) > MAX_ANGLE or (angle / STEP) != int(angle / STEP):
        raise ValueError(f"{name}: angolo {angle} non ammesso (multipli di {STEP} fino a {MAX_ANGLE})")
    return {"angle": num(angle), "axis": axis, "origin": vec(el.get("origin", [0, 0, 0]))}


def cube_json(el, tex_w, tex_h):
    out = {"name": el["name"], "from": vec(el["from"]), "to": vec(el["to"])}
    rot = rotation_of(el)
    if rot:
        out["rotation"] = rot
    faces = {}
    for fname, f in el["faces"].items():
        if f.get("texture") is None:
            continue
        u0, v0, u1, v1 = f["uv"]
        # UV Blockbench in pixel -> 0..16 dei modelli Java
        faces[fname] = {"uv": [num(u0 * 16 / tex_w), num(v0 * 16 / tex_h),
                               num(u1 * 16 / tex_w), num(v1 * 16 / tex_h)], "texture": "#0"}
    out["faces"] = faces
    return out


def walk(nodes, parent, groups, by_uuid, own):
    """Visita l'outliner: riempie groups[nome] = (parent, origin) e own[nome] = cubi diretti."""
    for n in nodes:
        if isinstance(n, str):
            own[parent].append(by_uuid[n])
            continue
        name = n["name"]
        groups[name] = (parent, n["origin"])
        own[name] = []
        walk(n["children"], name, groups, by_uuid, own)


def clip_bones(clip):
    """Canali dei bone di una clip: {bone: {canale: [keyframe qrm.anim/2]}}."""
    name = clip["name"]
    bones = {}
    for animator in clip.get("animators", {}).values():
        if animator.get("type", "bone") != "bone":
            continue
        chans = {}
        for kf in sorted(animator.get("keyframes", []), key=lambda k: float(k["time"])):
            ch = kf["channel"]
            if ch not in SIGN:
                raise ValueError(f"{name}/{animator['name']}: canale sconosciuto '{ch}'")
            interp = kf.get("interpolation", "linear")
            if interp not in INTERPOLATIONS:
                raise ValueError(f"{name}/{animator['name']}: interpolazione '{interp}' non ammessa "
                                 f"(solo {', '.join(INTERPOLATIONS)})")
            dp = kf["data_points"][0]
            try:
                v = [float(dp[a]) * sg for a, sg in zip("xyz", SIGN[ch])]
            except (KeyError, TypeError, ValueError):
                raise ValueError(f"{name}/{animator['name']}: valore non numerico a t={kf['time']} {dp}")
            chans.setdefault(ch, []).append({"t": num(kf["time"]), "v": vec(v), "interp": interp})
        if chans:
            bones[animator["name"]] = {ch: chans[ch] for ch in CHANNELS if ch in chans}
    return bones


def clip_events(clip):
    """Eventi dall'animatore effetti: una istruzione per riga, ordinati per tempo (stabile)."""
    name = clip["name"]
    events = []
    for animator in clip.get("animators", {}).values():
        if animator.get("type") != "effect":
            continue
        for kf in animator.get("keyframes", []):
            if kf.get("channel") != "timeline":
                continue
            t = round(float(kf["time"]), 3)
            for dp in kf.get("data_points", []):
                for line in str(dp.get("script", "")).splitlines():
                    words = line.split()
                    if not words:
                        continue
                    if words[0] not in INSTRUCTIONS:
                        raise ValueError(f"{name}: istruzione sconosciuta '{words[0]}' a t={t}")
                    events.append({"t": num(t), "type": words[0], "args": words[1:]})
    events.sort(key=lambda e: e["t"])
    return events


def export_animations(bb):
    clips = {}
    for clip in bb.get("animations", []):
        mode = clip.get("loop", "once")
        out = {"length": num(clip["length"]), "loop": mode == "loop", "hold_last": mode == "hold"}
        out["bones"] = clip_bones(clip)
        out["events"] = clip_events(clip)
        clips[clip["name"]] = out
    return {"format": ANIM_FORMAT, "animations": clips}


def export(bbmodel, out_dir, namespace=NAMESPACE):
    bbmodel, out_dir = Path(bbmodel), Path(out_dir)
    bb = json.loads(bbmodel.read_text(encoding="utf-8"))
    tex_w, tex_h = bb["resolution"]["width"], bb["resolution"]["height"]
    by_uuid = {e["uuid"]: e for e in bb["elements"]}
    groups, own = {}, {None: []}
    walk(bb["outliner"], None, groups, by_uuid, own)
    if own[None]:
        raise ValueError("cubi fuori da ogni gruppo: " + ", ".join(e["name"] for e in own[None]))

    side = DESIGN / "display.json"
    display = json.loads(side.read_text(encoding="utf-8"))
    notes = json.loads((DESIGN / "rig_notes.json").read_text(encoding="utf-8"))

    tex_ref = f"{namespace}:item/police_pistol"
    head = {"credit": display["credit"], "texture_size": [tex_w, tex_h],
            "textures": {"0": tex_ref, "particle": tex_ref}}

    # tutti i cubi, nell'ordine dell'outliner (gruppi in profondita')
    ordered = []

    def collect(name):
        ordered.extend(own[name])
        for g, (p, _) in groups.items():
            if p == name:
                collect(g)
    for g, (p, _) in groups.items():
        if p is None:
            collect(g)

    models = out_dir / "models/item"
    (models / "police_pistol_parts").mkdir(parents=True, exist_ok=True)
    full = dict(head, elements=[cube_json(e, tex_w, tex_h) for e in ordered], display=display["display"])
    (models / "police_pistol.json").write_text(json.dumps(full, indent=2) + "\n", encoding="utf-8")

    bones = {}
    for name, (parent, origin) in groups.items():
        has = bool(own[name])
        bones[name] = {"parent": parent, "pivot": vec(origin), "has_model": has,
                       "note": notes["notes"].get(name, "")}
        part = models / "police_pistol_parts" / f"{name}.json"
        if has:
            body = dict(head, elements=[cube_json(e, tex_w, tex_h) for e in own[name]])
            part.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")
        elif part.exists():
            part.unlink()

    rig = {"_doc": notes["_doc"], "model": tex_ref,
           "parts_prefix": f"{namespace}:item/police_pistol_parts/"}
    for key in ("baked_rotations", "magazine_axis", "suggested_travel"):
        rig[key] = notes[key]
    rig["bones"] = bones
    (out_dir / "rigs").mkdir(parents=True, exist_ok=True)
    (out_dir / "rigs/police_pistol.json").write_text(json.dumps(rig, indent=2) + "\n", encoding="utf-8")

    (out_dir / "animations").mkdir(parents=True, exist_ok=True)
    (out_dir / "animations/police_pistol.json").write_text(
        json.dumps(export_animations(bb), indent=1) + "\n", encoding="utf-8")

    src = bb["textures"][0]["source"]
    (out_dir / "textures/item").mkdir(parents=True, exist_ok=True)
    (out_dir / "textures/item/police_pistol.png").write_bytes(base64.b64decode(src.split(",", 1)[1]))
    return len(ordered), len(bones)


def main(argv):
    src = Path(argv[1]) if len(argv) > 1 else DEFAULT_INPUT
    out = Path(argv[2]) if len(argv) > 2 else DEFAULT_OUT
    cubes, bones = export(src, out)
    print(f"esportati {cubes} cubi, {bones} bone in {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
