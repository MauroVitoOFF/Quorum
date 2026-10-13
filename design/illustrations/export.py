"""Esporta le illustrazioni di incoming/ per il sito: WebP ad alta qualita, a piu' larghezze, in website/public/art/.
Uso: python export.py   (le larghezze sono in SIZES)"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, "..", "..", "website", "public", "art"))
SIZES = {
    "hero": [1920, 1280], "closing": [1920, 1280],
    "module-bank": [640], "module-locker": [640], "module-boss": [640],
}
os.makedirs(OUT, exist_ok=True)
for name, widths in SIZES.items():
    im = Image.open(os.path.join(HERE, "incoming", name + ".png")).convert("RGB")
    for w in widths:
        h = round(im.height * w / im.width)
        im.resize((w, h), Image.LANCZOS).save(os.path.join(OUT, f"{name}-{w}.webp"), quality=90, method=6)
for f in sorted(os.listdir(OUT)):
    print(f, os.path.getsize(os.path.join(OUT, f)) // 1024, "KB")
