from PIL import Image
import math, sys
W = H = 32
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
P = img.load()
def c(h): return tuple(int(h[i:i+2], 16) for i in (1, 3, 5)) + (255,)
K  = c("#2a1414"); G1 = c("#5c5d62"); G2 = c("#464a52"); GL = c("#9a9ca4"); GM = c("#7c7e86")
WH = c("#f4f6fa"); SH = c("#b8bcc8"); HD = c("#3a3e4a")
BR = c("#7a4430"); BG = c("#f0d8b0"); BS = c("#dcc08e"); TX = c("#2e3040"); RD = c("#d03030")
def px(x, y, col):
    if 0 <= x < W and 0 <= y < H: P[x, y] = col
def rect(x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): px(x, y, col)

# box: outline, front, side, bevels
rect(7, 10, 24, 27, K)
rect(8, 11, 21, 26, G1)
rect(22, 11, 23, 26, G2)
rect(8, 11, 23, 11, GL); rect(8, 12, 21, 12, GM); rect(22, 12, 23, 12, GM)
px(8, 26, GL); px(9, 26, GL); px(8, 25, GL); rect(10, 26, 21, 26, G2)
rect(23, 11, 23, 11, WH)
rect(9, 9, 10, 9, K); rect(20, 9, 21, 9, K)  # slot shadow

# clock
cx, cy = 14.5, 19.5
for y in range(13, 27):
    for x in range(8, 22):
        d = (x - cx) ** 2 + (y - cy) ** 2
        if d <= 25:
            px(x, y, SH if (x - cx < -2 and y - cy > 2) else WH)
        elif d <= 31: px(x, y, K)
for x, y in [(14,15),(15,15),(14,24),(15,24),(10,19),(10,20),(19,19),(19,20)]: px(x, y, HD)
for x, y in [(15,17),(15,18),(15,19),(15,20),(16,21),(17,22)]: px(x, y, HD)
px(14, 20, SH)

# paper (cols 11..19, rows 3..10)
rows = [
 "BBBBBBB..",
 "BfffffsB.",
 "BfdddfssB",
 "BfffffffB",
 "BdfdddfrB",
 "BfffffffB",
 "BdddddrrB",
 "BfffffffB",
]
cm = {"B": BR, "f": BG, "s": BS, "d": TX, "r": RD}
for j, row in enumerate(rows):
    for i, ch in enumerate(row):
        if ch in cm: px(11 + i, 3 + j, cm[ch])

def arrow(x0, y0, rows, cm):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in cm: px(x0 + i, y0 + j, cm[ch])
# green down arrow (cols 1..5)
GK = c("#1d5a3a"); GF = c("#5cbf4a"); GH = c("#a6e85a")
arrow(1, 17, [
 ".KKK.",
 ".KhK.",
 ".KfK.",
 "KKfKK",
 "KfffK",
 ".KfK.",
 "..K..",
], {"K": GK, "f": GF, "h": GH})
# red up arrow (cols 26..30)
RK = c("#5a1018"); RF = c("#d03a3a"); RH = c("#f06a5a")
arrow(26, 12, [
 "..K..",
 ".KhK.",
 "KhffK",
 "KKfKK",
 ".KfK.",
 ".KfK.",
 ".KKK.",
], {"K": RK, "f": RF, "h": RH})

img.save("timeclock_32.png")
img.resize((W * 16, H * 16), Image.NEAREST).save("timeclock_preview.png")
