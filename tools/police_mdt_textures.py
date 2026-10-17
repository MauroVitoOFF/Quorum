"""Genera le due texture 16x16 del modulo qrm-police (tablet e terminale). Solo Pillow."""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "qrm-police/src/main/resources/assets/qrm_police/textures"


def tablet() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for x in range(3, 13):
        for y in range(1, 15):
            px[x, y] = (24, 30, 44, 255)
    for x in range(4, 12):
        for y in range(2, 12):
            px[x, y] = (14, 40, 84, 255)
    for x in range(5, 11):
        px[x, 4] = (59, 130, 246, 255)
        px[x, 6] = (96, 165, 250, 255)
        px[x, 8] = (59, 130, 246, 255)
    px[7, 13] = (59, 130, 246, 255)
    px[8, 13] = (59, 130, 246, 255)
    return img


def computer() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (28, 34, 48, 255))
    px = img.load()
    for i in range(16):
        for edge in (0, 15):
            px[i, edge] = (16, 20, 32, 255)
            px[edge, i] = (16, 20, 32, 255)
    for x in range(2, 14):
        for y in range(2, 10):
            px[x, y] = (14, 40, 84, 255)
    for x in range(3, 13):
        px[x, 4] = (59, 130, 246, 255)
        px[x, 6] = (96, 165, 250, 255)
        px[x, 8] = (59, 130, 246, 255)
    for x in range(3, 13):
        for y in (12, 13):
            px[x, y] = (46, 56, 78, 255)
    return img


if __name__ == "__main__":
    (OUT / "item").mkdir(parents=True, exist_ok=True)
    (OUT / "block").mkdir(parents=True, exist_ok=True)
    tablet().save(OUT / "item/police_tablet.png")
    computer().save(OUT / "block/police_computer.png")
    print("ok")
