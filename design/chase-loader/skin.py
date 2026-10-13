"""Skin dipinta a 2x (256x128 texel) per il modello chase.bbmodel.

Lo spazio UV del progetto resta 128x64: ogni "pixel Minecraft" occupa 2x2 texel, quindi una faccia della
testa e' 16x16 texel invece di 8x8. Poliziotto: regione sinistra (u 0..63), fuggitivo: destra (u 64..127).
Le facce "east" (vista dalla camera laterale) hanno i dettagli piu' curati, la "west" ne e' lo specchio.
"""
import base64, io, os, random
from PIL import Image, ImageOps

HERE = os.path.dirname(os.path.abspath(__file__))
W, H = 128, 64          # spazio UV del progetto
SC = 2                  # texel per pixel Minecraft
TW, TH = W * SC, H * SC

# ---------------------------------------------------------------- palette
def c(r, g, b): return (r, g, b, 255)
SKIN, SKIN_L, SKIN_D, SKIN_DD = c(234, 180, 138), c(250, 206, 166), c(198, 140, 104), c(160, 106, 80)
BLUSH = c(226, 150, 124)
HAIR, HAIR_L, HAIR_D = c(98, 60, 40), c(130, 86, 58), c(62, 38, 28)
BROW = c(46, 28, 22)
UNI, UNI_L, UNI_D, UNI_DD = c(40, 76, 148), c(66, 108, 184), c(28, 54, 112), c(20, 38, 84)
PANTS, PANTS_L, PANTS_D = c(30, 58, 118), c(46, 80, 146), c(20, 38, 84)
GOLD, GOLD_L, GOLD_D = c(246, 198, 52), c(255, 226, 120), c(184, 134, 22)
BELT, BELT_L = c(26, 24, 32), c(54, 52, 66)
BOOT, BOOT_L, BOOT_D = c(78, 48, 36), c(116, 78, 58), c(44, 28, 22)
WHITE, WHITE_D = c(246, 246, 250), c(188, 190, 206)
TIE, TIE_L = c(20, 24, 46), c(40, 46, 80)
EYE_W, EYE_P, EYE_S = c(250, 250, 252), c(26, 28, 44), c(110, 140, 200)
MOUTH, MOUTH_D = c(132, 58, 54), c(88, 36, 38)
VISOR, VISOR_L = c(24, 24, 32), c(70, 70, 88)
BLACK, BLACK_L, BLACK_D = c(36, 36, 44), c(72, 72, 90), c(20, 20, 28)
STRIPE_W, STRIPE_WD = c(240, 240, 244), c(184, 186, 202)
GREY, GREY_L, GREY_D = c(176, 176, 186), c(206, 206, 216), c(126, 128, 142)
SHOE, SHOE_L = c(34, 34, 42), c(86, 86, 104)
SACK, SACK_L, SACK_D = c(172, 120, 64), c(206, 154, 88), c(116, 78, 42)
DUST, DUST_L, DUST_D = c(150, 128, 104), c(184, 162, 134), c(116, 98, 80)
BATON, BATON_L = c(40, 40, 50), c(98, 98, 116)


def rect(face, u, v, w, h, d):
    return {
        "up": (u + d, v, u + d + w, v + d),
        "down": (u + d + w, v, u + 2 * w + d, v + d),
        "east": (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),
        "west": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }[face]


skin = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))


# ---------------------------------------------------------------- strumenti di disegno (texel)
def new(w, h, col):
    return Image.new("RGBA", (w, h), col)


def P(im, x, y, col):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), col)


