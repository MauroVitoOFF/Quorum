#!/usr/bin/env python3
"""Genera la texture della pistola d'ordinanza (police_pistol.png).

Legge le coordinate UV direttamente dal modello (police_pistol.json, generato da
police_pistol_model.py), cosi' la texture combacia sempre con i cubi: ogni faccia
viene dipinta nel suo rettangolo. Densita' costante: 1 unita' di Blockbench = 2 pixel.

Orientamento delle facce (muso verso -Z): sulla faccia "east" la u cresce verso
il muso, su "west" verso il calcio; "up" ha il nord in alto.

Uso (dalla radice del repo):
    python tools/police_pistol_texture.py [modello.json] [uscita.png]
"""
import json
import sys
from pathlib import Path

from PIL import Image

ASSETS = Path("qrm-neoforge/src/main/resources/assets/qrm")
MODEL = Path(sys.argv[1]) if len(sys.argv) > 1 else ASSETS / "models/item/police_pistol.json"
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else ASSETS / "textures/item/police_pistol.png"


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# Palette
SLIDE, SLIDE_HI, SLIDE_LO = hexc("2B2D2F"), hexc("3A3D40"), hexc("1D1F21")
FRAME, FRAME_HI, FRAME_LO = hexc("3A3F45"), hexc("4B515A"), hexc("2A2E33")
STEEL, STEEL_HI = hexc("50555B"), hexc("6B7178")
PLATE, PLATE_HI = hexc("5A6068"), hexc("737A83")
WHITE = hexc("F2F2EA")
BRASS, BRASS_HI, BRASS_LO = hexc("B8923A"), hexc("D9B556"), hexc("7A5F22")
COPPER, COPPER_HI, COPPER_LO = hexc("B87333"), hexc("D99A5B"), hexc("7A4A22")
PRIMER = hexc("C9CCCF")
BLACK = hexc("101214")


def palette(name):
    if name in ("barrel_tube", "barrel_hood") or name.startswith((
            "trigger_blade", "trigger_shoe", "trigger_safety", "trigger_bar",
            "extractor", "slide_stop", "takedown", "mag_release")):
        return STEEL, STEEL_HI, SLIDE_LO
    if name.startswith(("slide_", "rear_plate", "front_sight", "rear_sight")):
        return SLIDE, SLIDE_HI, SLIDE_LO
    if name == "magazine_base":
        return PLATE, PLATE_HI, FRAME_LO
    if name == "magazine_body":
        return FRAME_LO, FRAME, hexc("1F2328")
    if name in ("round_case", "casing_ejected"):
        return BRASS, BRASS_HI, BRASS_LO
    if name == "round_bullet":
        return COPPER, COPPER_HI, COPPER_LO
    return FRAME, FRAME_HI, FRAME_LO


