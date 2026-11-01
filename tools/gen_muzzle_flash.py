#!/usr/bin/env python3
"""Genera la texture pixel art della vampa della pistola (16x16): stella irregolare giallo-arancio con nucleo chiaro.

Uso (dalla radice del repo): python tools/gen_muzzle_flash.py
"""
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "qrm-police/src/main/resources/assets/qrm_police/textures/effect/muzzle_flash.png"
SIZE = 16
# (raggio massimo relativo, colore): dal bordo al nucleo
PALETTE = [
    (1.00, (214, 92, 14, 200)),
    (0.78, (240, 140, 24, 235)),
    (0.55, (255, 196, 56, 255)),
    (0.30, (255, 236, 150, 255)),
    (0.14, (255, 250, 220, 255)),
]


def main():
    rnd = random.Random(7)
    spikes = 7
    phase = [rnd.uniform(0.6, 1.0) for _ in range(spikes)]
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    c = (SIZE - 1) / 2
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy) / (SIZE / 2)
            a = math.atan2(dy, dx)
            k = (a / (2 * math.pi) + 0.5) * spikes
            i = int(k) % spikes
            f = k - int(k)
            lo, hi = phase[i], phase[(i + 1) % spikes]
            # raggio della stella: picchi sulle punte, valli tra l'una e l'altra
            edge = 0.30 + 0.72 * (lo * (1 - f) + hi * f) * abs(math.cos(f * math.pi)) ** 1.5
            if r > edge:
                continue
            rel = r / edge
            for limit, col in reversed(PALETTE):
                if rel <= limit:
                    img.putpixel((x, y), col)
                    break
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print("scritto", OUT)


if __name__ == "__main__":
    main()
