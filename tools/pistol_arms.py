#!/usr/bin/env python3
"""Aggiunge al progetto Blockbench della pistola le due braccia e le anima in ogni clip.

Le braccia sono i bone arm_right e arm_left, figli di root, con un cubo 4x12x4 ciascuno (le
proporzioni del braccio del giocatore). Il cubo serve solo all'anteprima in Blockbench: in gioco il
braccio e' quello vero del giocatore (skin, divisa di Wardrobe), appeso al perno del bone (vedi
PistolFirstPerson). Convenzione del gioco: il perno e' la spalla, il braccio pende lungo -Y dal perno;
il destro occupa x da -1 a 3 rispetto al perno, il sinistro da -3 a 1; il pugno e' in fondo.

Animazione: nessun keyframe a mano. Per ogni clip si valuta la pistola (esportata in formato
qrm.anim/2) a 20 fotogrammi al secondo e si mette il pugno su un punto di una parte dell'arma:
  - mano destra: sempre sull'impugnatura (grip);
  - mano sinistra: di solito accanto all'impugnatura (presa a due mani); nelle ricariche segue il
    fondo del caricatore, quando si manovra il carrello ne segue il retro, poi torna alla presa.
Il braccio e' rigido (non ha gomito): il pugno sta sul punto scelto e il braccio punta da una spalla
fissa rispetto alla camera, fuori dallo schermo. Cosi' le mani restano attaccate all'arma in ogni
fotogramma, anche durante rinculo, estrazione, ispezione e ricariche.

Lo script e' idempotente: bone, cubi, texture d'anteprima e keyframe delle braccia vengono
sostituiti, mai duplicati (uuid deterministici). Dopo averlo eseguito: python tools/pistol_export.py

Uso (dalla radice del repo): python tools/pistol_arms.py [progetto.bbmodel]
"""
import base64
import io
import json
import math
import sys
import uuid
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import pistol_export  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_BBMODEL = ROOT / "design/police-pistol/police_pistol.bbmodel"
NS = uuid.UUID("0b8f3a5e-7c21-4d6a-9e44-2f1b6c8d7a90")  # spazio fisso per uuid ripetibili

FPS = 20
ARMS = ("arm_right", "arm_left")
ARM_TEXTURE = "arm_preview.png"
# perno di riposo (spalla) dei bone: solo riferimento, il movimento arriva dai keyframe
REST_SHOULDER = {"arm_right": [12.0, -4.0, 22.0], "arm_left": [-2.0, -4.0, 20.0]}
# centro del pugno nel sistema del braccio (rispetto al perno): asse del braccio a x=+-1, pugno a y=-8
FIST = {"arm_right": np.array([1.0, -8.0, 0.0]), "arm_left": np.array([-1.0, -8.0, 0.0])}
# punti di presa: (bone, punto del pugno nello spazio del modello a riposo, spalla fissa da cui arriva
# il braccio). La camera e' circa a (-7, 12.7, 28.7) e guarda verso -Z: le spalle stanno sotto il bordo
# inferiore dello schermo, cosi' le braccia salgono dal basso e la loro estremita' non si vede.
ANCHORS = {
    "grip": ("grip", np.array([8.0, 5.6, 11.6]), np.array([12.0, -20.0, 16.0])),
    "support": ("grip", np.array([5.9, 5.2, 11.0]), np.array([-4.0, -20.0, 14.0])),
    "mag": ("magazine", np.array([8.0, 1.2, 12.4]), np.array([-2.0, -22.0, 16.0])),
    # presa a pinza sul lato sinistro della parte anteriore del carrello: piu' lontana dalla camera del
    # retro, cosi' il braccio non copre l'arma; sale dal basso senza attraversarla
    "slide": ("slide", np.array([5.8, 10.4, 7.0]), np.array([0.0, -22.0, 4.0])),
}
# mano sinistra: (inizio, fine) della transizione verso il punto; "freeze" blocca la mano dov'e'
LEFT_SCHEDULE = {
    "reload_tactical": [(0.30, 0.42, "mag"), (1.85, 2.15, "support")],
    "reload_empty": [(0.30, 0.42, "mag"), (1.70, 1.83, "slide"), (2.03, 2.03, "freeze"),
                     (2.15, 2.45, "support")],
    "inspect": [(1.80, 2.00, "mag"), (2.70, 2.90, "slide"), (3.55, 3.55, "freeze"), (3.60, 3.95, "support")],
    "ammo_check": [(0.00, 0.20, "mag"), (0.80, 1.00, "support")],
    "slide_release": [(0.00, 0.08, "slide"), (0.15, 0.15, "freeze"), (0.22, 0.40, "support")],
    "slide_rack": [(0.00, 0.06, "slide"), (0.24, 0.24, "freeze"), (0.30, 0.50, "support")],
}
HINT_X = np.array([1.0, 0.0, 0.0])  # il lato esterno del braccio guarda verso destra


def uid(*parts):
    return str(uuid.uuid5(NS, "/".join(parts)))


# --------------------------------------------------------------- valutazione qrm.anim/2

