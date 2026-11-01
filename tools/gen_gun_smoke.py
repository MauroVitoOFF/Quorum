#!/usr/bin/env python3
"""Genera le texture pixel art del fumo dello sparo (3 varianti 16x16): sbuffi grigio chiaro semitrasparenti, a blocchi 2x2.

Uso (dalla radice del repo): python tools/gen_gun_smoke.py
"""
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "qrm-police/src/main/resources/assets/qrm_police/textures/particle"
GRID = 8  # blocchi per lato; ogni blocco e' 2x2 pixel


def puff(seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (GRID * 2, GRID * 2), (0, 0, 0, 0))
    c = (GRID - 1) / 2
    for by in range(GRID):
        for bx in range(GRID):
            d = math.hypot(bx - c, by - c) / (GRID / 2)
            if d + rnd.uniform(-0.28, 0.28) > 0.95:
                continue
            a = max(60, int(215 * (1 - d * 0.75)) + rnd.randint(-25, 25))
            g = rnd.randint(205, 245)
            for dy in range(2):
                for dx in range(2):
                    img.putpixel((bx * 2 + dx, by * 2 + dy), (g, g, g - 4, a))
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for i in range(3):
        puff(11 + i * 5).save(OUT / f"gun_smoke_{i}.png")
    print("scritte 3 texture in", OUT)


if __name__ == "__main__":
    main()
