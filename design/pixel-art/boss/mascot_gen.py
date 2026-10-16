"""Mascotte "Boss" chibi animata del menu aziendale (stile reference: testa grande, saluta, prop accanto).

Uso (da qualsiasi cartella): python -I mascot_gen.py
Scrive:
  qrm-boss/.../textures/gui/boss_mascot.png   spritesheet verticale, FRAMES frame da FW x FH
  design/pixel-art/boss/mascot_anim.gif              anteprima animata (x8)
  design/pixel-art/boss/mascot_big.png               tutti i frame affiancati (x4)
  design/pixel-art/boss/mockup_mascot.png            menu Conto con la mascotte sul bordo della cornice
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "qrm-boss/src/main/resources/assets/qrm_boss/textures/gui"
HERE = Path(__file__).resolve().parent

FW, FH, FRAMES = 32, 40, 8
FRAME_MS = 140

P = {
    "K": "#1a0c08",
    "sk": "#f8c9a0", "skh": "#ffe2c6", "sks": "#dfa27c", "skd": "#bb7e5c", "blush": "#f09a86",
    "hr": "#6a4222", "hrh": "#8c5c34", "hrd": "#3e2410",
    "su": "#2e3560", "suh": "#434d8a", "sud": "#1c2146", "lap": "#5964a8",
    "pa": "#2a2f50", "pah": "#3a4072", "pad": "#181c34",
    "sh": "#f8f4ea", "shd": "#c9c3b4",
    "ti": "#e0402a", "tih": "#ff7a56", "tid": "#932012",
    "au": "#ffc83a", "auh": "#fff2a8", "aud": "#c27a10",
    "gr": "#e0342a", "gb": "#4a9af0",
    "le": "#a05e32", "leh": "#c68448", "led": "#6e3b1c", "lex": "#47240f",
    "so": "#5a3422", "soh": "#8e5a3a", "sod": "#2c160c",
    "ey": "#ffffff", "iris": "#5a50d8", "irish": "#a8a0ff", "pup": "#1c1860",
    "mo": "#9a3a2a",
}


def rgba(c, a=255):
    c = P.get(c, c)
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (a,)


class Layer:
    def __init__(self):
        self.img = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))

    def px(self, x, y, c, a=255):
        if 0 <= x < FW and 0 <= y < FH:
            self.img.putpixel((x, y), rgba(c, a))

    def rect(self, x, y, w, h, c):
        for j in range(h):
            for i in range(w):
                self.px(x + i, y + j, c)

    def shape(self, pts, base, hi, dark):
        s = set(pts)
        for (x, y) in s:
            if (x - 1, y) not in s or (x, y - 1) not in s:
                c = hi
            elif (x + 1, y) not in s or (x, y + 1) not in s:
                c = dark
            else:
                c = base
            self.px(x, y, c)

    def outline(self, c="K"):
        src = self.img.copy()
        for y in range(FH):
            for x in range(FW):
                if src.getpixel((x, y))[3]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < FW and 0 <= ny < FH and src.getpixel((nx, ny))[3] == 255:
                        self.img.putpixel((x, y), rgba(c))
                        break


def box(x, y, w, h):
    return [(x + i, y + j) for j in range(h) for i in range(w)]


def line(p0, p1, w):
    (x0, y0), (x1, y1) = p0, p1
    n = max(abs(x1 - x0), abs(y1 - y0), 1) * 2
    pts = set()
    for k in range(n + 1):
        x = round(x0 + (x1 - x0) * k / n)
        y = round(y0 + (y1 - y0) * k / n)
        pts.update(box(x - w // 2, y - w // 2, w, w))
    return pts


def frame(i):
    b = [1, 0, 0, 0, 0, 0, 0, 1][i]                 # accovacciamento quando riprende la moneta
    wave = [-1, 0, 1, 2, 1, 0, -1, -2][i]            # oscillazione della mano che saluta
    lift = [0, 0, -1, -1, -1, 0, 0, 0][i]            # il braccio si alza nel picco del saluto
    ox = [0, 0, 0, 1, 1, 0, -1, -1][i]               # la testa ondeggia
    blink = i == 6
    cheer = i in (2, 3, 4)                           # bocca aperta
    tap = i in (2, 3)                                # il piede batte il tempo
    spin = [4, 3, 2, 3, 4, 3, 2, 3][i]               # larghezza della moneta che gira
    toss = [0, 3, 6, 8, 9, 8, 6, 3][i]               # altezza della moneta
    L = Layer()

    # gambe: una appoggiata, l'altra batte il tempo
    L.shape(box(12, 28 + b, 3, 7 - b), "pa", "pah", "pad")
    L.shape(box(17, 28 + b, 3, 7 - b - (1 if tap else 0)), "pa", "pah", "pad")
    L.shape(box(11, 35, 5, 3), "so", "soh", "sod")
    L.shape(box(16, 34 if tap else 35, 5, 3), "so", "soh", "sod")

    # braccio destro teso in avanti, con il palmo sotto la moneta
    L.shape(line((20, 22 + b), (25, 25 + b), 3), "su", "suh", "sud")
    L.rect(23, 24 + b, 1, 3, "sh")
    L.shape(box(25, 24 + b, 4, 3), "sk", "skh", "sks")
    L.px(25, 23 + b, "sks")
    cx = 27
    cy = 20 + b - toss
    x0 = cx - spin // 2
    L.rect(x0, cy, spin, 4, "au")
    L.rect(x0, cy, 1, 4, "auh"); L.rect(x0 + spin - 1, cy, 1, 4, "aud")
    if spin >= 3:
        L.px(cx, cy + 1, "aud"); L.px(cx, cy + 2, "aud")

    # busto che si comprime e si allunga
    L.shape(box(11, 19 + b, 10, 9 - b), "su", "suh", "sud")
    for k, (bx, bw) in enumerate([(14, 4), (14, 4), (15, 2)]):
        if k < 9 - b - 1:
            L.rect(bx, 19 + b + k, bw, 1, "sh")
    L.px(13, 19 + b, "lap"); L.px(13, 20 + b, "lap"); L.px(18, 19 + b, "lap"); L.px(18, 20 + b, "lap")
    sway = 1 if wave > 0 else 0
    L.rect(15, 20 + b, 2, 4, "ti"); L.px(15, 20 + b, "tih"); L.rect(16, 21 + b, 1, 3, "tid")
    L.px(15 + sway, 24 + b, "ti"); L.px(16 + sway, 24 + b, "tid")
    L.px(19, 23 + b, "sh"); L.px(19, 22 + b, "gr")
    L.rect(11, 27, 10, 1, "sud"); L.rect(15, 27, 2, 1, "au")

    # braccio sinistro: saluto ampio
    hx, hy = 5 + wave, 13 + b + lift
    L.shape(line((11, 22 + b), (hx + 1, hy + 5), 3), "su", "suh", "sud")
    L.rect(hx, hy + 4, 3, 1, "sh")
    L.shape(box(hx - 1, hy + 1, 3, 3), "sk", "skh", "sks")
    L.px(hx - 1, hy, "sks"); L.px(hx + 1, hy, "sk"); L.px(hx - 2, hy + 2, "sk")

    # testa
    h0 = 9 + b
    hxo = 8 + ox
    L.shape(box(10 + ox, h0 + 2, 12, 8), "sk", "skh", "sks")
    L.rect(10 + ox, h0 + 9, 12, 1, "skd")
    L.rect(hxo, h0, 16, 3, "hr"); L.rect(9 + ox, h0 + 1, 8, 1, "hrh")
    L.rect(9 + ox, h0 + 3, 2, 5, "hr"); L.rect(21 + ox, h0 + 3, 3, 7, "hrd"); L.rect(22 + ox, h0 + 3, 1, 5, "hr")
    L.px(22 + ox + sway, h0 + 10, "hrd")
    for x in (10, 11, 12, 13, 15, 16, 17, 19, 20):
        L.px(x + ox, h0 + 3, "hr")
    L.px(14 + ox, h0 + 3, "hrd"); L.px(18 + ox, h0 + 3, "hrd")
    L.px(11 + ox, h0 + 4, "hr"); L.px(20 + ox, h0 + 4, "hrd")
    for ex, wx in ((12, 11), (18, 20)):
        if blink:
            L.rect(min(ex, wx) + ox, h0 + 6, 3, 1, "hrd")
        else:
            L.rect(ex + ox, h0 + 5, 2, 3, "iris"); L.px(ex + ox, h0 + 5, "irish")
            L.px(ex + 1 + ox, h0 + 7, "pup")
            L.rect(wx + ox, h0 + 5, 1, 3, "ey")
            L.rect(min(ex, wx) + ox, h0 + 4, 3, 1, "hrd")
    L.px(11 + ox, h0 + 8, "blush"); L.px(20 + ox, h0 + 8, "blush")
    if cheer:
        L.rect(14 + ox, h0 + 8, 4, 1, "mo"); L.rect(15 + ox, h0 + 9, 2, 1, "mo"); L.px(15 + ox, h0 + 9, "#f07070")
    else:
        L.px(15 + ox, h0 + 8, "mo"); L.px(16 + ox, h0 + 8, "mo")

    # corona
    cyy = h0 - 3
    L.shape(box(9 + ox, cyy + 1, 14, 3), "au", "auh", "aud")
    for sx in (9, 13, 17, 21):
        tall = 3 if sx in (13, 17) else 2
        L.shape(box(sx + ox, cyy + 1 - tall, 2, tall), "au", "auh", "aud")
        L.px(sx + ox, cyy + 1 - tall, "auh")
    L.rect(15 + ox, cyy + 2, 2, 1, "gr")
    L.px(11 + ox, cyy + 2, "gb"); L.px(20 + ox, cyy + 2, "gb")

    L.outline("K")
    out = L.img
    # scintille sulla moneta al culmine
    if toss >= 8:
        out.putpixel((cx, cy - 2), rgba("ey"))
        out.putpixel((cx - 3, cy + 1), rgba("auh"))
        if toss == 9:
            out.putpixel((cx + 3, cy), rgba("ey")); out.putpixel((cx, cy - 3), rgba("auh"))
    return out


def sheet():
    s = Image.new("RGBA", (FW, FH * FRAMES), (0, 0, 0, 0))
    for i in range(FRAMES):
        s.alpha_composite(frame(i), (0, i * FH))
    return s


def nine(dst, src, b, x, y, w, h):
    sw, sh = src.size
    xs, ys = [0, b, sw - b, sw], [0, b, sh - b, sh]
    dx, dy = [x, x + b, x + w - b, x + w], [y, y + b, y + h - b, y + h]
    for i in range(3):
        for j in range(3):
            box_ = (xs[i], ys[j], xs[i + 1], ys[j + 1])
            tw, th = dx[i + 1] - dx[i], dy[j + 1] - dy[j]
            if tw <= 0 or th <= 0 or box_[2] <= box_[0] or box_[3] <= box_[1]:
                continue
            dst.alpha_composite(src.crop(box_).resize((tw, th), Image.NEAREST), (dx[i], dy[j]))


def load(name):
    return Image.open(TEX / f"{name}.png").convert("RGBA")


def mockup(m):
    OY = 34
    W, H = 200, 134 + OY
    im = Image.new("RGBA", (W, H), rgba("#2a1d17"))
    nine(im, load("frame"), 8, 26, 12 + OY, 156, 114)
    nine(im, load("title_plate"), 6, 64, 4 + OY, 80, 16)
    for i, (k, ic) in enumerate([("tab", "overview"), ("tab", "members"), ("tab_selected", "account")]):
        im.alpha_composite(load(k), (-2 if i == 2 else 2, 34 + i * 24 + OY))
        im.alpha_composite(load("icon_" + ic), (4 if i == 2 else 8, 36 + i * 24 + OY))
    nine(im, load("balance_plate"), 6, 40, 34 + OY, 130, 24)
    im.alpha_composite(load("icon_account"), (44, 38 + OY))
    for i in range(4):
        nine(im, load("button_confirm_normal"), 4, 40 + i * 33, 70 + OY, 31, 14)
        nine(im, load("button_danger_normal"), 4, 40 + i * 33, 90 + OY, 31, 14)
    # in piedi sul bordo superiore della cornice, a destra
    im.alpha_composite(m, (146, 12 + OY + 2 - 38))
    big = im.resize((W * 5, H * 5), Image.NEAREST)
    d = ImageDraw.Draw(big)
    f = ImageFont.load_default()
    d.text((120 * 5, (41 + OY) * 5), "12.500 $", fill=rgba("auh"), font=f)
    d.text((42 * 5, (24 + OY) * 5), "Conto aziendale", fill=rgba("led"), font=f)
    return big


if __name__ == "__main__":
    TEX.mkdir(parents=True, exist_ok=True)
    s = sheet()
    s.save(TEX / "boss_mascot.png")
    frames = [s.crop((0, i * FH, FW, (i + 1) * FH)) for i in range(FRAMES)]
    bg = Image.new("RGBA", (FW, FH), rgba("#6a5a50"))
    gif = []
    for fr in frames:
        c = bg.copy(); c.alpha_composite(fr)
        gif.append(c.resize((FW * 8, FH * 8), Image.NEAREST).convert("P", palette=Image.ADAPTIVE))
    gif[0].save(HERE / "mascot_anim.gif", save_all=True, append_images=gif[1:], duration=FRAME_MS, loop=0)
    strip = Image.new("RGBA", (FW * FRAMES, FH), rgba("#6a5a50"))
    for i, fr in enumerate(frames):
        strip.alpha_composite(fr, (i * FW, 0))
    strip.resize((strip.size[0] * 4, strip.size[1] * 4), Image.NEAREST).save(HERE / "mascot_big.png")
    mockup(frames[3]).save(HERE / "mockup_mascot.png")
    print("sheet", s.size, "frames", FRAMES)