def channel_value(keys, t, rest):
    """Valore di un canale al tempo t (semantica di AnimEvaluator: step, catmullrom, linear)."""
    if not keys:
        return np.array(rest, float)
    if t < keys[0]["t"]:
        return np.array(rest, float)
    if t >= keys[-1]["t"]:
        return np.array(keys[-1]["v"], float)
    for i in range(len(keys) - 1):
        a, b = keys[i], keys[i + 1]
        if a["t"] <= t < b["t"]:
            break
    span = b["t"] - a["t"]
    x = 0.0 if span <= 0 else (t - a["t"]) / span
    va, vb = np.array(a["v"], float), np.array(b["v"], float)
    if a["interp"] == "step":
        return va
    if a["interp"] == "catmullrom" or b["interp"] == "catmullrom":
        p0 = np.array(keys[i - 1]["v"], float) if i > 0 else va
        p3 = np.array(keys[i + 2]["v"], float) if i + 2 < len(keys) else vb
        return 0.5 * (2 * va + (-p0 + vb) * x + (2 * p0 - 5 * va + 4 * vb - p3) * x * x
                      + (-p0 + 3 * va - 3 * vb + p3) * x ** 3)
    return va + (vb - va) * x


def translate(v):
    m = np.eye(4)
    m[:3, 3] = v
    return m


def rot_xyz(deg):
    x, y, z = np.radians(deg)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    m = np.eye(4)
    m[:3, :3] = rz @ ry @ rx
    return m


def world_matrices(rig, clip, t):
    """Matrici mondo dei bone (unita' Blockbench): T(pos) T(pivot) Rz Ry Rx S T(-pivot), figlio = padre . locale."""
    out = {}
    for name, bone in rig.items():
        ch = clip["bones"].get(name, {})
        pos = channel_value(ch.get("position"), t, [0, 0, 0])
        rot = channel_value(ch.get("rotation"), t, [0, 0, 0])
        scl = channel_value(ch.get("scale"), t, [1, 1, 1])
        piv = np.array(bone["pivot"], float)
        local = translate(pos) @ translate(piv) @ rot_xyz(rot) @ np.diag([*scl, 1.0]) @ translate(-piv)
        parent = bone["parent"]
        out[name] = (out[parent] @ local) if parent else local
    return out


def point(m, p):
    return (m @ np.append(p, 1.0))[:3]


# --------------------------------------------------------------- braccio rigido

def arm_frame(arm, hand, shoulder):
    """Telaio della spalla (4x4) con il pugno in `hand` e il braccio puntato dalla spalla `shoulder`."""
    d = hand - shoulder
    d /= np.linalg.norm(d)
    y = -d  # +Y del braccio va dal pugno verso la spalla
    x = HINT_X - np.dot(HINT_X, y) * y
    x /= np.linalg.norm(x)
    z = np.cross(x, y)
    r = np.column_stack([x, y, z])
    f = np.eye(4)
    f[:3, :3] = r
    f[:3, 3] = hand - r @ FIST[arm]
    return f


def euler_zyx(r):
    """Angoli in gradi tali che r = Rz Ry Rx."""
    y = math.asin(max(-1.0, min(1.0, -r[2, 0])))
    x = math.atan2(r[2, 1], r[2, 2])
    z = math.atan2(r[1, 0], r[0, 0])
    return np.degrees([x, y, z])


def unwrap(prev, cur):
    if prev is None:
        return cur
    return prev + (cur - prev + 180.0) % 360.0 - 180.0


def smooth(w):
    w = max(0.0, min(1.0, w))
    return w * w * (3 - 2 * w)


def hand_at(anchor, mats):
    """(pugno, spalla) per un punto di presa con le matrici mondo date."""
    bone, p, shoulder = ANCHORS[anchor]
    return point(mats[bone], p), shoulder


def left_hand(name, t, mats):
    """(pugno, spalla) del braccio sinistro al tempo t secondo LEFT_SCHEDULE (default: presa a due mani).

    Nelle transizioni pugno e spalla scorrono insieme dal punto di partenza a quello d'arrivo; "freeze"
    lascia il pugno fermo dove si trova (il carrello scatta via dalla mano)."""
    hand, shoulder = hand_at("support", mats)
    frozen = None
    for start, end, target in LEFT_SCHEDULE.get(name, []):
        if t < start:
            break
        if target == "freeze":
            frozen = hand.copy()
            continue
        dest, dest_shoulder = hand_at(target, mats)
        w = 1.0 if end <= start else smooth((t - start) / (end - start))
        src = frozen if frozen is not None else hand
        hand = src + (dest - src) * w
        shoulder = shoulder + (dest_shoulder - shoulder) * w
        if w >= 1.0:
            frozen = None
    return hand, shoulder


