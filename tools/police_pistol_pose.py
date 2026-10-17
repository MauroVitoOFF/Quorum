"""Valutazione delle animazioni della pistola (riferimento per l'implementazione in gioco).

Contratto di riproduzione (formato "qrm.anim/1"):
  * ogni bone ha canali opzionali position / rotation / scale, ciascuno una lista di
    keyframe {t, v, ease}; "ease" descrive la curva del tratto che ARRIVA al keyframe
  * position e' in unita' Blockbench (16 = 1 blocco), rotation in gradi [x, y, z],
    scale e' moltiplicativa (0 nasconde il bone, 1 e' a riposo)
  * trasformazione del bone attorno al suo pivot:  T(pos) . T(pivot) . Rz . Ry . Rx . S . T(-pivot)
    (si applica prima X, poi Y, poi Z); i bone figli ereditano quella del padre
  * prima del primo keyframe il canale e' a riposo (0 / 0 / 1), dopo l'ultimo resta all'ultimo valore;
    canale assente = riposo
Assi: muso verso -Z, alto +Y, +X e' la destra del tiratore.
"""
import math

import numpy as np


# --------------------------------------------------------------------- easing
def _out_bounce(x):
    n1, d1 = 7.5625, 2.75
    if x < 1 / d1:
        return n1 * x * x
    if x < 2 / d1:
        x -= 1.5 / d1
        return n1 * x * x + 0.75
    if x < 2.5 / d1:
        x -= 2.25 / d1
        return n1 * x * x + 0.9375
    x -= 2.625 / d1
    return n1 * x * x + 0.984375


def _out_elastic(x):
    if x in (0, 1):
        return x
    c4 = (2 * math.pi) / 3
    return 2 ** (-10 * x) * math.sin((x * 10 - 0.75) * c4) + 1


C1, C3 = 1.70158, 2.70158
EASE = {
    "linear": lambda x: x,
    "step": lambda x: 0.0 if x < 1 else 1.0,
    "easeInQuad": lambda x: x * x,
    "easeOutQuad": lambda x: 1 - (1 - x) ** 2,
    "easeInOutQuad": lambda x: 2 * x * x if x < 0.5 else 1 - (-2 * x + 2) ** 2 / 2,
    "easeInCubic": lambda x: x ** 3,
    "easeOutCubic": lambda x: 1 - (1 - x) ** 3,
    "easeInOutCubic": lambda x: 4 * x ** 3 if x < 0.5 else 1 - (-2 * x + 2) ** 3 / 2,
    "easeOutExpo": lambda x: 1 if x == 1 else 1 - 2 ** (-10 * x),
    "easeInBack": lambda x: C3 * x ** 3 - C1 * x ** 2,
    "easeOutBack": lambda x: 1 + C3 * (x - 1) ** 3 + C1 * (x - 1) ** 2,
    "easeOutElastic": _out_elastic,
    "easeOutBounce": _out_bounce,
}


def channel(keys, default):
    """Valuta una lista di keyframe [{t, v, ease}] al tempo dato (restituisce una funzione)."""
    keys = sorted(keys, key=lambda k: k["t"])

    def at(t):
        if not keys:
            return np.array(default, float)
        if t < keys[0]["t"]:
            return np.array(default, float)      # prima del primo keyframe: posa di riposo
        if t == keys[0]["t"]:
            return np.array(keys[0]["v"], float)
        for a, b in zip(keys, keys[1:]):
            if t <= b["t"]:
                span = b["t"] - a["t"]
                x = 0.0 if span <= 0 else (t - a["t"]) / span
                e = EASE[b.get("ease", "linear")](x)
                return np.array(a["v"], float) + (np.array(b["v"], float) - np.array(a["v"], float)) * e
        return np.array(keys[-1]["v"], float)
    return at


def bone_pose(anim, bone, t):
    ch = anim.get("bones", {}).get(bone, {})
    pos = channel(ch.get("position", []), [0, 0, 0])(t)
    rot = channel(ch.get("rotation", []), [0, 0, 0])(t)
    scl = channel(ch.get("scale", []), [1, 1, 1])(t)
    return pos, rot, scl


# ------------------------------------------------------------------- matrici
def translate(v):
    m = np.eye(4)
    m[:3, 3] = v
    return m


def rot_xyz(deg):
    ax, ay, az = (math.radians(a) for a in deg)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    rx = np.array([[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]])
    ry = np.array([[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]])
    rz = np.array([[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])
    return rz @ ry @ rx


def bone_matrix(pos, rot, scl, pivot):
    return translate(pos) @ translate(pivot) @ rot_xyz(rot) @ np.diag([*scl, 1.0]) @ translate(-np.array(pivot, float))


def world_matrices(rig, anim, t):
    """Matrice mondo di ogni bone al tempo t (anim puo' essere None = posa di riposo)."""
    bones = rig["bones"]
    cache = {}

    def get(name):
        if name in cache:
            return cache[name]
        b = bones[name]
        pos, rot, scl = bone_pose(anim, name, t) if anim else (np.zeros(3), np.zeros(3), np.ones(3))
        m = bone_matrix(pos, rot, scl, b["pivot"])
        if b["parent"]:
            m = get(b["parent"]) @ m
        cache[name] = m
        return m

    return {n: get(n) for n in bones}