def R(im, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            P(im, x, y, col)


def PP(im, pts, col):
    for x, y in pts:
        P(im, x, y, col)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


def finish(im, kind="side"):
    """luce in alto, ombra in basso e sul lato destro: rilievo discreto senza toccare i dettagli."""
    w, h = im.size
    out = im.copy()
    for y in range(h):
        for x in range(w):
            p = im.getpixel((x, y))
            if p[3] == 0:
                continue
            t = 0.0
            if y == 0: t += 0.10
            if y >= h - 2: t -= 0.14 if y == h - 1 else 0.07
            if x == w - 1: t -= 0.10
            if x == 0: t += 0.05
            if t > 0:
                out.putpixel((x, y), mix(p, (255, 255, 255, 255), t))
            elif t < 0:
                out.putpixel((x, y), mix(p, (0, 0, 0, 255), -t))
    return out


def mirror(im):
    return ImageOps.mirror(im)


def put_box(ox, u, v, w, h, d, painter):
    for face in ("up", "down", "east", "north", "west", "south"):
        x0, y0, x1, y1 = rect(face, u, v, w, h, d)
        img = painter(face, (x1 - x0) * SC, (y1 - y0) * SC)
        skin.paste(img, (ox * SC + x0 * SC, y0 * SC))


# ================================================================ POLIZIOTTO
def hair_top(im):
    R(im, 0, 0, im.width - 1, im.height - 1, HAIR)
    for i in range(0, im.width, 3):
        R(im, i, 0, i, im.height - 1, HAIR_L)
    return im


def cop_head(face, fw, fh):
    if face == "up":
        return hair_top(new(fw, fh, HAIR))
    if face == "down":
        return new(fw, fh, SKIN_D)
    if face == "south":                                    # nuca
        im = new(fw, fh, HAIR); R(im, 0, 0, fw - 1, 1, HAIR_L); R(im, 3, fh - 3, fw - 4, fh - 1, SKIN_D)
        return im
    if face in ("east", "west"):
        e = new(16, 16, SKIN)
        R(e, 0, 0, 15, 3, HAIR)                            # sotto il berretto
        R(e, 0, 4, 6, 10, HAIR); R(e, 7, 4, 8, 6, HAIR); R(e, 5, 7, 8, 10, HAIR)   # nuca e basetta
        R(e, 0, 9, 6, 10, HAIR_D); R(e, 3, 5, 3, 8, HAIR_L)
        R(e, 8, 8, 9, 11, SKIN_D); P(e, 8, 9, SKIN_DD); P(e, 9, 10, SKIN_DD)         # orecchio
        R(e, 9, 10, 10, 11, BLUSH)
        PP(e, [(9, 4), (10, 4), (11, 4), (11, 5), (12, 5), (13, 5), (13, 6), (14, 6), (14, 7)], BROW)   # sopracciglio corrucciato
        PP(e, [(11, 6), (12, 6)], SKIN_D)                  # palpebra
        R(e, 11, 7, 12, 8, EYE_W); R(e, 13, 7, 13, 8, EYE_P); P(e, 13, 7, EYE_S); P(e, 12, 9, SKIN_D)
        PP(e, [(14, 9), (15, 9), (15, 10), (14, 10), (15, 11)], SKIN_D); P(e, 14, 8, SKIN_L)       # naso
        R(e, 11, 12, 14, 12, MOUTH_D); R(e, 12, 12, 13, 12, EYE_W); R(e, 11, 13, 14, 13, MOUTH)    # denti stretti
        R(e, 0, 15, 15, 15, SKIN_D); PP(e, [(13, 14), (14, 14)], SKIN_D)
        return e if face == "east" else mirror(e)
    n = new(16, 16, SKIN)                                  # fronte
    R(n, 0, 0, 15, 3, HAIR); R(n, 0, 4, 1, 8, HAIR); R(n, 14, 4, 15, 8, HAIR); PP(n, [(2, 4), (13, 4), (3, 4), (12, 4)], HAIR)
    PP(n, [(2, 5), (3, 5), (4, 6), (5, 6), (6, 6), (7, 6)], BROW)
    PP(n, [(13, 5), (12, 5), (11, 6), (10, 6), (9, 6), (8, 6)], BROW)
    R(n, 3, 8, 5, 9, EYE_W); R(n, 5, 8, 5, 9, EYE_P); P(n, 5, 8, EYE_S)
    R(n, 10, 8, 12, 9, EYE_W); R(n, 10, 8, 10, 9, EYE_P); P(n, 10, 8, EYE_S)
    R(n, 7, 10, 8, 11, SKIN_D); R(n, 7, 11, 8, 11, SKIN_DD)
    R(n, 3, 11, 4, 12, BLUSH); R(n, 11, 11, 12, 12, BLUSH)
    R(n, 5, 13, 10, 13, EYE_W); R(n, 5, 14, 10, 14, MOUTH); R(n, 5, 13, 5, 13, MOUTH_D); R(n, 10, 13, 10, 13, MOUTH_D)
    R(n, 0, 15, 15, 15, SKIN_D)
    return n


def cop_cap_crown(face, fw, fh):
    if face == "up":
        im = new(fw, fh, UNI_L); R(im, 0, 0, fw - 1, 1, c(96, 134, 206)); R(im, fw // 2 - 1, fh // 2 - 1, fw // 2, fh // 2, UNI_D)
        return im
    if face == "down":
        return new(fw, fh, UNI_DD)
    im = new(fw, fh, UNI)
    R(im, 0, 0, fw - 1, 1, UNI_L)
    R(im, 0, fh - 3, fw - 1, fh - 1, BELT)                 # fascia nera
    R(im, 0, fh - 3, fw - 1, fh - 3, BELT_L)
    if face == "north":                                    # stemma
        cx = fw // 2
        R(im, cx - 2, 1, cx + 1, fh - 4, GOLD); R(im, cx - 1, 1, cx, fh - 5, GOLD_L); P(im, cx + 1, fh - 4, GOLD_D); P(im, cx - 2, fh - 4, GOLD_D)
    return im


def cop_cap_brim(face, fw, fh):
    im = new(fw, fh, VISOR)
    if face in ("up",):
        R(im, 0, 0, fw - 1, 0, VISOR_L); R(im, 2, 2, fw - 3, 3, c(44, 44, 58))
    return im


def cop_body(face, fw, fh):
    if face == "up":
        return new(fw, fh, UNI_L)
    if face == "down":
        return new(fw, fh, UNI_DD)
    im = new(fw, fh, UNI)
    # sfumatura verticale leggera
    for y in range(fh):
        t = y / fh * 0.18
        R(im, 0, y, fw - 1, y, mix(UNI, UNI_D, t))
    R(im, 0, fh - 5, fw - 1, fh - 3, BELT); R(im, 0, fh - 5, fw - 1, fh - 5, BELT_L)      # cintura
    R(im, 0, fh - 2, fw - 1, fh - 1, PANTS)
    if face == "north":
        R(im, 0, 0, fw - 1, 1, UNI_L)
        PP(im, [(5, 0), (6, 1), (7, 2), (8, 2), (9, 1), (10, 0), (6, 0), (9, 0)], WHITE)   # colletto a V
        R(im, 7, 2, 8, fh - 7, TIE); R(im, 7, 2, 7, fh - 7, TIE_L)                          # cravatta
        R(im, 10, 5, 12, 8, GOLD); R(im, 10, 5, 11, 6, GOLD_L); PP(im, [(12, 8), (11, 9)], GOLD_D); P(im, 10, 9, GOLD_D)  # placca
        R(im, 2, 6, 5, 9, UNI_D); R(im, 2, 6, 5, 6, UNI_L)                                   # taschino
        R(im, 7, fh - 5, 8, fh - 3, GOLD); P(im, 7, fh - 5, GOLD_L)                          # fibbia
        for y in (11, 14):
            P(im, 7, y, GOLD_D)
    if face in ("east", "west"):
        R(im, 0, 0, fw - 1, 1, UNI_L)
        R(im, 2, fh - 12, 5, fh - 6, BELT); R(im, 2, fh - 12, 5, fh - 12, BELT_L)           # fondina
    if face == "south":
        R(im, 0, 0, fw - 1, 1, UNI_L); R(im, 7, 2, 8, 12, UNI_D)
    return im


def cop_arm(face, fw, fh, near=True):
    if face == "up":
        return new(fw, fh, UNI_L)
    if face == "down":
        return new(fw, fh, SKIN)
    im = new(fw, fh, UNI)
    for y in range(fh):
        R(im, 0, y, fw - 1, y, mix(UNI, UNI_D, y / fh * 0.15))
    R(im, 0, 0, fw - 1, 1, UNI_L)
    R(im, 0, fh - 8, fw - 1, fh - 8, UNI_D)                # piega del gomito
    R(im, 0, fh - 7, fw - 1, fh - 6, WHITE); R(im, 0, fh - 6, fw - 1, fh - 6, WHITE_D)  # polsino
    R(im, 0, fh - 5, fw - 1, fh - 1, SKIN)                 # mano
    R(im, 0, fh - 3, fw - 1, fh - 3, SKIN_D)               # nocche
    R(im, 0, fh - 1, fw - 1, fh - 1, SKIN_D)
    if face in ("east", "west") and near:
        R(im, 1, 1, 6, 4, GOLD); R(im, 1, 1, 6, 1, GOLD_L); R(im, 1, 4, 6, 4, GOLD_D)      # spallina
    return im


def cop_leg(face, fw, fh):
    if face == "up":
        return new(fw, fh, PANTS_D)
    if face == "down":
        return new(fw, fh, BELT)
    im = new(fw, fh, PANTS)
    for y in range(fh):
        R(im, 0, y, fw - 1, y, mix(PANTS, PANTS_D, y / fh * 0.2))
    R(im, 0, 0, fw - 1, 1, PANTS_L)
    R(im, 0, fh - 8, fw - 1, fh - 8, PANTS_D)              # piega del ginocchio
    if face in ("east", "west"):
        R(im, 3, 2, 3, fh - 9, PANTS_L)                    # piega del pantalone
    R(im, 0, fh - 6, fw - 1, fh - 1, BOOT); R(im, 0, fh - 6, fw - 1, fh - 6, BOOT_L)    # stivale
    R(im, 0, fh - 2, fw - 1, fh - 1, BOOT_D); R(im, 0, fh - 1, fw - 1, fh - 1, BELT)
    R(im, 1, fh - 5, 1, fh - 3, BOOT_L)
    return im


def baton(face, fw, fh):
    im = new(fw, fh, BATON)
    R(im, 0, 0, fw - 1, 0, BATON_L)
    return im


# ================================================================ FUGGITIVO
def stripes(fw, fh, a, a_d, b, step=3, phase=0, lit=True):
    im = new(fw, fh, a)
    for y in range(fh):
        w = ((y + phase) // step) % 2 == 0
        R(im, 0, y, fw - 1, y, a if w else b)
    for y in range(fh):                                    # ombra dentro alle bande bianche, verso il basso
        if ((y + phase) // step) % 2 == 0 and (y + phase) % step == step - 1:
            R(im, 0, y, fw - 1, y, a_d)
    return im


def thief_head(face, fw, fh):
    if face == "up":
        im = new(fw, fh, HAIR)
        for i in range(0, fw, 3): R(im, i, 0, i, fh - 1, HAIR_L)
        return im
    if face == "down":
        return new(fw, fh, SKIN_D)
    if face == "south":
        im = new(fw, fh, HAIR); R(im, 0, 6, fw - 1, 7, BLACK); R(im, 3, fh - 3, fw - 4, fh - 1, SKIN_D); R(im, 0, 0, fw - 1, 1, HAIR_L)
        return im
    if face in ("east", "west"):
        e = new(16, 16, SKIN)
        R(e, 0, 0, 15, 4, HAIR); R(e, 0, 5, 5, 10, HAIR); R(e, 4, 4, 15, 4, HAIR_D)      # capelli
        PP(e, [(6, 5), (7, 5), (8, 5), (9, 5)], HAIR); R(e, 1, 1, 12, 1, HAIR_L); R(e, 4, 2, 4, 6, HAIR_L)
        R(e, 5, 6, 15, 9, BLACK); R(e, 5, 6, 15, 6, BLACK_L); R(e, 0, 7, 4, 8, BLACK)     # mascherina e laccio
        R(e, 11, 7, 13, 7, EYE_W); P(e, 13, 7, EYE_P); R(e, 11, 8, 12, 8, BLACK_D)         # feritoia dell'occhio
        PP(e, [(10, 4), (11, 4), (12, 5), (13, 5), (14, 5), (15, 5)], BROW)                 # sopracciglio
        R(e, 7, 10, 8, 12, SKIN_D); P(e, 7, 11, SKIN_DD)
        PP(e, [(15, 10), (14, 10), (15, 11)], SKIN_D)
        R(e, 10, 12, 15, 12, MOUTH_D); R(e, 11, 12, 14, 12, EYE_W); R(e, 10, 13, 15, 13, MOUTH)  # ghigno
        PP(e, [(8, 12), (9, 13), (10, 14), (12, 14), (7, 11), (9, 11), (13, 14)], SKIN_D)  # barba
        R(e, 0, 15, 15, 15, SKIN_D)
        return e if face == "east" else mirror(e)
    n = new(16, 16, SKIN)
    R(n, 0, 0, 15, 4, HAIR); PP(n, [(2, 5), (3, 5), (12, 5), (13, 5), (0, 5), (15, 5), (0, 6), (15, 6)], HAIR)
    R(n, 1, 1, 14, 1, HAIR_L); R(n, 0, 4, 15, 4, HAIR_D)
    R(n, 0, 6, 15, 9, BLACK); R(n, 0, 6, 15, 6, BLACK_L)
    R(n, 3, 8, 5, 8, EYE_W); P(n, 5, 8, EYE_P); R(n, 10, 8, 12, 8, EYE_W); P(n, 10, 8, EYE_P)
    PP(n, [(2, 5), (3, 5), (4, 5), (5, 5), (13, 5), (12, 5), (11, 5), (10, 5)], BROW)
    R(n, 7, 10, 8, 11, SKIN_D)
    R(n, 4, 13, 11, 13, EYE_W)
    for x in (5, 7, 9): R(n, x, 13, x, 13, MOUTH_D)
    R(n, 3, 12, 12, 12, MOUTH_D); R(n, 4, 14, 11, 14, MOUTH)
    PP(n, [(2, 11), (3, 14), (12, 14), (13, 11)], SKIN_D)
    R(n, 0, 15, 15, 15, SKIN_D)
    return n


def thief_body(face, fw, fh):
    if face == "up":
        return new(fw, fh, BLACK)
    if face == "down":
        return new(fw, fh, GREY_D)
    im = stripes(fw, fh, STRIPE_W, STRIPE_WD, BLACK, 3)
    R(im, 0, 0, fw - 1, 0, STRIPE_W)
    if face == "north":
        R(im, 5, 0, 10, 1, BLACK)                          # scollo
        PP(im, [(6, 2), (7, 2), (8, 2), (9, 2)], BLACK)
        R(im, 3, fh - 5, fw - 4, fh - 4, BLACK_D)
    if face in ("east", "west"):
        R(im, 0, fh - 2, fw - 1, fh - 1, BLACK)
    return im


def thief_arm(face, fw, fh, near=True):
    if face == "up":
        return new(fw, fh, STRIPE_W)
    if face == "down":
        return new(fw, fh, SKIN)
    im = stripes(fw, fh, STRIPE_W, STRIPE_WD, BLACK, 3)
    R(im, 0, 0, fw - 1, 0, STRIPE_W)
    R(im, 0, fh - 7, fw - 1, fh - 7, BLACK_D)              # gomito
    R(im, 0, fh - 6, fw - 1, fh - 1, SKIN); R(im, 0, fh - 3, fw - 1, fh - 3, SKIN_D); R(im, 0, fh - 1, fw - 1, fh - 1, SKIN_D)
    R(im, 0, fh - 7, fw - 1, fh - 7, BLACK)
    return im


def thief_leg(face, fw, fh):
    if face == "up":
        return new(fw, fh, GREY)
    if face == "down":
        return new(fw, fh, SHOE)
    im = stripes(fw, fh, GREY, GREY_D, BLACK, 3, 1)
    R(im, 0, 0, fw - 1, 0, GREY_L)
    R(im, 0, fh - 8, fw - 1, fh - 8, BLACK_D)              # ginocchio
    R(im, 0, fh - 5, fw - 1, fh - 1, SHOE); R(im, 0, fh - 5, fw - 1, fh - 5, SHOE_L)   # scarpe
    R(im, 0, fh - 1, fw - 1, fh - 1, BLACK_D); R(im, 1, fh - 4, 1, fh - 3, SHOE_L)
    return im


def sack(face, fw, fh):
    rnd = random.Random(7)
    im = new(fw, fh, SACK)
    for _ in range(fw * fh // 5):
        P(im, rnd.randrange(fw), rnd.randrange(fh), SACK_D if rnd.random() < 0.55 else SACK_L)   # trama di juta
    R(im, 0, 0, fw - 1, 1, SACK_D); R(im, 0, fh - 1, fw - 1, fh - 1, SACK_D); R(im, fw - 1, 0, fw - 1, fh - 1, SACK_D)
    if face in ("east", "west"):
        cx, cy = fw // 2, fh // 2
        e = im.copy()
        S_ = [(1, 0), (2, 0), (3, 0), (0, 1), (1, 1), (1, 2), (2, 2), (3, 3), (4, 3), (0, 4), (1, 4), (2, 4), (3, 4)]   # S
        for dx, dy in [(2, -2), (2, -1), (2, 0), (2, 1), (2, 2), (2, 3)]:
            pass
        ox, oy = cx - 3, cy - 4
        for x, y in S_:
            P(e, ox + 1 + x, oy + y + 1, GOLD_L)
        for y in range(-1, 6):
            P(e, ox + 3, oy + y + 1, GOLD)                   # barra verticale del dollaro
        return e if face == "east" else mirror(e)
    return im


def knot(face, fw, fh):
    return new(fw, fh, SACK_D)


def dust(face, fw, fh):
    im = new(fw, fh, DUST); R(im, 0, 0, fw - 1, 0, DUST_L); R(im, 0, fh - 1, fw - 1, fh - 1, DUST_D)
    return im


PARTS = {
    "cop": dict(ox=0, head=cop_head, body=cop_body, arm_n=lambda f, w, h: cop_arm(f, w, h, True),
                arm_f=lambda f, w, h: cop_arm(f, w, h, False), leg_n=cop_leg, leg_f=cop_leg),
    "thief": dict(ox=64, head=thief_head, body=thief_body, arm_n=lambda f, w, h: thief_arm(f, w, h, True),
                  arm_f=lambda f, w, h: thief_arm(f, w, h, False), leg_n=thief_leg, leg_f=thief_leg),
}
UV = {"head": (0, 0), "body": (16, 16), "arm_n": (40, 16), "leg_n": (0, 16),
      "arm_f": (32, 48), "leg_f": (16, 48)}
SIZE = {"head": (8, 8, 8), "body": (8, 12, 4), "arm_n": (4, 12, 4), "arm_f": (4, 12, 4),
        "leg_n": (4, 12, 4), "leg_f": (4, 12, 4)}

for who, cfg in PARTS.items():
    for part in UV:
        u, v = UV[part]
        w, h, d = SIZE[part]
        put_box(cfg["ox"], u, v, w, h, d, cfg[part])

put_box(0, 0, 32, 9, 3, 9, cop_cap_crown)      # corona del berretto
put_box(0, 36, 32, 9, 1, 4, cop_cap_brim)      # visiera
put_box(0, 0, 44, 1, 1, 7, baton)              # manganello
put_box(64, 0, 50, 4, 6, 4, sack)              # sacco
put_box(64, 40, 40, 2, 2, 2, knot)             # nodo
put_box(0, 56, 60, 2, 2, 2, dust)              # polvere poliziotto
put_box(64, 56, 60, 2, 2, 2, dust)             # polvere fuggitivo

skin.save(os.path.join(HERE, "chase.png"))
_buf = io.BytesIO()
skin.save(_buf, "PNG")
SKIN_B64 = base64.b64encode(_buf.getvalue()).decode()
