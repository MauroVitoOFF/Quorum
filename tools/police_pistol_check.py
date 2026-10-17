#!/usr/bin/env python3
"""Controlli di coerenza su modello, texture e rig della pistola d'ordinanza.

  1. nessun rettangolo UV ne copre un altro (le texture non si intersecano)
  2. ogni pixel UV e' opaco nella texture e nessun pixel opaco e' fuori dalle UV
  3. nessuna coppia di facce coincidenti e sovrapposte (z-fighting) tra cubi con la stessa rotazione
  4. i modelli delle parti contengono esattamente i cubi del modello completo
  5. ogni bone del rig con "has_model" ha il suo file

Uso (dalla radice del repo): python tools/police_pistol_check.py
"""
import json
import sys
from pathlib import Path

from PIL import Image

ASSETS = Path("qrm-neoforge/src/main/resources/assets/qrm")
errors = []


def err(msg):
    errors.append(msg)
    print("ERRORE:", msg)


def main():
    model = json.loads((ASSETS / "models/item/police_pistol.json").read_text(encoding="utf-8"))
    img = Image.open(ASSETS / "textures/item/police_pistol.png").convert("RGBA")
    tex = model["texture_size"][0]
    assert img.size == (tex, tex), (img.size, tex)
    k = tex / 16

    # 1 + 2: UV
    owner = {}
    for el in model["elements"]:
        for face, f in el["faces"].items():
            u0, v0, u1, v1 = (round(v * k) for v in f["uv"])
            if abs(f["uv"][0] * k - u0) > 1e-6 or abs(f["uv"][2] * k - u1) > 1e-6:
                err(f"{el['name']}.{face}: UV non allineate al pixel")
            for x in range(u0, u1):
                for y in range(v0, v1):
                    if (x, y) in owner:
                        err(f"{el['name']}.{face} si sovrappone a {owner[(x, y)]} in ({x},{y})")
                    owner[(x, y)] = f"{el['name']}.{face}"
                    if img.getpixel((x, y))[3] != 255:
                        err(f"{el['name']}.{face}: pixel trasparente in ({x},{y})")
    stray = sum(1 for x in range(tex) for y in range(tex)
                if img.getpixel((x, y))[3] and (x, y) not in owner)
    if stray:
        err(f"{stray} pixel opachi non referenziati da nessuna UV")
    print(f"UV: {len(owner)} pixel usati su {tex * tex} ({len(model['elements'])} cubi)")

    # 3: facce coincidenti e sovrapposte
    def rot_key(el):
        r = el.get("rotation")
        return None if not r else (r["angle"], r["axis"], tuple(r["origin"]))

    def faces(el):
        (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
        return {
            "east": (0, x1, ((y0, y1), (z0, z1))), "west": (0, x0, ((y0, y1), (z0, z1))),
            "up": (1, y1, ((x0, x1), (z0, z1))), "down": (1, y0, ((x0, x1), (z0, z1))),
            "south": (2, z1, ((x0, x1), (y0, y1))), "north": (2, z0, ((x0, x1), (y0, y1))),
        }

    els = model["elements"]
    fights = 0
    for i in range(len(els)):
        for j in range(i + 1, len(els)):
            a, b = els[i], els[j]
            if rot_key(a) != rot_key(b):
                continue
            fa, fb = faces(a), faces(b)
            for na, (axa, pa, ra) in fa.items():
                for nb, (axb, pb, rb) in fb.items():
                    if na != nb or axa != axb or abs(pa - pb) > 1e-6:
                        continue
                    ov = [min(ra[d][1], rb[d][1]) - max(ra[d][0], rb[d][0]) for d in range(2)]
                    if ov[0] > 1e-6 and ov[1] > 1e-6:
                        err(f"z-fighting: {a['name']}.{na} e {b['name']}.{nb} coincidono ({ov[0]:.2f}x{ov[1]:.2f})")
                        fights += 1
    print(f"facce coincidenti: {fights}")

    # 4: parti
    part_dir = ASSETS / "models/item/police_pistol_parts"
    in_parts = []
    for p in sorted(part_dir.glob("*.json")):
        in_parts += [e["name"] for e in json.loads(p.read_text(encoding="utf-8"))["elements"]]
    if sorted(in_parts) != sorted(e["name"] for e in els):
        err("i modelli delle parti non coincidono con il modello completo")
    print(f"parti: {len(list(part_dir.glob('*.json')))} file, {len(in_parts)} cubi")

    # 5: rig
    rig = json.loads((ASSETS / "rigs/police_pistol.json").read_text(encoding="utf-8"))
    for b, v in rig["bones"].items():
        if v["has_model"] and not (part_dir / f"{b}.json").exists():
            err(f"bone {b}: manca il file del modello")
    print(f"rig: {len(rig['bones'])} bone")

    print("TUTTO OK" if not errors else f"{len(errors)} problemi")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