def main():
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    size = model["texture_size"][0]
    k = size / 16
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()

    for el in model["elements"]:
        name = el["name"]
        base, hi, lo = palette(name)
        for face, f in el["faces"].items():
            u0, v0, u1, v1 = f["uv"]
            x0, y0 = round(u0 * k), round(v0 * k)
            w, h = round((u1 - u0) * k), round((v1 - v0) * k)

            def p(x, y, c, x0=x0, y0=y0, w=w, h=h):
                if 0 <= x < w and 0 <= y < h:
                    px[x0 + x, y0 + y] = c

            def fill(c):
                for yy in range(h):
                    for xx in range(w):
                        p(xx, yy, c)

            def checker(c, margin=1):
                for yy in range(margin, h - margin):
                    for xx in range(w):
                        if (xx + yy) % 2 == 0:
                            p(xx, yy, c)

            side = face in ("east", "west")
            ring = face in ("north", "south", "east", "west")
            east = face == "east"
            fill(base)
            if ring:  # luce dall'alto: bordo chiaro sopra, scuro sotto
                for xx in range(w):
                    p(xx, 0, hi)
                    if h > 2:
                        p(xx, h - 1, lo)
            elif face == "up":
                for xx in range(w):
                    p(xx, 0, hi)
                for yy in range(h):
                    p(0, yy, hi)
            elif face == "down":
                for xx in range(w):
                    p(xx, h - 1, lo)

            # ------------------------------------------------------------ carrello
            if name in ("slide_front", "slide_rear", "slide_wall_left", "port_plug"):
                z0, z1 = el["from"][2], el["to"][2]

                def col(zs):
                    """Colonna di pixel (mezza unita') che parte a z=zs, sul lato est (u verso il muso) o ovest."""
                    return round((z1 - zs - 0.5) * 2) if east else round((zs - z0) * 2)

                if side:
                    for zs in (3.0, 4.0, 5.0, 11.5, 12.5, 13.5):          # zigrinature anteriore e posteriore
                        if z0 <= zs < z1 and name != "port_plug":
                            for yy in range(1, h):
                                p(col(zs), yy, lo)
                    if not east and name != "port_plug":                  # incisione laterale
                        for zs in (6.0, 6.5, 7.0):
                            if z0 <= zs < z1:
                                p(col(zs), 1, hi)
                if name == "port_plug" and east:                          # finestra di espulsione dipinta
                    fill(SLIDE_LO)
                    for xx in range(w):
                        p(xx, 0, lo)
                    for yy in range(h):
                        p(0, yy, lo); p(w - 1, yy, lo)
                if name == "port_plug" and not east:
                    fill(SLIDE_LO)
                if face == "north" and name == "slide_front":
                    p(0, h - 1, lo); p(w - 1, h - 1, lo)
                if face == "south" and name == "slide_rear":
                    p(1, 1, lo); p(w - 2, 1, lo)
            elif name == "slide_lower":
                if face == "up":
                    fill(SLIDE_LO)
            elif name == "slide_top":
                if face == "up":
                    for yy in range(1, h):
                        p(w // 2, yy, SLIDE_LO)          # scanalatura centrale
            elif name == "rear_plate":
                if face == "south":
                    for xx in (w // 2 - 1, w // 2):
                        p(xx, h // 2, lo); p(xx, h // 2 - 1, lo)
            elif name == "extractor":
                if east:
                    p(w // 2, h // 2, BRASS)
            elif name == "front_sight":
                if face in ("north", "up"):
                    for xx in range(w):
                        p(xx, 0 if face == "north" else h - 1, WHITE)
            elif name in ("rear_sight_l", "rear_sight_r"):
                if face == "south":
                    p(w // 2, 1, WHITE)
                if face == "up":
                    for yy in range(h):
                        p(0, yy, lo)

            # --------------------------------------------------------------- canna
            elif name == "barrel_tube":
                if face == "north":
                    fill(STEEL)
                    for yy in range(h // 2 - 1, h // 2 + 1):
                        for xx in range(w // 2 - 1, w // 2 + 1):
                            p(xx, yy, BLACK)
                if side or face in ("up", "down"):
                    for yy in range(h):
                        p(1, yy, STEEL_HI)
            elif name == "barrel_hood":
                if east:
                    p(w // 2, h // 2, SLIDE_LO)
                if face == "up":
                    for xx in range(w):
                        p(xx, h // 2, STEEL_HI)

            # --------------------------------------------------------- cartuccia
            elif name in ("round_case", "casing_ejected"):
                if face in ("south", "north"):          # fondello con innesco / bocca
                    fill(BRASS_LO)
                    p(w // 2, h // 2, PRIMER if face == "south" else BLACK)
                if side:
                    p(w - 1, h // 2, BRASS_LO)           # gola del bossolo
                    p(w - 2, h // 2, BRASS_LO)
            elif name == "round_bullet":
                if face == "north":
                    fill(COPPER_HI)

            # ---------------------------------------------------------------- fusto
            elif name == "frame_dustcover":
                if face == "down":
                    for yy in range(1, h, 2):          # tacche della slitta accessori
                        for xx in range(w):
                            p(xx, yy, lo)
                if side:
                    for xx in range(2, w - 2, 3):
                        p(xx, h - 1, lo)
            elif name == "frame_mid":
                if side:
                    p(w - 3, 1, lo)
                    if east:
                        for xx in range(2, 7):
                            p(xx, 2, hi)          # matricola incisa
            elif name == "frame_rear":
                if face == "up":
                    for yy in range(h):
                        p(0, yy, lo); p(w - 1, yy, lo)
            elif name in ("takedown_lever", "slide_stop", "mag_release"):
                if face == "west":
                    for yy in range(h):
                        p(w // 2, yy, hi)
                    for xx in range(0, w, 2):
                        p(xx, h - 1, lo)

            # ----------------------------------------------------------- impugnatura
            elif name == "grip_main":
                if ring:
                    checker(lo)
            elif name == "grip_backstrap":
                if ring:
                    checker(lo)
                if face == "south":
                    for yy in range(1, h - 1):
                        p(w // 2, yy, hi)
            elif name.startswith("grip_ridge"):
                if ring:
                    for xx in range(w):
                        p(xx, 0, FRAME_HI)
                        p(xx, h - 1, FRAME_LO)

            # ------------------------------------------------------------ caricatore
            elif name == "magazine_body":
                if side:
                    for yy in range(2, h - 3, 3):          # finestrelle conteggio colpi
                        for xx in range(w // 2 - 1, w // 2 + 1):
                            p(xx, yy, BRASS if yy < h - 8 else BRASS_LO)
                if face == "up":
                    fill(FRAME_LO)
                    for xx in range(1, w - 1):       # labbra del caricatore
                        p(xx, 0, STEEL); p(xx, h - 1, STEEL)
                    p(w // 2, h // 2, BRASS); p(w // 2 - 1, h // 2, BRASS)
            elif name == "magazine_base":
                if ring:
                    for xx in range(w):
                        p(xx, 0, PLATE_HI)
                if face == "down":
                    for yy in range(1, h - 1):
                        for xx in range(1, w - 1):
                            if (xx + yy) % 3 == 0:
                                p(xx, yy, FRAME_LO)

            # ---------------------------------------------------------- ponticello
            elif name in ("guard_post", "guard_bottom"):
                if ring or face == "up":
                    for xx in range(w):
                        p(xx, 0, hi)

            # ----------------------------------------------------------- grilletto
            elif name == "trigger_blade":
                if face in ("north", "south"):
                    for yy in range(h):
                        p(w // 2, yy, STEEL_HI)
                if side:
                    for yy in range(h):
                        p(0, yy, STEEL_HI)
            elif name == "trigger_safety":
                if face == "north":
                    fill(STEEL_HI)
            elif name == "trigger_bar":
                if face == "up":
                    for yy in range(h):
                        p(w // 2, yy, STEEL_HI)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print(f"scritto {OUT} ({size}x{size})")


if __name__ == "__main__":
    main()
