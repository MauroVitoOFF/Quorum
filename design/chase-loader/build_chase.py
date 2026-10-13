"""Genera chase.bbmodel (modello, rig e animazione "run"). La skin e' dipinta da skin.py (chase.png).

Poliziotto e fuggitivo sono costruiti rivolti a nord (-Z) e il gruppo radice e' ruotato per farli correre
lungo +X; la faccia "east" e' quella vista dalla camera laterale.
In Blockbench una rotazione X positiva porta un arto all'indietro ma inclina il busto in avanti.
"""
import json, math, os, uuid
from skin import *   # W, H, TW, TH, SC, rect, UV, SIZE, PARTS, SKIN_B64

HERE = os.path.dirname(os.path.abspath(__file__))
FWD = float(os.environ.get("FWD", "1"))
ROOT_Y = float(os.environ.get("ROOT_Y", "-90"))

# ---------------------------------------------------------------- modello
elements = []
anim_keys = {}   # uuid gruppo -> {name, kf: {canale: [(t,(x,y,z))]}}


def U():
    return str(uuid.uuid4())


def cube(name, frm, to, u, v, ox=0, origin=None, rotation=None):
    """cubo con box UV (un cubo = una regione di skin)."""
    w, h, d = (to[0] - frm[0], to[1] - frm[1], to[2] - frm[2])
    faces = {}
    for f in ("north", "east", "south", "west", "up", "down"):
        x0, y0, x1, y1 = rect(f, u + ox, v, w, h, d)
        faces[f] = {"uv": [x0, y0, x1, y1], "texture": 0}
    el = {"name": name, "box_uv": True, "rescale": False, "locked": False, "light_emission": 0,
          "render_order": "default", "allow_mirror_modeling": False, "from": frm, "to": to,
          "autouv": 0, "color": 0, "origin": origin or [0, 0, 0], "uv_offset": [u + ox, v],
          "faces": faces, "type": "cube", "uuid": U()}
    if rotation:
        el["rotation"] = rotation
    elements.append(el)
    return el["uuid"]


def limb_cube(name, x0, x1, y0, y1, z0, z1, uvpart, ox, y_bottom):
    """meta di un arto 4x12x4: UV esplicite nella regione dell'arto intero (y_bottom = quota del fondo)."""
    u, v = UV[uvpart]
    w, h, d = SIZE[uvpart]
    top_row, bot_row = (y_bottom + h) - y1, (y_bottom + h) - y0
    faces = {}
    for f in ("north", "east", "south", "west"):
        rx0, ry0, rx1, ry1 = rect(f, u + ox, v, w, h, d)
        faces[f] = {"uv": [rx0, ry0 + top_row, rx1, ry0 + bot_row], "texture": 0}
    faces["up"] = {"uv": list(rect("up", u + ox, v, w, h, d)), "texture": 0}
    faces["down"] = {"uv": list(rect("down", u + ox, v, w, h, d)), "texture": 0}
    el = {"name": name, "box_uv": False, "rescale": False, "locked": False, "light_emission": 0,
          "render_order": "default", "allow_mirror_modeling": False,
          "from": [x0, y0, z0], "to": [x1, y1, z1], "autouv": 0, "color": 0, "origin": [0, 0, 0],
          "faces": faces, "type": "cube", "uuid": U()}
    elements.append(el)
    return el["uuid"]


def group(name, origin, children, rotation=None, anim=None):
    g = {"name": name, "origin": origin, "color": 0, "uuid": U(), "export": True,
         "isOpen": True, "locked": False, "visibility": True, "autouv": 0, "children": children}
    if rotation:
        g["rotation"] = rotation
    if anim:
        anim_keys[g["uuid"]] = {"name": name, "kf": anim}
    return g


LEN = 0.6
N = 8
TS = [LEN * i / N for i in range(N + 1)]
TAU = 2 * math.pi
F = FWD   # avanti = negativo per gli arti, positivo per l'inclinazione del busto (convenzione Blockbench)


def samp(fn):
    return [(t, (fn(t / LEN), 0, 0)) for t in TS]


