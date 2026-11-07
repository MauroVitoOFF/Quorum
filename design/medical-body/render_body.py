"""Renderizza il manichino del modulo medico in stile Minecraft: blocchi netti con le proporzioni del giocatore,
luce per faccia come nel gioco (sopra piena, davanti 80%, lati 60%), texture a pixel e un volto minimale.

Per ogni zona scrive un PNG con la stessa inquadratura (si sovrappongono senza ritocchi):
  RGB = luminosita' in scala di grigi, ALPHA = 255 sulla zona. A runtime il PNG viene moltiplicato per il colore
  della ferita (blit con colore). Nessuna sfumatura: pixel netti, da disegnare senza filtro.

Uso (dalla radice del repo):  python design/medical-body/render_body.py [cartella-uscita] [altezza-px]
  altezza-px = altezza dell'immagine in pixel. Default 22 (come la hotbar) -> immagine 11x22.
Dipendenze: numpy, Pillow. Deterministico.
"""
import os
import sys
import numpy as np
from PIL import Image

PARTS = ["head", "chest", "abdomen", "arm_right", "arm_left", "leg_right", "leg_left"]
# (centro, semi-dimensioni) in unita' del modello
BOXES = {
    "head": ((0, 28, 0), (4, 4, 4)),
    "chest": ((0, 21, 0), (4, 3, 2)),
    "abdomen": ((0, 15, 0), (4, 3, 2)),
    "arm_right": ((6, 18, 0), (2, 6, 2)),   # destra del personaggio = sinistra sullo schermo
    "arm_left": ((-6, 18, 0), (2, 6, 2)),
    "leg_right": ((2, 6, 0), (2, 6, 2)),
    "leg_left": ((-2, 6, 0), (2, 6, 2)),
}
BASE = 0.62          # luminosita' media della pietra: e' il tono di partenza, il colore (grigio scuro se illeso) arriva dalla tinta
EDGE_GAIN = 0.55     # quanto si schiarisce il contorno di ogni zona
CANVAS_W, CANVAS_H = 16, 32  # unita' del modello visibili: esattamente la figura, senza margini


def sdf(p, c, h):
    q = np.abs(p - np.array(c, np.float32)) - np.array(h, np.float32)
    return np.linalg.norm(np.maximum(q, 0), axis=-1) + np.minimum(np.max(q, axis=-1), 0)


def scene(p):
    return np.stack([sdf(p, *BOXES[n]) for n in PARTS], axis=-1)


def hash3(a, b, c, k):
    """Rumore deterministico in [-1, 1] per texel di skin."""
    n = (a * 73856093) ^ (b * 19349663) ^ (c * 83492791) ^ (k * 2654435761 & 0x7FFFFFFF)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n & 0xFFFF) / 32767.5) - 1.0


def render(height=22):
    scale = height / CANVAS_H  # pixel per unita' del modello (puo' essere minore di 1)
    w, h = round(CANVAS_W * scale), height
    cam = np.array([0, 16, -130], np.float32)  # vista esattamente frontale
    target = np.array([0, 16, 0], np.float32)
    fwd = target - cam
    fwd /= np.linalg.norm(fwd)
    right = np.cross(fwd, np.array([0, 1, 0], np.float32))
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    xs = ((np.arange(w, dtype=np.float32) + 0.5) / w - 0.5) * CANVAS_W
    ys = (0.5 - (np.arange(h, dtype=np.float32) + 0.5) / h) * CANVAS_H
    gx, gy = np.meshgrid(xs, ys)
    pts = target + right * gx[..., None] + up * gy[..., None]
    org = (pts - fwd * 100).reshape(-1, 3).astype(np.float32)  # camera ortografica: raggi paralleli
    n = org.shape[0]
    t = np.zeros(n, np.float32)
    active = np.arange(n)
    hit = np.zeros(n, bool)
    for _ in range(80):
        if active.size == 0:
            break
        p = org[active] + fwd * t[active, None]
        d = scene(p).min(axis=-1)
        done = d < 0.005
        hit[active[done]] = True
        t[active] += np.where(done, 0, d)
        active = active[~done & (t[active] < 220)]
    p = org + fwd * t[:, None]
    pid = np.full(n, -1, np.int8)
    pid[hit] = scene(p[hit]).argmin(axis=-1)

    # normale della faccia: asse dominante dell'offset dal centro della scatola colpita
    nrm = np.zeros((n, 3), np.float32)
    for i, name in enumerate(PARTS):
        m = pid == i
        if not m.any():
            continue
        c, hh = BOXES[name]
        rel = (p[m] - np.array(c, np.float32)) / np.array(hh, np.float32)
        ax = np.abs(rel).argmax(axis=-1)
        nn = np.zeros((m.sum(), 3), np.float32)
        nn[np.arange(m.sum()), ax] = np.sign(rel[np.arange(m.sum()), ax])
        nrm[m] = nn

    # pietra scura: tre scale di rumore per texel di skin (macchie da 4, da 2 e da 1), crepe e puntini chiari
    q = np.floor(p - nrm * 0.02).astype(np.int64)
    axis = np.abs(nrm).argmax(axis=-1)
    big = hash3(q[:, 0] // 4, q[:, 1] // 4, q[:, 2] // 4, axis + 11)
    mid = hash3(q[:, 0] // 2, q[:, 1] // 2, q[:, 2] // 2, axis + 21)
    fine = hash3(q[:, 0], q[:, 1], q[:, 2], axis + 31)
    stone = BASE * (1 + 0.10 * big + 0.09 * mid + 0.08 * fine)
    stone = np.where(fine > 0.86, stone * 0.72, stone)   # crepe
    stone = np.where(fine < -0.9, stone * 1.28, stone)   # venature chiare
    inten = stone

    img_pid = pid.reshape(h, w)
    img_int = inten.reshape(h, w)
    # bordo sfumato verso l'interno: ogni zona e' piu' chiara sul contorno e scende a pietra scura dopo EDGE pixel
    EDGE = max(1, round(1.5 * scale))
    depth = np.zeros((h, w), np.int32)
    for i in range(len(PARTS)):
        m = img_pid == i
        cur = m.copy()
        for k in range(1, EDGE + 1):
            er = cur.copy()
            er[1:, :] &= cur[:-1, :]
            er[:-1, :] &= cur[1:, :]
            er[:, 1:] &= cur[:, :-1]
            er[:, :-1] &= cur[:, 1:]
            er[0, :] = er[-1, :] = False
            er[:, 0] = er[:, -1] = False
            depth[cur & ~er] = k - 1
            cur = er
        depth[cur] = EDGE
    edge = np.clip(1 - depth / EDGE, 0, 1) ** 1.4
    img_int = img_int * (1 + EDGE_GAIN * edge)

    out = {}
    for i, name in enumerate(PARTS):
        m = img_pid == i
        rgba = np.zeros((h, w, 4), np.uint8)
        v = (np.clip(img_int, 0, 1) * 255).astype(np.uint8)
        rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = np.where(m, v, 0)
        rgba[..., 3] = np.where(m, 255, 0)
        out[name] = Image.fromarray(rgba, "RGBA")
    return out


if __name__ == "__main__":
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "out")
    height = int(sys.argv[2]) if len(sys.argv) > 2 else 22
    os.makedirs(out_dir, exist_ok=True)
    for name, img in render(height).items():
        img.save(os.path.join(out_dir, f"{name}.png"))
    print("scritti", len(PARTS), "PNG in", out_dir)