def bake(name, clip, rig):
    """Keyframe (nella convenzione del gioco) di posizione e rotazione dei due bone delle braccia."""
    length = float(clip["length"])
    times = [round(i / FPS, 4) for i in range(int(math.floor(length * FPS + 1e-6)) + 1)]
    if times[-1] < length - 1e-6:
        times.append(round(length, 4))
    keys = {arm: {"position": [], "rotation": []} for arm in ARMS}
    prev = {arm: None for arm in ARMS}
    for t in times:
        mats = world_matrices(rig, clip, t)
        inv_root = np.linalg.inv(mats["root"])
        hands = {"arm_right": hand_at("grip", mats), "arm_left": left_hand(name, t, mats)}
        for arm in ARMS:
            local = inv_root @ arm_frame(arm, *hands[arm])
            rot = unwrap(prev[arm], euler_zyx(local[:3, :3]))
            prev[arm] = rot
            pos = local[:3, 3] - np.array(REST_SHOULDER[arm])
            keys[arm]["position"].append((t, pos))
            keys[arm]["rotation"].append((t, rot))
    return keys


# --------------------------------------------------------------- scrittura nel .bbmodel

def arm_texture_source():
    """Texture d'anteprima 16x16: pelle con polsino della divisa."""
    img = Image.new("RGBA", (16, 16), (196, 140, 104, 255))
    for yy in range(0, 5):
        for xx in range(16):
            img.putpixel((xx, yy), (40, 52, 84, 255))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def ensure_texture(bb):
    texs = bb["textures"]
    base = dict(texs[0])
    base.update({"name": ARM_TEXTURE, "id": "1", "width": 16, "height": 16, "uv_width": 16, "uv_height": 16,
                 "uuid": uid("texture"), "source": arm_texture_source(), "particle": False})
    texs[:] = [t for t in texs if t.get("name") != ARM_TEXTURE]
    texs.append(base)
    return len(texs) - 1


def arm_cube(arm, tex_index):
    s = REST_SHOULDER[arm]
    x0, x1 = (-1, 3) if arm == "arm_right" else (-3, 1)
    frm = [s[0] + x0, s[1] - 10, s[2] - 2]
    to = [s[0] + x1, s[1] + 2, s[2] + 2]
    faces = {f: {"uv": [0, 0, 16, 16], "texture": tex_index} for f in ("north", "east", "south", "west", "up", "down")}
    return {"name": arm, "box_uv": False, "render_order": "default", "locked": False, "export": True,
            "scope": 0, "allow_mirror_modeling": True, "from": frm, "to": to, "autouv": 0, "color": 1,
            "origin": list(s), "faces": faces, "type": "cube", "uuid": uid("cube", arm)}


def ensure_arm_groups(bb, tex_index):
    root = bb["outliner"][0]
    assert root["name"] == "root", "il primo gruppo dell'outliner deve essere root"
    arm_cubes = {uid("cube", arm) for arm in ARMS}
    bb["elements"][:] = [e for e in bb["elements"] if e["uuid"] not in arm_cubes]
    root["children"][:] = [c for c in root["children"] if not (isinstance(c, dict) and c.get("name") in ARMS)]
    for arm in ARMS:
        cube = arm_cube(arm, tex_index)
        bb["elements"].append(cube)
        root["children"].append({"name": arm, "origin": list(REST_SHOULDER[arm]), "color": 2,
                                 "uuid": uid("group", arm), "export": True, "isOpen": False, "locked": False,
                                 "visibility": True, "autouv": 0, "children": [cube["uuid"]]})


def bb_value(channel, v):
    sign = pistol_export.SIGN[channel]
    return {a: pistol_export.num(float(c) * s) for a, c, s in zip("xyz", v, sign)}


def write_keys(bb_clip, keys):
    for arm in ARMS:
        gid = uid("group", arm)
        kfs = []
        for channel in ("position", "rotation"):
            for t, v in keys[arm][channel]:
                kfs.append({"channel": channel, "data_points": [bb_value(channel, v)],
                            "uuid": uid("kf", bb_clip["name"], arm, channel, str(t)), "time": t,
                            "color": -1, "interpolation": "linear"})
        bb_clip.setdefault("animators", {})[gid] = {"name": arm, "type": "bone", "keyframes": kfs}


def main(argv):
    path = Path(argv[1]) if len(argv) > 1 else DEFAULT_BBMODEL
    bb = json.loads(path.read_text(encoding="utf-8"))
    tex_index = ensure_texture(bb)
    ensure_arm_groups(bb, tex_index)

    # rig e clip della pistola senza le braccia (le loro tracce vecchie non contano)
    groups, own = {}, {None: []}
    by_uuid = {e["uuid"]: e for e in bb["elements"]}
    pistol_export.walk(bb["outliner"], None, groups, by_uuid, own)
    rig = {n: {"parent": p, "pivot": o} for n, (p, o) in groups.items() if n not in ARMS}
    clips = pistol_export.export_animations(bb)["animations"]
    for bb_clip in bb["animations"]:
        clip = clips[bb_clip["name"]]
        for arm in ARMS:
            clip["bones"].pop(arm, None)
        write_keys(bb_clip, bake(bb_clip["name"], clip, rig))
    path.write_text(json.dumps(bb), encoding="utf-8")
    print(f"braccia animate in {len(bb['animations'])} clip")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
