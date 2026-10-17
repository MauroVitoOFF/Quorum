#!/usr/bin/env python3
"""Anteprima software delle animazioni della pistola: fogli di fotogrammi in PNG.

Usa le stesse formule di police_pistol_pose.py (quelle da replicare in gioco), quindi serve
a verificare a occhio una posa senza aprire il gioco o Blockbench.

Uso (dalla radice del repo):
    python tools/police_pistol_preview.py <animazione|rest> [COLxRIGHE] [vista] [uscita.png] [t0 t1]
      vista: side (da destra, muso a destra) | q (3/4 dall'alto) | front | rear
"""
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
import police_pistol_pose as pose  # noqa: E402

ASSETS = Path("qrm-neoforge/src/main/resources/assets/qrm")
VIEWS = {
    "side": (1, 0, 0),
    "q": (0.75, 0.55, -0.55),
    "front": (0.15, 0.1, -1),
    "rear": (0.35, 0.2, 1),
}
LIGHT = np.array([0.3, 0.8, 0.5]) / np.linalg.norm([0.3, 0.8, 0.5])


def load():
    model = json.loads((ASSETS / "models/item/police_pistol.json").read_text(encoding="utf-8"))
    rig = json.loads((ASSETS / "rigs/police_pistol.json").read_text(encoding="utf-8"))
    anims = json.loads((ASSETS / "animations/police_pistol.json").read_text(encoding="utf-8")) \
        if (ASSETS / "animations/police_pistol.json").exists() else {"animations": {}}
    tex = Image.open(ASSETS / "textures/item/police_pistol.png").convert("RGBA")
    owner = {}
    for p in (ASSETS / "models/item/police_pistol_parts").glob("*.json"):
        for e in json.loads(p.read_text(encoding="utf-8"))["elements"]:
            owner[e["name"]] = p.stem
    return model, rig, anims["animations"], tex, owner


FACES = {  # nome: (assi dei 4 angoli, normale)
    "east": ([(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)], (1, 0, 0)),
    "west": ([(0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 0, 1)], (-1, 0, 0)),
    "up": ([(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], (0, 1, 0)),
    "down": ([(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)], (0, -1, 0)),
    "south": ([(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)], (0, 0, 1)),
    "north": ([(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)], (0, 0, -1)),
}


def face_color(tex, uv, k):
    u0, v0, u1, v1 = (round(c * k) for c in uv)
    px = [tex.getpixel((x, y))[:3] for x in range(u0, u1) for y in range(v0, v1)]
    return np.mean(px, axis=0) if px else np.array([128, 128, 128])


def render(model, rig, anim, tex, owner, t, view="side", scale=20, size=(420, 330), center=(8, 7, 8)):
    mats = pose.world_matrices(rig, anim, t)
    k = model["texture_size"][0] / 16
    eye = np.array(VIEWS[view], float)
    eye /= np.linalg.norm(eye)
    fwd = -eye
    right = np.cross(fwd, [0, 1, 0])
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    polys = []
    for el in model["elements"]:
        m = mats[owner[el["name"]]]
        (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
        lo, hi = np.array([x0, y0, z0], float), np.array([x1, y1, z1], float)
        r = el.get("rotation")
        for fname, f in el["faces"].items():
            corners, normal = FACES[fname]
            pts = [lo + (hi - lo) * np.array(c) for c in corners]
            n = np.array(normal, float)
            if r:
                rm = pose.translate(r["origin"]) @ pose.rot_xyz([r["angle"], 0, 0]) @ pose.translate(-np.array(r["origin"], float))
                pts = [(rm @ np.append(p, 1))[:3] for p in pts]
                n = (pose.rot_xyz([r["angle"], 0, 0]) @ np.append(n, 0))[:3]
            pts = [(m @ np.append(p, 1))[:3] for p in pts]
            n = (m @ np.append(n, 0))[:3]
            ln = np.linalg.norm(n)
            if ln < 1e-6:
                continue
            n /= ln
            if n @ eye <= 1e-6:
                continue
            col = face_color(tex, f["uv"], k) * (0.55 + 0.6 * max(0.0, float(n @ LIGHT)))
            depth = float(np.mean([p @ eye for p in pts]))
            polys.append((depth, pts, tuple(int(min(255, c)) for c in col)))
    polys.sort(key=lambda p: p[0])
    ss = 2
    img = Image.new("RGB", (size[0] * ss, size[1] * ss), (226, 228, 232))
    d = ImageDraw.Draw(img)
    c = np.array(center, float)
    for _, pts, col in polys:
        xy = []
        for p in pts:
            q = p - c
            xy.append((size[0] * ss / 2 + (q @ right) * scale * ss, size[1] * ss / 2 - (q @ up) * scale * ss))
        d.polygon(xy, fill=col)
    return img.resize(size, Image.LANCZOS)


def main():
    name = sys.argv[1] if len(sys.argv) > 1 else "rest"
    grid = sys.argv[2] if len(sys.argv) > 2 else "6x1"
    cols, rows = (int(v) for v in grid.lower().split("x"))
    view = sys.argv[3] if len(sys.argv) > 3 else "side"
    out = Path(sys.argv[4]) if len(sys.argv) > 4 else Path(f"preview_{name}.png")
    model, rig, anims, tex, owner = load()
    if name == "rest":
        out_img = render(model, rig, None, tex, owner, 0, view)
        out_img.save(out)
        print("scritto", out)
        return
    anim = anims[name]
    t0 = float(sys.argv[5]) if len(sys.argv) > 5 else 0.0
    t1 = float(sys.argv[6]) if len(sys.argv) > 6 else anim["length"]
    n = cols * rows
    w, h = 420, 330
    sheet = Image.new("RGB", (cols * w, rows * h), (226, 228, 232))
    dr = ImageDraw.Draw(sheet)
    for i in range(n):
        t = t0 + (t1 - t0) * (i / max(1, n - 1))
        sheet.paste(render(model, rig, anim, tex, owner, t, view, size=(w, h)), ((i % cols) * w, (i // cols) * h))
        dr.text(((i % cols) * w + 8, (i // cols) * h + 6), f"{name}  t={t:.2f}s", fill=(20, 20, 20))
    sheet.save(out)
    print("scritto", out, f"({n} fotogrammi)")


if __name__ == "__main__":
    main()