def character(who, cx, A):
    cfg = PARTS[who]
    ox = cfg["ox"]

    head = cube("head", [cx - 4, 24, -4], [cx + 4, 32, 4], *UV["head"], ox)
    head_kids = [head]
    if who == "cop":
        head_kids.append(cube("cap_crown", [cx - 4.5, 31, -4.5], [cx + 4.5, 34, 4.5], 0, 32, 0))
        head_kids.append(cube("cap_brim", [cx - 4.5, 30.5, -8.5], [cx + 4.5, 31.5, -4.5], 36, 32, 0))
    body = cube("body", [cx - 4, 12, -2], [cx + 4, 24, 2], *UV["body"], ox)

    def arm(side, name):
        x0, x1 = (cx + 4, cx + 8) if side == "n" else (cx - 8, cx - 4)
        part = "arm_" + side
        upper = limb_cube(name + "_upper", x0, x1, 18, 24, -2, 2, part, ox, 12)
        lower = limb_cube(name + "_lower", x0, x1, 12, 18, -2, 2, part, ox, 12)
        return upper, lower, (x0 + x1) / 2

    def leg(side, name):
        x0, x1 = (cx, cx + 4) if side == "n" else (cx - 4, cx)
        part = "leg_" + side
        upper = limb_cube(name + "_upper", x0, x1, 6, 12, -2, 2, part, ox, 0)
        lower = limb_cube(name + "_lower", x0, x1, 0, 6, -2, 2, part, ox, 0)
        return upper, lower, (x0 + x1) / 2

    # ---- braccia
    an_up, an_lo, anx = arm("n", "arm_near")
    af_up, af_lo, afx = arm("f", "arm_far")
    near_lower_kids = [an_lo]
    if who == "cop":
        near_lower_kids.append(cube("baton", [anx - 0.5, 11.5, -1.5], [anx + 0.5, 12.5, 5.5], 0, 44, 0,
                                    origin=[anx, 12, 0], rotation=[35, 0, 0]))
    else:
        near_lower_kids.append(cube("sack", [anx - 0.5, 4.5, -2], [anx + 3.5, 10.5, 2], 0, 50, ox))
        near_lower_kids.append(cube("sack_knot", [anx + 0.5, 10.5, -1], [anx + 2.5, 12.5, 1], 40, 40, ox))
    g_an_lo = group("forearm_near", [anx, 18, 0], near_lower_kids, anim={"rotation": A["fore_n"]})
    g_af_lo = group("forearm_far", [afx, 18, 0], [af_lo], anim={"rotation": A["fore_f"]})
    g_an = group("arm_near", [anx, 22, 0], [an_up, g_an_lo], anim={"rotation": A["arm_n"]})
    g_af = group("arm_far", [afx, 22, 0], [af_up, g_af_lo], anim={"rotation": A["arm_f"]})
    g_head = group("head", [cx, 24, 0], head_kids, anim={"rotation": A["head"]})
    g_torso = group("torso", [cx, 12, 0], [body, g_head, g_an, g_af], anim={"rotation": A["torso"]})

    # ---- gambe
    ln_up, ln_lo, lnx = leg("n", "leg_near")
    lf_up, lf_lo, lfx = leg("f", "leg_far")
    g_ln_lo = group("shin_near", [lnx, 6, 0], [ln_lo], anim={"rotation": A["shin_n"]})
    g_lf_lo = group("shin_far", [lfx, 6, 0], [lf_lo], anim={"rotation": A["shin_f"]})
    g_ln = group("leg_near", [lnx, 12, 0], [ln_up, g_ln_lo], anim={"rotation": A["leg_n"]})
    g_lf = group("leg_far", [lfx, 12, 0], [lf_up, g_lf_lo], anim={"rotation": A["leg_f"]})

    # ---- polvere: tre sbuffi dietro ai piedi (scala + posizione, sfasati)
    dust_groups = []
    for i in range(3):
        pu = cube(f"dust{i}", [cx - 1, 0, 2], [cx + 1, 2, 4], 56, 60, 0 if who == "cop" else 64,
                  origin=[cx, 1, 3])
        t0 = i * LEN / 3
        pos, scl = [], []
        for t in TS:
            ph = ((t - t0) % LEN) / LEN
            pos.append((t, (0, 2.0 * ph, 9 * ph)))
            s = (0.4 + 0.8 * ph / 0.25) if ph < 0.25 else 1.2 * (1 - ph) / 0.75
            scl.append((t, (max(s, 0.01),) * 3))
        dust_groups.append(group(f"dust{i}", [cx, 1, 3], [pu], anim={"position": pos, "scale": scl}))

    return group(f"{who}_root", [cx, 0, 0], [g_torso, g_ln, g_lf] + dust_groups,
                 rotation=[0, ROOT_Y, 0], anim={"position": A["root_pos"]})


