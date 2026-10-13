"""Ritaglia i fotogrammi renderizzati da Blockbench e produce sprite sheet e anteprima."""
import glob, os
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__))
frames = [Image.open(p).convert("RGBA") for p in sorted(glob.glob(os.path.join(HERE, "_frames", "f*.png")))]
boxes = [f.getchannel("A").getbbox() for f in frames]
x0 = min(b[0] for b in boxes); y0 = min(b[1] for b in boxes)
x1 = max(b[2] for b in boxes); y1 = max(b[3] for b in boxes)
pad = 24
box = (x0 - pad, y0 - pad, x1 + pad, y1 + pad)
w, h = box[2] - box[0], box[3] - box[1]
print("union bbox", (x0, y0, x1, y1), "crop", w, "x", h)
crops = [f.crop(box) for f in frames]
sheet = Image.new("RGBA", (w * len(crops), h), (0, 0, 0, 0))
for i, c in enumerate(crops):
    sheet.paste(c, (i * w, 0))
sheet.save(os.path.join(HERE, "chase-sheet.png"))
bgs = []
for c in crops:
    bg = Image.new("RGBA", c.size, (36, 36, 40, 255)); bg.alpha_composite(c); bgs.append(bg.convert("RGB"))
bgs[0].save(os.path.join(HERE, "_frames", "preview.gif"), save_all=True, append_images=bgs[1:], duration=50, loop=0)
# striscia di controllo ridotta
small = [b.resize((w // 3, h // 3), Image.LANCZOS) for b in bgs]
strip = Image.new("RGB", (small[0].width * 6, small[0].height * 2))
for i, s in enumerate(small):
    strip.paste(s, ((i % 6) * s.width, (i // 6) * s.height))
strip.save(os.path.join(HERE, "_frames", "strip.png"))
