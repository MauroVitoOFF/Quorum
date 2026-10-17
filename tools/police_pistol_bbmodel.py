#!/usr/bin/env python3
"""Esporta modello, rig e animazioni della pistola in un progetto Blockbench (.bbmodel).

Blockbench non ha un modo Animate per i modelli "Java Block", quindi il progetto e' in formato
"Generic Model": stessi cubi, stessa gerarchia di bone (con i pivot del rig) e le animazioni di
assets/qrm/animations/police_pistol.json. Le curve con nome (easeOutBack, ...) non esistono in
Blockbench, quindi vengono campionate in keyframe lineari ravvicinati (~60 fps).

E' solo un'anteprima: la fonte di verita' resta il JSON delle animazioni. Non va rimportato.

Uso (dalla radice del repo): python tools/police_pistol_bbmodel.py [uscita.bbmodel]
"""
import base64
import json
import math
import sys
import uuid
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import police_pistol_model as pm  # noqa: E402
import police_pistol_pose as pose  # noqa: E402

ASSETS = Path("qrm-neoforge/src/main/resources/assets/qrm")
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("design/police-pistol/police_pistol.bbmodel")
FPS = 60
REST = {"position": [0, 0, 0], "rotation": [0, 0, 0], "scale": [1, 1, 1]}
# Blockbench anima con X della posizione e X/Y della rotazione invertiti rispetto a police_pistol_pose.py
# (verificato confrontando le posizioni mondo dei cubi in Blockbench con quelle calcolate qui)
SIGN = {"position": [-1, 1, 1], "rotation": [-1, -1, 1], "scale": [1, 1, 1]}


def uid():
    return str(uuid.uuid4())


def bake(keys, rest):
    """Keyframe con easing -> lista [(t, v)] lineare equivalente (campionata)."""
    keys = sorted(keys, key=lambda k: k["t"])
    fn = pose.channel(keys, rest)
    out = []

    def put(t, v):
        t = round(t, 4)
        if out and abs(out[-1][0] - t) < 1e-6:
            out[-1] = (t, v)
        else:
            out.append((t, v))

    first = keys[0]
    if first["t"] > 0:
        put(0.0, list(rest))
        put(first["t"] - 0.001, list(rest))
    put(first["t"], [float(x) for x in first["v"]])
    for a, b in zip(keys, keys[1:]):
        ease = b.get("ease", "linear")
        span = b["t"] - a["t"]
        if ease == "step":
            if span > 0.001:
                put(b["t"] - 0.001, [float(x) for x in a["v"]])
        elif ease != "linear" and span > 0:
            n = max(2, math.ceil(span * FPS))
            for i in range(1, n):
                t = a["t"] + span * i / n
                put(t, [round(float(x), 4) for x in fn(t)])
        put(b["t"], [float(x) for x in b["v"]])
    return out


def main():
    model = json.loads((ASSETS / "models/item/police_pistol.json").read_text(encoding="utf-8"))
    anims = json.loads((ASSETS / "animations/police_pistol.json").read_text(encoding="utf-8"))["animations"]
    tex_bytes = (ASSETS / "textures/item/police_pistol.png").read_bytes()
    size = model["texture_size"][0]
    k = size / 16

    owner = {name: bone for bone, name, *_ in pm.CUBES}
    elements, el_uuid = [], {}
    for el in model["elements"]:
        u = uid()
        el_uuid[el["name"]] = u
        faces = {f: {"uv": [round(c * k, 4) for c in d["uv"]], "texture": 0} for f, d in el["faces"].items()}
        e = {
            "name": el["name"], "box_uv": False, "rescale": False, "locked": False, "light_emission": 0,
            "render_order": "default", "allow_mirror_modeling": True,
            "from": el["from"], "to": el["to"], "autouv": 0, "color": 0,
            "origin": el["rotation"]["origin"] if "rotation" in el else [0, 0, 0],
            "faces": faces, "type": "cube", "uuid": u,
        }
        if "rotation" in el:
            e["rotation"] = [el["rotation"]["angle"], 0, 0]
        elements.append(e)

    children = {}
    for name, (parent, _, _) in pm.BONES.items():
        children.setdefault(parent, []).append(name)
    group_uuid = {name: uid() for name in pm.BONES}

    def group(name):
        _, pivot, _ = pm.BONES[name]
        kids = [el_uuid[c] for c, b in owner.items() if b == name] + [group(c) for c in children.get(name, [])]
        return {"name": name, "origin": pivot, "color": 0, "uuid": group_uuid[name], "export": True,
                "isOpen": name in ("root", "body"), "locked": False, "visibility": True, "autouv": 0,
                "children": kids}

    out_anims = []
    for name, a in anims.items():
        animators = {}
        for bone, chans in a["bones"].items():
            kfs = []
            for ch in ("position", "rotation", "scale"):
                if ch not in chans:
                    continue
                for t, v in bake(chans[ch], REST[ch]):
                    v = [v[0] * SIGN[ch][0], v[1] * SIGN[ch][1], v[2] * SIGN[ch][2]]
                    kfs.append({"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}],
                                "uuid": uid(), "time": t, "color": -1, "interpolation": "linear"})
            animators[group_uuid[bone]] = {"name": bone, "type": "bone", "keyframes": kfs}
        loop = "loop" if a.get("loop") else ("hold" if a.get("hold_last") else "once")
        out_anims.append({
            "uuid": uid(), "name": name, "loop": loop, "override": False, "length": a["length"],
            "snapping": 60, "selected": False, "anim_time_update": "", "blend_weight": "",
            "start_delay": "", "loop_delay": "", "animators": animators,
        })

    project = {
        "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
        "name": "police_pistol", "model_identifier": "police_pistol",
        "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [],
        "timeline_setups": [], "unhandled_root_fields": {},
        "resolution": {"width": size, "height": size},
        "elements": elements, "outliner": [group("root")],
        "textures": [{
            "path": "", "name": "police_pistol.png", "folder": "", "namespace": "", "id": "0",
            "width": size, "height": size, "uv_width": size, "uv_height": size, "particle": False,
            "use_as_default": False, "layers_enabled": False, "sync_to_project": "", "render_mode": "default",
            "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
            "frame_interpolate": False, "visible": True, "internal": True, "saved": False, "uuid": uid(),
            "source": "data:image/png;base64," + base64.b64encode(tex_bytes).decode(),
        }],
        "animations": out_anims,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(project), encoding="utf-8")
    print(f"scritto {OUT} ({len(elements)} cubi, {len(out_anims)} animazioni, {OUT.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