def stride(phase, amp=52):
    return samp(lambda p: F * amp * math.sin(TAU * (p + phase)))


def knee(phase, fold=75):
    # lo stinco si ripiega all'indietro (+) mentre la coscia passa in avanti
    return samp(lambda p: F * fold * max(0.0, -math.cos(TAU * (p + phase))))


def bob(amp):
    return [(t, (0, amp * (0.5 - 0.5 * math.cos(2 * TAU * t / LEN)), 0)) for t in TS]


def lean(deg):
    return samp(lambda p: F * deg + 2 * math.sin(2 * TAU * p))


def hold(deg):
    return samp(lambda p: deg)


cop_anim = {
    "head": hold(F * -8), "torso": lean(14),
    "arm_n": samp(lambda p: F * 38 * math.sin(TAU * (p + 0.5))),   # braccio col manganello
    "fore_n": samp(lambda p: -F * (28 + 14 * math.sin(TAU * (p + 0.5)))),
    "arm_f": samp(lambda p: -F * (84 + 4 * math.sin(2 * TAU * p))),  # braccio teso verso il fuggitivo
    "fore_f": hold(-F * 6),
    "leg_n": stride(0.0, 58), "shin_n": knee(0.0, 80),
    "leg_f": stride(0.5, 58), "shin_f": knee(0.5, 80),
    "root_pos": bob(1.6),
}
thief_anim = {
    "head": hold(F * -4), "torso": lean(10),
    "arm_n": samp(lambda p: F * 42 * math.sin(TAU * (p + 0.5))),   # braccio col sacco
    "fore_n": samp(lambda p: -F * (35 + 12 * math.sin(TAU * (p + 0.5)))),
    "arm_f": samp(lambda p: F * 52 * math.sin(TAU * p)),
    "fore_f": samp(lambda p: -F * (70 + 20 * math.sin(TAU * p))),
    "leg_n": stride(0.5, 62), "shin_n": knee(0.5, 85),
    "leg_f": stride(0.0, 62), "shin_f": knee(0.0, 85),
    "root_pos": bob(1.8),
}

outliner_root = [character("cop", -14, cop_anim), character("thief", 14, thief_anim)]

animators = {}
for gid, info in anim_keys.items():
    kfs = []
    for channel, pts in info["kf"].items():
        for (t, (x, y, z)) in pts:
            kfs.append({"channel": channel, "data_points": [{"x": round(x, 3), "y": round(y, 3), "z": round(z, 3)}],
                        "uuid": U(), "time": round(t, 4), "color": -1,
                        "interpolation": "linear" if channel == "scale" else "catmullrom"})
    animators[gid] = {"name": info["name"], "type": "bone", "keyframes": kfs}

model = {
    "meta": {"format_version": "4.10", "creation_time": 0, "model_format": "free", "box_uv": True},
    "name": "chase", "model_identifier": "", "visible_box": [4, 4, 0],
    "variable_placeholders": "", "variable_placeholder_buttons": [],
    "timeline_setups": [], "unhandled_marker_colors": [],
    "resolution": {"width": W, "height": H},
    "elements": elements, "outliner": outliner_root,
    "textures": [{"path": "", "name": "chase.png", "folder": "", "namespace": "", "id": "0", "group": "",
                  "width": TW, "height": TH, "uv_width": W, "uv_height": H, "particle": False,
                  "use_as_default": False, "layered_textures": [], "sync_to_project": "",
                  "render_mode": "default", "render_sides": "auto", "frame_time": 1,
                  "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
                  "visible": True, "internal": True, "saved": True, "uuid": U(),
                  "source": "data:image/png;base64," + SKIN_B64}],
    "animations": [{"uuid": U(), "name": "run", "loop": "loop", "override": False, "length": LEN,
                    "snapping": 20, "selected": True, "anim_time_update": "", "blend_weight": "",
                    "start_delay": "", "loop_delay": "", "animators": animators}],
}
with open(os.path.join(HERE, "chase.bbmodel"), "w", encoding="utf-8") as f:
    json.dump(model, f, indent=1)
print("ok", len(elements), "cubi")
