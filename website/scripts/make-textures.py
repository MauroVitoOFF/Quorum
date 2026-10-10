"""Genera le texture pixel originali usate nelle animazioni del sito (public/pixel/).

Sono disegnate qui con rumore a seme fisso: non derivano dalle texture di Minecraft.
Uso: python scripts/make-textures.py   (richiede Pillow)
"""
import random
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / 'public' / 'pixel'


def noise(base, spread, seed, size=16):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            d = rnd.randint(-spread, spread)
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))
    return img


def save(img, name):
    img.save(OUT / name)


dirt = noise((124, 84, 54), 14, 1)
stone = noise((128, 132, 138), 12, 2)
grass_top = noise((88, 142, 62), 14, 3)

grass_side = dirt.copy()
rnd = random.Random(4)
for x in range(16):
    h = rnd.choice([3, 4, 4, 5])
    for y in range(h):
        d = rnd.randint(-14, 14)
        grass_side.putpixel((x, y), (88 + d, 142 + d, 62 + d, 255))

save(dirt, 'dirt.png')
save(stone, 'stone.png')
save(grass_top, 'grass_top.png')
save(grass_side, 'grass_side.png')

# fasi di rottura del blocco: 10 fotogrammi 16x16 in una striscia, crepe via via più fitte
strip = Image.new('RGBA', (160, 16), (0, 0, 0, 0))
rnd = random.Random(7)
order = [(x, y) for x in range(16) for y in range(16)]
rnd.shuffle(order)
cracked = []
for stage in range(10):
    # ogni fase aggiunge una "crepa" a random camminando dal centro
    x, y = 8, 8
    for _ in range(5 + stage * 4):
        x = max(0, min(15, x + rnd.choice([-1, 0, 1])))
        y = max(0, min(15, y + rnd.choice([-1, 0, 1])))
        cracked.append((x, y))
    frame = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (cx, cy) in cracked:
        frame.putpixel((cx, cy), (0, 0, 0, 170))
    strip.paste(frame, (stage * 16, 0))
save(strip, 'crack.png')
print('texture scritte in', OUT)
