#!/usr/bin/env python3
"""Genera le animazioni della pistola d'ordinanza (formato "qrm.anim/1").

Scrive assets/qrm/animations/police_pistol.json. Il contratto di riproduzione (unita',
ordine delle rotazioni, easing, pivot) e' descritto in police_pistol_pose.py; le anteprime
si producono con police_pistol_preview.py.

Uso (dalla radice del repo): python tools/police_pistol_anim.py
"""
import json
import math
from pathlib import Path

import police_pistol_pose as pose

ASSETS = Path("qrm-neoforge/src/main/resources/assets/qrm")
OUT = ASSETS / "animations/police_pistol.json"

SLIDE_BACK = 2.5           # corsa del carrello
CH = [0, 1.75, 1.0]        # bossolo a riposo -> posizione nella camera di scoppio
ROUND_FEED = [0, -1.6, 1.3]  # cartuccia in cima al caricatore, relativa alla camera


def mag(d):
    """Spostamento del caricatore di d unita' lungo l'asse dell'impugnatura (d>0 = verso il basso)."""
    return [0, round(-0.966 * d, 3), round(0.259 * d, 3)]


def add3(a, b):
    return [round(x + y, 3) for x, y in zip(a, b)]


class Anim:
    def __init__(self, length, loop=False, hold_last=False, note=""):
        self.length, self.loop, self.hold_last, self.note = length, loop, hold_last, note
        self.bones, self.events = {}, []

    def key(self, bone, ch, t, v, ease="linear"):
        self.bones.setdefault(bone, {}).setdefault(ch, []).append({"t": round(t, 4), "v": list(v), "ease": ease})

    def pos(self, bone, t, v, ease="linear"):
        self.key(bone, "position", t, v, ease)

    def rot(self, bone, t, v, ease="linear"):
        self.key(bone, "rotation", t, v, ease)

    def scl(self, bone, t, v, ease="linear"):
        self.key(bone, "scale", t, v, ease)

    def hide(self, bone, t):
        self.scl(bone, t, [0, 0, 0], "step")

    def show(self, bone, t):
        self.scl(bone, t, [1, 1, 1], "step")

    def ev(self, t, type_, **kw):
        self.events.append({"t": round(t, 4), "type": type_, **kw})

    def derive_port_cover(self):
        """Il tappo della finestra segue il carrello: aperto (scala X = 0) se e' arretrato oltre 0.3."""
        sl = self.bones.get("slide", {}).get("position")
        if not sl:
            return
        fn = pose.channel(sorted(sl, key=lambda k: k["t"]), [0, 0, 0])
        steps = int(self.length * 120) + 1
        state, keys = None, []
        for i in range(steps + 1):
            t = min(self.length, i / 120)
            open_ = fn(t)[2] > 0.3
            if state is None:
                state = open_
                keys.append((0.0, state))
            elif open_ != state:
                keys.append((round(t, 4), open_))
                state = open_
        for t, o in keys:
            v = [0, 1, 1] if o else [1, 1, 1]
            if t > 0:
                prev = [1, 1, 1] if o else [0, 1, 1]
                self.key("port_cover", "scale", max(0.0, t - 0.015), prev, "linear")
            self.key("port_cover", "scale", t, v, "easeOutQuad")

    def done(self):
        self.derive_port_cover()
        for b in self.bones.values():
            for ch in b.values():
                ch.sort(key=lambda k: k["t"])
        self.events.sort(key=lambda e: e["t"])
        d = {"length": self.length, "loop": self.loop}
        if self.hold_last:
            d["hold_last"] = True
        if self.note:
            d["note"] = self.note
        d["events"] = self.events
        d["bones"] = self.bones
        return d


# ------------------------------------------------------------------- blocchi
def fly(a, bone, t0, p0, r0, scale_end=True):
    """Oggetto espulso dalla finestra: arco, rotazione e sparizione."""
    x, y, z = p0
    a.pos(bone, t0, p0)
    a.pos(bone, t0 + 0.05, [x + 1.6, y + 0.9, z + 0.4], "easeOutQuad")
    a.pos(bone, t0 + 0.17, [x + 4.2, y + 3.0, z + 1.2], "easeOutQuad")
    a.pos(bone, t0 + 0.30, [x + 6.5, y + 0.2, z + 1.9], "easeInQuad")
    a.pos(bone, t0 + 0.50, [x + 8.5, y - 8.0, z + 2.4], "easeInQuad")
    a.rot(bone, t0, r0)
    a.rot(bone, t0 + 0.17, add3(r0, [100, 30, 380]))
    a.rot(bone, t0 + 0.50, add3(r0, [250, 50, 800]))
    if scale_end:
        a.hide(bone, t0 + 0.50)


def feed_round(a, t0, dur=0.05):
    """Cartuccia nuova: esce dal caricatore e sale in camera (con il carrello che chiude)."""
    a.hide("chambered_round", t0 - 0.001)
    a.show("chambered_round", t0)
    a.pos("chambered_round", t0, ROUND_FEED)
    a.rot("chambered_round", t0, [28, 0, 0])
    a.pos("chambered_round", t0 + dur, [0, 0, 0], "easeOutQuad")
    a.rot("chambered_round", t0 + dur, [0, 0, 0], "easeOutQuad")


def slide_close(a, t0, from_z, snap=0.06):
    """Carrello che scatta in avanti con piccolo rimbalzo a fine corsa."""
    a.pos("slide", t0, [0, 0, from_z])
    a.pos("slide", t0 + snap, [0, 0, 0], "easeInCubic")
    a.pos("slide", t0 + snap + 0.03, [0, 0, 0.14], "easeOutQuad")
    a.pos("slide", t0 + snap + 0.09, [0, 0, 0], "easeOutCubic")
    a.rot("slide", t0, [0, 0, 0])
    a.rot("slide", t0 + snap, [-0.8, 0, 0], "easeInQuad")
    a.rot("slide", t0 + snap + 0.09, [0, 0, 0], "easeOutBack")
    a.pos("barrel", t0, [0, 0, 0])
    a.rot("barrel", t0, [0, 0, 0])


def tap_mag(a, t0):
    """Colpetto sul fondo del caricatore ("tap")."""
    a.pos("root", t0, [0, 0, 0])
    a.pos("root", t0 + 0.05, [0, -0.35, 0.05], "easeOutQuad")
    a.pos("root", t0 + 0.14, [0, 0, 0], "easeOutCubic")
    a.pos("magazine", t0, [0, 0, 0])
    a.pos("magazine", t0 + 0.05, mag(-0.35), "easeOutQuad")
    a.pos("magazine", t0 + 0.16, [0, 0, 0], "easeOutBack")
    a.ev(t0, "arm", pose="support_hand_tap_magazine")
    a.ev(t0 + 0.05, "sound", id="pistol.mag_tap")


def shot(a, ts, end="normal"):
    """Un colpo al tempo ts. end: normal | lock | stovepipe | feed | double."""
    # grilletto
    a.rot("trigger", ts - 0.07, [0, 0, 0])
    a.rot("trigger", ts, [-13, 0, 0], "easeInQuad")
    a.rot("trigger", ts + 0.11, [-13, 0, 0])
    a.rot("trigger", ts + 0.24, [0, 0, 0], "easeOutCubic")
    a.pos("trigger_bar", ts - 0.07, [0, 0, 0])
    a.pos("trigger_bar", ts, [0, 0, 0.45], "easeInQuad")
    a.pos("trigger_bar", ts + 0.11, [0, 0, 0.45])
    a.pos("trigger_bar", ts + 0.24, [0, 0, 0], "easeOutCubic")
    a.rot("trigger_safety", ts - 0.07, [0, 0, 0])
    a.rot("trigger_safety", ts - 0.03, [-5, 0, 0], "easeOutQuad")
    a.rot("trigger_safety", ts + 0.22, [0, 0, 0], "easeOutCubic")
    # braccio e rinculo
    a.pos("root", ts, [0, 0, 0])
    a.pos("root", ts + 0.025, [0, -0.12, 0.95], "easeOutQuad")
    a.pos("root", ts + 0.14, [0, 0.05, 0.25], "easeOutCubic")
    a.pos("root", ts + 0.30, [0, 0, 0], "easeOutBack")
    a.rot("root", ts, [0, 0, 0])
    a.rot("root", ts + 0.025, [9, 0.7, -0.9], "easeOutQuad")
    a.rot("root", ts + 0.14, [1.8, 0, 0.3], "easeOutCubic")
    a.rot("root", ts + 0.30, [0, 0, 0], "easeOutBack")
    # canna e caricatore
    a.pos("barrel", ts, [0, 0, 0])
    a.pos("barrel", ts + 0.025, [0, 0, 0.5], "easeOutQuad")
    a.pos("barrel", ts + 0.045, [0, 0, 0.5])
    a.rot("barrel", ts, [0, 0, 0])
    a.rot("barrel", ts + 0.03, [5, 0, 0], "easeOutQuad")
    a.rot("barrel", ts + 0.05, [5, 0, 0])
    a.pos("magazine", ts, [0, 0, 0])
    a.pos("magazine", ts + 0.03, [0, -0.1, 0.05], "easeOutQuad")
    a.pos("magazine", ts + 0.18, [0, 0, 0], "easeOutBack")
    # cartuccia (scompare allo sparo)
    a.show("chambered_round", ts - 0.001)
    a.hide("chambered_round", ts)
    # carrello indietro
    a.pos("slide", ts, [0, 0, 0])
    a.pos("slide", ts + 0.035, [0, 0, SLIDE_BACK], "easeOutQuad")
    a.rot("slide", ts, [0, 0, 0])
    a.rot("slide", ts + 0.035, [1.2, 0, 0], "easeOutQuad")
    a.ev(ts - 0.07, "arm", pose="trigger_squeeze")
    a.ev(ts, "sound", id="pistol.fire")
    a.ev(ts, "muzzle_flash", bone="barrel", offset=[8, 9.8, 1.5], size=1.0)
    a.ev(ts, "camera_kick", pitch=1.4, yaw=0.25)

    # bossolo: compare in camera allo sparo
    a.pos("ejected_casing", ts - 0.001, [0, 0, 0])
    a.pos("ejected_casing", ts, CH, "step")
    a.pos("ejected_casing", ts + 0.035, [0, 1.75, 1.8], "easeOutQuad")

    if end in ("normal", "lock"):
        fly(a, "ejected_casing", ts + 0.05, [0, 1.75, 1.8], [0, 0, 0])
        a.rot("ejected_casing", ts + 0.035, [0, 0, 0])
        a.ev(ts + 0.05, "eject_shell", bone="slide", offset=[9.3, 10.0, 9.8], velocity=[2.2, 1.6, 0.8])
        a.ev(ts + 0.2, "sound", id="pistol.shell_drop")
    if end == "normal":
        slide_close(a, ts + 0.12, SLIDE_BACK)
        a.pos("slide", ts + 0.035, [0, 0, SLIDE_BACK])
        a.pos("slide", ts + 0.12, [0, 0, SLIDE_BACK])
        feed_round(a, ts + 0.15)
        a.ev(ts + 0.18, "sound", id="pistol.slide_forward")
    if end == "lock":
        a.pos("slide", ts + 0.12, [0, 0, SLIDE_BACK])
        a.pos("slide", ts + 0.4, [0, 0, SLIDE_BACK])
        a.ev(ts + 0.1, "sound", id="pistol.slide_lock")
    if end == "stovepipe":
        a.rot("ejected_casing", ts + 0.035, [0, 0, 0])
        a.pos("ejected_casing", ts + 0.14, [0.9, 2.4, 0.9], "easeInQuad")
        a.rot("ejected_casing", ts + 0.14, [90, 0, 0], "easeInQuad")
        a.pos("ejected_casing", ts + 0.22, [0.9, 2.2, 0.8], "easeOutBack")
        a.pos("slide", ts + 0.035, [0, 0, SLIDE_BACK])
        a.pos("slide", ts + 0.11, [0, 0, SLIDE_BACK])
        a.pos("slide", ts + 0.15, [0, 0, 0.55], "easeInQuad")
        a.pos("slide", ts + 0.19, [0, 0, 0.95], "easeOutQuad")
        a.pos("slide", ts + 0.26, [0, 0, 0.55], "easeInQuad")
        a.rot("root", ts + 0.15, [1.8, 0, 0.3])
        a.rot("root", ts + 0.17, [3.4, 0.3, 0.8], "easeOutQuad")
        a.rot("root", ts + 0.34, [1.2, 0, 0.3], "easeOutCubic")
        a.ev(ts + 0.15, "sound", id="pistol.jam_clunk")
    if end == "feed":
        fly(a, "ejected_casing", ts + 0.05, [0, 1.75, 1.8], [0, 0, 0])
        a.rot("ejected_casing", ts + 0.035, [0, 0, 0])
        a.ev(ts + 0.05, "eject_shell", bone="slide", offset=[9.3, 10.0, 9.8], velocity=[2.2, 1.6, 0.8])
        a.pos("slide", ts + 0.035, [0, 0, SLIDE_BACK])
        a.pos("slide", ts + 0.11, [0, 0, SLIDE_BACK])
        a.pos("slide", ts + 0.16, [0, 0, 1.4], "easeInQuad")
        a.pos("slide", ts + 0.2, [0, 0, 1.7], "easeOutQuad")
        a.pos("slide", ts + 0.27, [0, 0, 1.4], "easeInQuad")
        a.show("chambered_round", ts + 0.15)
        a.pos("chambered_round", ts + 0.15, ROUND_FEED)
        a.rot("chambered_round", ts + 0.15, [28, 0, 0])
        a.pos("chambered_round", ts + 0.24, [0.5, 0.2, 1.4], "easeOutQuad")
        a.rot("chambered_round", ts + 0.24, [34, 0, 0], "easeOutQuad")
        a.rot("root", ts + 0.16, [1.8, 0, 0.3])
        a.rot("root", ts + 0.18, [3.0, -0.3, 0.6], "easeOutQuad")
        a.rot("root", ts + 0.36, [1.0, 0, 0.2], "easeOutCubic")
        a.ev(ts + 0.16, "sound", id="pistol.jam_clunk")
    if end == "double":
        a.rot("ejected_casing", ts + 0.035, [0, 0, 0])
        a.pos("ejected_casing", ts + 0.12, [0.5, 1.95, 2.6], "easeOutQuad")
        a.rot("ejected_casing", ts + 0.12, [0, 0, 18], "easeOutQuad")
        a.pos("slide", ts + 0.035, [0, 0, SLIDE_BACK])
        a.pos("slide", ts + 0.1, [0, 0, SLIDE_BACK])
        a.pos("slide", ts + 0.17, [0, 0, 1.9], "easeInQuad")
        a.pos("slide", ts + 0.21, [0, 0, 2.15], "easeOutQuad")
        a.pos("slide", ts + 0.28, [0, 0, 1.9], "easeInQuad")
        a.show("chambered_round", ts + 0.13)
        a.pos("chambered_round", ts + 0.13, ROUND_FEED)
        a.rot("chambered_round", ts + 0.13, [26, 0, 0])
        a.pos("chambered_round", ts + 0.22, [0.5, 0.05, 1.4], "easeOutQuad")
        a.rot("chambered_round", ts + 0.22, [30, 0, 0], "easeOutQuad")
        a.rot("root", ts + 0.17, [1.8, 0, 0.3])
        a.rot("root", ts + 0.19, [3.8, 0.4, 1.0], "easeOutQuad")
        a.rot("root", ts + 0.38, [1.4, 0, 0.4], "easeOutCubic")
        a.ev(ts + 0.17, "sound", id="pistol.jam_clunk")


def swap_mag(a, t0, empty_slide=False):
    """Cambio caricatore completo a partire da t0 (gun gia' inclinata). Restituisce il tempo di fine."""
    a.ev(t0, "arm", pose="support_hand_to_magazine")
    a.pos("magazine", t0, [0, 0, 0])
    a.pos("magazine", t0 + 0.12, mag(0.8), "easeOutBack")               # sgancio: balza fuori
    a.ev(t0 + 0.04, "sound", id="pistol.mag_release")
    a.pos("magazine", t0 + 0.42, mag(7.5), "easeInOutQuad")             # estrazione
    a.ev(t0 + 0.4, "sound", id="pistol.mag_out")
    a.pos("magazine", t0 + 0.64, add3(mag(7.5), [3.6, -6.0, -1.6]), "easeInCubic")  # mano che lo mette via
    a.rot("magazine", t0 + 0.42, [0, 0, 0])
    a.rot("magazine", t0 + 0.64, [10, 0, 38], "easeInCubic")
    a.hide("magazine", t0 + 0.66)
    # caricatore nuovo
    a.show("magazine", t0 + 0.7)
    a.pos("magazine", t0 + 0.7, add3(mag(9.0), [-3.4, -3.5, -0.8]))
    a.rot("magazine", t0 + 0.7, [-8, 0, -34])
    a.scl("magazine", t0 + 0.66, [0, 0, 0])
    a.scl("magazine", t0 + 0.7, [0.6, 0.6, 0.6], "step")
    a.scl("magazine", t0 + 0.8, [1, 1, 1], "easeOutBack")
    a.ev(t0 + 0.7, "arm", pose="support_hand_new_magazine")
    a.pos("magazine", t0 + 1.08, mag(2.0), "easeInOutCubic")
    a.rot("magazine", t0 + 1.08, [0, 0, 0], "easeInOutCubic")
    a.pos("magazine", t0 + 1.2, mag(-0.3), "easeInQuad")
    a.pos("magazine", t0 + 1.3, [0, 0, 0], "easeOutBack")
    a.ev(t0 + 1.2, "sound", id="pistol.mag_in")
    # contraccolpo dell'inserimento sul braccio
    a.pos("root", t0 + 1.17, [0.3, 0.8, 0.4])
    a.pos("root", t0 + 1.22, [0.3, 0.55, 0.55], "easeOutQuad")
    a.pos("root", t0 + 1.4, [0.3, 0.8, 0.4], "easeOutCubic")
    a.rot("root", t0 + 1.17, [10, -8, 38])
    a.rot("root", t0 + 1.22, [7, -8, 38], "easeOutQuad")
    a.rot("root", t0 + 1.42, [10, -8, 38], "easeOutBack")
    a.pos("slide", t0 + 1.22, [0, 0, 0] if not empty_slide else [0, 0, SLIDE_BACK])
    if not empty_slide:
        a.pos("slide", t0 + 1.26, [0, 0, 0.09], "easeOutQuad")
        a.pos("slide", t0 + 1.34, [0, 0, 0], "easeOutCubic")
    return t0 + 1.45


def tilt_up(a, t0, t1):
    a.pos("root", t0, [0, 0, 0])
    a.pos("root", t1, [0.3, 0.8, 0.4], "easeInOutCubic")
    a.rot("root", t0, [0, 0, 0])
    a.rot("root", t1, [10, -8, 38], "easeInOutCubic")


def tilt_down(a, t0, t1):
    a.pos("root", t0, [0.3, 0.8, 0.4])
    a.pos("root", t1, [0, 0, 0], "easeInOutCubic")
    a.rot("root", t0, [10, -8, 38])
    a.rot("root", t1, [0, 0, 0], "easeInOutCubic")


def clear_rack(a, t0, slide0, casing0=None, round0=None, eject=None, feed=True):
    """Tap-rack: colpetto, carrello tirato indietro, ostacolo espulso, rilascio e nuova cartuccia.

    casing0 / round0 = (posizione, rotazione) di partenza se lo stato di inceppamento li sposta;
    eject = "casing" | "round" (quale oggetto vola fuori dalla finestra durante la manovra).
    """
    tap_mag(a, t0)
    a.pos("slide", t0, [0, 0, slide0])
    a.pos("slide", t0 + 0.38, [0, 0, slide0])
    a.ev(t0 + 0.3, "arm", pose="support_hand_to_slide")
    a.pos("root", t0 + 0.14, [0, 0, 0])
    a.pos("root", t0 + 0.4, [0.2, 0.5, 0.3], "easeInOutCubic")
    a.rot("root", t0 + 0.14, [0, 0, 0])
    a.rot("root", t0 + 0.4, [6, -5, 20], "easeInOutCubic")
    a.pos("slide", t0 + 0.64, [0, 0, SLIDE_BACK], "easeOutQuad")
    a.ev(t0 + 0.5, "sound", id="pistol.rack_back")
    a.pos("barrel", t0 + 0.38, [0, 0, 0])
    a.rot("barrel", t0 + 0.38, [0, 0, 0])
    a.pos("barrel", t0 + 0.5, [0, 0, 0.5], "easeOutQuad")
    a.rot("barrel", t0 + 0.5, [5, 0, 0], "easeOutQuad")
    if eject == "casing":
        c_pos, c_rot = casing0
        a.pos("ejected_casing", t0, c_pos)
        a.rot("ejected_casing", t0, c_rot)
        a.pos("ejected_casing", t0 + 0.5, c_pos)
        fly(a, "ejected_casing", t0 + 0.55, c_pos, c_rot)
        a.ev(t0 + 0.55, "eject_shell", bone="slide", offset=[9.3, 10.0, 9.8], velocity=[2.0, 2.0, 0.8])
    if eject == "round":
        r_pos, r_rot = round0
        a.show("chambered_round", t0 - 0.001)
        a.pos("chambered_round", t0, r_pos)
        a.rot("chambered_round", t0, r_rot)
        a.pos("chambered_round", t0 + 0.5, r_pos)
        fly(a, "chambered_round", t0 + 0.55, r_pos, r_rot)
        a.ev(t0 + 0.55, "eject_live_round", bone="slide", offset=[9.3, 10.0, 9.8], velocity=[2.0, 2.0, 0.8])
    a.pos("slide", t0 + 1.12, [0, 0, SLIDE_BACK])
    a.ev(t0 + 1.12, "arm", pose="support_hand_release_slide")
    slide_close(a, t0 + 1.12, SLIDE_BACK)
    a.ev(t0 + 1.17, "sound", id="pistol.slide_forward")
    if feed:
        feed_round(a, t0 + 1.15)
    a.pos("root", t0 + 1.12, [0.2, 0.5, 0.3])
    a.pos("root", t0 + 1.17, [0.2, 0.35, 0.55], "easeOutQuad")
    a.pos("root", t0 + 1.75, [0, 0, 0], "easeInOutCubic")
    a.rot("root", t0 + 1.12, [6, -5, 20])
    a.rot("root", t0 + 1.17, [3, -5, 20], "easeOutQuad")
    a.rot("root", t0 + 1.75, [0, 0, 0], "easeInOutCubic")
    return t0 + 1.75


# ---------------------------------------------------------------- animazioni
def build():
    A = {}

    # ---- idle: respiro e ondeggio del polso
    a = Anim(4.0, loop=True, note="a riposo: respiro, ondeggio, assestamento lento")
    for t, y, rx, rz, x in ((0, 0, 0, 0, 0), (1.0, 0.1, 0.5, 0.35, 0.04), (2.0, 0, 0, 0, 0),
                            (3.0, 0.1, 0.5, -0.35, -0.04), (4.0, 0, 0, 0, 0)):
        a.pos("root", t, [x, y, 0], "easeInOutQuad")
        a.rot("root", t, [rx, 0, rz], "easeInOutQuad")
    A["idle"] = a.done()

    # ---- draw / holster
    a = Anim(0.8, note="estrazione: dal basso con leggero sovra-sbalzo, la pistola si assesta")
    a.pos("root", 0, [0.4, -7.5, 3.5])
    a.pos("root", 0.45, [-0.1, 0.5, -0.3], "easeOutCubic")
    a.pos("root", 0.7, [0, 0, 0], "easeOutBack")
    a.rot("root", 0, [-58, 18, 14])
    a.rot("root", 0.45, [8, -2, -2], "easeOutCubic")
    a.rot("root", 0.7, [0, 0, 0], "easeOutBack")
    a.pos("slide", 0.5, [0, 0, 0])
    a.pos("slide", 0.54, [0, 0, 0.12], "easeOutQuad")
    a.pos("slide", 0.62, [0, 0, 0], "easeOutCubic")
    a.pos("trigger_bar", 0.5, [0, 0, 0])
    a.ev(0.0, "sound", id="pistol.draw")
    a.ev(0.47, "sound", id="pistol.draw_settle")
    A["draw"] = a.done()

    a = Anim(0.55, hold_last=True, note="rimessa nella fondina")
    a.pos("root", 0, [0, 0, 0])
    a.pos("root", 0.15, [0, 0.4, -0.3], "easeOutQuad")
    a.pos("root", 0.55, [0.4, -7.5, 3.5], "easeInBack")
    a.rot("root", 0, [0, 0, 0])
    a.rot("root", 0.15, [6, 0, 0], "easeOutQuad")
    a.rot("root", 0.55, [-58, 18, 14], "easeInBack")
    a.ev(0.0, "sound", id="pistol.holster")
    A["holster"] = a.done()

    # ---- sprint
    a = Anim(0.3, hold_last=True, note="ingresso nella posa di corsa (pistola abbassata)")
    a.pos("root", 0, [0, 0, 0])
    a.pos("root", 0.3, [0.5, -2.0, -0.6], "easeInOutCubic")
    a.rot("root", 0, [0, 0, 0])
    a.rot("root", 0.3, [-34, 4, 6], "easeInOutCubic")
    A["sprint_in"] = a.done()
    a = Anim(0.3, hold_last=True, note="uscita dalla posa di corsa")
    a.pos("root", 0, [0.5, -2.0, -0.6])
    a.pos("root", 0.3, [0, 0, 0], "easeOutBack")
    a.rot("root", 0, [-34, 4, 6])
    a.rot("root", 0.3, [0, 0, 0], "easeOutBack")
    A["sprint_out"] = a.done()
    a = Anim(0.7, loop=True, note="corsa: dondolio a due passi")
    for t, dy, rz, rx in ((0, 0, 6, -34), (0.175, 0.45, 3, -36), (0.35, 0, -2, -34),
                          (0.525, 0.45, 3, -36), (0.7, 0, 6, -34)):
        a.pos("root", t, [0.5, -2.0 + dy, -0.6], "easeInOutQuad")
        a.rot("root", t, [rx, 4, rz], "easeInOutQuad")
    a.pos("magazine", 0, [0, 0, 0])
    a.pos("magazine", 0.175, [0, -0.06, 0.02], "easeInOutQuad")
    a.pos("magazine", 0.35, [0, 0, 0], "easeInOutQuad")
    a.pos("magazine", 0.525, [0, -0.06, 0.02], "easeInOutQuad")
    a.pos("magazine", 0.7, [0, 0, 0], "easeInOutQuad")
    A["sprint"] = a.done()

    # ---- sparo
    a = Anim(0.62, note="sparo normale: grilletto, rinculo, bossolo, nuova cartuccia in camera")
    shot(a, 0.09, "normal")
    A["fire"] = a.done()

    a = Anim(0.62, hold_last=True, note="ultimo colpo: il carrello resta bloccato indietro")
    shot(a, 0.09, "lock")
    A["fire_last"] = a.done()

    a = Anim(0.3, note="a vuoto / caricatore scarico: solo grilletto e clic")
    a.rot("trigger", 0, [0, 0, 0])
    a.rot("trigger", 0.06, [-13, 0, 0], "easeInQuad")
    a.rot("trigger", 0.14, [-13, 0, 0])
    a.rot("trigger", 0.26, [0, 0, 0], "easeOutCubic")
    a.pos("trigger_bar", 0.06, [0, 0, 0.45], "easeInQuad")
    a.pos("trigger_bar", 0.14, [0, 0, 0.45])
    a.pos("trigger_bar", 0.26, [0, 0, 0], "easeOutCubic")
    a.pos("root", 0.06, [0, 0, 0])
    a.pos("root", 0.075, [0, -0.03, 0.18], "easeOutQuad")
    a.pos("root", 0.22, [0, 0, 0], "easeOutCubic")
    a.rot("root", 0.06, [0, 0, 0])
    a.rot("root", 0.075, [1.4, 0, 0], "easeOutQuad")
    a.rot("root", 0.22, [0, 0, 0], "easeOutCubic")
    a.ev(0.06, "sound", id="pistol.dry_fire")
    A["dry_fire"] = a.done()

    # ---- ricariche
    a = Anim(2.65, note="ricarica tattica: il colpo in canna resta, il caricatore si cambia")
    tilt_up(a, 0.0, 0.34)
    end = swap_mag(a, 0.38)
    tap_mag(a, end - 0.02)
    tilt_down(a, end + 0.12, end + 0.62)
    a.length = round(end + 0.66, 2)
    A["reload_tactical"] = a.done()

    a = Anim(3.2, note="ricarica a otturatore aperto: carrello fermo indietro, cambio caricatore, rilascio")
    a.pos("slide", 0, [0, 0, SLIDE_BACK])
    a.hide("chambered_round", 0)
    tilt_up(a, 0.0, 0.34)
    end = swap_mag(a, 0.38, empty_slide=True)
    a.ev(end, "arm", pose="support_hand_to_slide")
    a.pos("slide", end + 0.2, [0, 0, SLIDE_BACK])
    a.ev(end + 0.2, "arm", pose="support_hand_release_slide")
    slide_close(a, end + 0.2, SLIDE_BACK)
    feed_round(a, end + 0.23)
    a.ev(end + 0.25, "sound", id="pistol.slide_forward")
    a.pos("root", end + 0.2, [0.3, 0.8, 0.4])
    a.pos("root", end + 0.25, [0.3, 0.55, 0.65], "easeOutQuad")
    a.pos("root", end + 0.85, [0, 0, 0], "easeInOutCubic")
    a.rot("root", end + 0.2, [10, -8, 38])
    a.rot("root", end + 0.25, [6, -8, 38], "easeOutQuad")
    a.rot("root", end + 0.85, [0, 0, 0], "easeInOutCubic")
    a.length = round(end + 0.9, 2)
    A["reload_empty"] = a.done()

    # ---- ispezione (stile controllo caricatore/camera)
    a = Anim(4.2, note="ispezione: gira l'arma, controlla il caricatore e la camera (press check)")
    a.pos("root", 0, [0, 0, 0])
    a.pos("root", 0.5, [0.4, 0.7, 0.3], "easeInOutCubic")
    a.pos("root", 3.7, [0.4, 0.7, 0.3])
    a.pos("root", 4.2, [0, 0, 0], "easeInOutCubic")
    a.rot("root", 0, [0, 0, 0])
    a.rot("root", 0.5, [4, -42, 8], "easeInOutCubic")           # mostra il lato destro
    a.rot("root", 1.35, [4, -42, 8])
    a.rot("root", 1.95, [-6, 28, -16], "easeInOutCubic")        # mostra il lato sinistro / caricatore
    a.rot("root", 2.6, [12, -6, 30], "easeInOutCubic")          # guarda in camera dall'alto
    a.rot("root", 3.7, [12, -6, 30])
    a.rot("root", 4.2, [0, 0, 0], "easeInOutCubic")
    a.pos("magazine", 1.9, [0, 0, 0])
    a.pos("magazine", 2.05, mag(1.1), "easeOutBack")            # lo sfila di un dito per vedere i colpi
    a.pos("magazine", 2.35, mag(1.1))
    a.pos("magazine", 2.45, mag(-0.2), "easeInQuad")
    a.pos("magazine", 2.55, [0, 0, 0], "easeOutBack")
    a.pos("slide", 2.8, [0, 0, 0])
    a.pos("slide", 3.0, [0, 0, 1.3], "easeOutQuad")             # press check
    a.pos("slide", 3.45, [0, 0, 1.3])
    a.pos("slide", 3.58, [0, 0, 0], "easeInCubic")
    a.pos("barrel", 2.8, [0, 0, 0])
    a.pos("barrel", 3.0, [0, 0, 0.3], "easeOutQuad")
    a.pos("barrel", 3.45, [0, 0, 0.3])
    a.pos("barrel", 3.58, [0, 0, 0], "easeInCubic")
    a.ev(0.5, "arm", pose="inspect_turn")
    a.ev(2.05, "sound", id="pistol.mag_peek")
    a.ev(2.55, "sound", id="pistol.mag_in")
    a.ev(3.0, "sound", id="pistol.press_check")
    a.ev(3.58, "sound", id="pistol.slide_forward")
    A["inspect"] = a.done()

    # ---- inceppamenti: stato di inceppamento + manovra risolutiva
    a = Anim(0.9, hold_last=True, note="inceppamento: bossolo di traverso nella finestra (stovepipe); resta in posa")
    shot(a, 0.09, "stovepipe")
    A["stovepipe_jam"] = a.done()

    cas_stove = ([0.9, 2.2, 0.8], [90, 0, 0])
    a = Anim(0, note="soluzione stovepipe: colpetto, carrello indietro, il bossolo vola via, nuova cartuccia")
    a.pos("ejected_casing", 0, cas_stove[0])
    a.rot("ejected_casing", 0, cas_stove[1])
    a.hide("chambered_round", 0)       # il bossolo incastrato ha preso il posto della cartuccia
    a.pos("slide", 0, [0, 0, 0.55])
    t = clear_rack(a, 0.0, 0.55, casing0=cas_stove, eject="casing")
    a.length = round(t, 2)
    A["clear_stovepipe"] = a.done()

    a = Anim(0.9, hold_last=True, note="inceppamento: mancata alimentazione, cartuccia di punta, carrello fuori batteria")
    shot(a, 0.09, "feed")
    A["feed_jam"] = a.done()

    rd_feed = ([0.5, 0.2, 1.4], [34, 0, 0])
    a = Anim(0, note="soluzione mancata alimentazione: la cartuccia incastrata viene espulsa, nuova cartuccia in camera")
    a.hide("ejected_casing", 0)
    a.pos("chambered_round", 0, rd_feed[0])
    a.rot("chambered_round", 0, rd_feed[1])
    a.show("chambered_round", 0)
    a.pos("slide", 0, [0, 0, 1.4])
    t = clear_rack(a, 0.0, 1.4, round0=rd_feed, eject="round")
    a.length = round(t, 2)
    A["clear_feed_jam"] = a.done()

    a = Anim(0.7, hold_last=True, note="cilecca: il grilletto scatta ma non parte il colpo (cartuccia in camera resta)")
    a.rot("trigger", 0.0, [0, 0, 0])
    a.rot("trigger", 0.07, [-13, 0, 0], "easeInQuad")
    a.rot("trigger", 0.16, [-13, 0, 0])
    a.rot("trigger", 0.32, [-4, 0, 0], "easeOutCubic")
    a.pos("trigger_bar", 0.07, [0, 0, 0.45], "easeInQuad")
    a.pos("trigger_bar", 0.16, [0, 0, 0.45])
    a.pos("trigger_bar", 0.32, [0, 0, 0.12], "easeOutCubic")
    a.pos("root", 0.07, [0, 0, 0])
    a.pos("root", 0.085, [0, -0.04, 0.2], "easeOutQuad")
    a.pos("root", 0.26, [0, 0, 0], "easeOutCubic")
    a.rot("root", 0.085, [1.6, 0, 0], "easeOutQuad")
    a.rot("root", 0.26, [0, 0, 0], "easeOutCubic")
    a.ev(0.07, "sound", id="pistol.misfire_click")
    A["misfire"] = a.done()

    a = Anim(0, note="soluzione cilecca: carrello indietro, la cartuccia difettosa vola via, nuova cartuccia")
    a.pos("trigger_bar", 0, [0, 0, 0.12])
    a.rot("trigger", 0, [-4, 0, 0])
    a.rot("trigger", 0.2, [0, 0, 0], "easeOutCubic")
    a.pos("trigger_bar", 0.2, [0, 0, 0], "easeOutCubic")
    t = clear_rack(a, 0.0, 0.0, round0=([0, 0, 0], [0, 0, 0]), eject="round")
    a.length = round(t, 2)
    A["clear_misfire"] = a.done()

    a = Anim(0.95, hold_last=True, note="inceppamento: doppia alimentazione, bossolo e cartuccia incastrati, carrello bloccato")
    shot(a, 0.09, "double")
    A["double_feed_jam"] = a.done()

    cas_dbl = ([0.5, 1.95, 2.6], [0, 0, 18])
    rd_dbl = ([0.5, 0.05, 1.4], [30, 0, 0])
    a = Anim(0, note="soluzione doppia alimentazione: caricatore fuori, tre scarrellate, caricatore nuovo, scarrellata finale")
    a.pos("ejected_casing", 0, cas_dbl[0])
    a.rot("ejected_casing", 0, cas_dbl[1])
    a.pos("chambered_round", 0, rd_dbl[0])
    a.rot("chambered_round", 0, rd_dbl[1])
    a.show("chambered_round", 0)
    a.pos("slide", 0, [0, 0, 1.9])
    tilt_up(a, 0.0, 0.34)
    a.ev(0.0, "arm", pose="support_hand_to_magazine")
    # 1) caricatore via (senza ricarica ancora)
    a.pos("magazine", 0.38, [0, 0, 0])
    a.pos("magazine", 0.5, mag(0.8), "easeOutBack")
    a.pos("magazine", 0.8, mag(7.5), "easeInOutQuad")
    a.pos("magazine", 1.02, add3(mag(7.5), [3.6, -6.0, -1.6]), "easeInCubic")
    a.rot("magazine", 0.8, [0, 0, 0])
    a.rot("magazine", 1.02, [10, 0, 38], "easeInCubic")
    a.hide("magazine", 1.04)
    a.ev(0.42, "sound", id="pistol.mag_release")
    a.ev(0.8, "sound", id="pistol.mag_out")
    # 2) scarrellate: il bossolo e la cartuccia schizzano fuori alla prima
    for i, tr in enumerate((1.1, 1.62, 2.14)):
        a.pos("slide", tr, [0, 0, 1.9 if i == 0 else 0])
        a.pos("slide", tr + 0.2, [0, 0, SLIDE_BACK], "easeOutQuad")
        a.pos("slide", tr + 0.34, [0, 0, SLIDE_BACK])
        a.pos("slide", tr + 0.4, [0, 0, 0], "easeInCubic")
        a.pos("slide", tr + 0.44, [0, 0, 0.12], "easeOutQuad")
        a.pos("slide", tr + 0.5, [0, 0, 0], "easeOutCubic")
        a.ev(tr + 0.12, "sound", id="pistol.rack_back")
        a.ev(tr + 0.4, "sound", id="pistol.slide_forward")
        a.pos("root", tr, [0.3, 0.8, 0.4])
        a.pos("root", tr + 0.2, [0.3, 0.55, 0.9], "easeOutQuad")
        a.pos("root", tr + 0.5, [0.3, 0.8, 0.4], "easeOutBack")
    a.pos("ejected_casing", 1.28, cas_dbl[0])
    a.rot("ejected_casing", 1.28, cas_dbl[1])
    fly(a, "ejected_casing", 1.3, cas_dbl[0], cas_dbl[1])
    a.pos("chambered_round", 1.34, rd_dbl[0])
    a.rot("chambered_round", 1.34, rd_dbl[1])
    fly(a, "chambered_round", 1.36, rd_dbl[0], rd_dbl[1])
    a.ev(1.3, "eject_shell", bone="slide", offset=[9.3, 10.0, 9.8], velocity=[2.0, 2.0, 0.8])
    a.ev(1.36, "eject_live_round", bone="slide", offset=[9.3, 10.0, 9.8], velocity=[1.8, 2.2, 0.6])
    a.rot("root", 0.34, [10, -8, 38])
    a.rot("root", 2.66, [10, -8, 38])
    # 3) caricatore nuovo e scarrellata di chiusura
    t0 = 2.7
    a.show("magazine", t0)
    a.scl("magazine", t0 - 0.04, [0, 0, 0])
    a.scl("magazine", t0, [0.6, 0.6, 0.6], "step")
    a.scl("magazine", t0 + 0.1, [1, 1, 1], "easeOutBack")
    a.pos("magazine", t0, add3(mag(9.0), [-3.4, -3.5, -0.8]))
    a.rot("magazine", t0, [-8, 0, -34])
    a.pos("magazine", t0 + 0.38, mag(2.0), "easeInOutCubic")
    a.rot("magazine", t0 + 0.38, [0, 0, 0], "easeInOutCubic")
    a.pos("magazine", t0 + 0.5, mag(-0.3), "easeInQuad")
    a.pos("magazine", t0 + 0.6, [0, 0, 0], "easeOutBack")
    a.ev(t0 + 0.5, "sound", id="pistol.mag_in")
    a.pos("root", t0 + 0.47, [0.3, 0.8, 0.4])
    a.pos("root", t0 + 0.52, [0.3, 0.55, 0.55], "easeOutQuad")
    a.pos("root", t0 + 0.7, [0.3, 0.8, 0.4], "easeOutCubic")
    t1 = t0 + 0.8
    a.pos("slide", t1, [0, 0, 0])
    a.pos("slide", t1 + 0.2, [0, 0, SLIDE_BACK], "easeOutQuad")
    a.pos("slide", t1 + 0.34, [0, 0, SLIDE_BACK])
    slide_close(a, t1 + 0.34, SLIDE_BACK)
    a.hide("chambered_round", t1 + 0.3)
    feed_round(a, t1 + 0.37)
    a.ev(t1 + 0.12, "sound", id="pistol.rack_back")
    a.ev(t1 + 0.4, "sound", id="pistol.slide_forward")
    tilt_down(a, t1 + 0.7, t1 + 1.2)
    a.length = round(t1 + 1.25, 2)
    A["clear_double_feed"] = a.done()
    return A


# ------------------------------------------------------------ serializzazione
def dump(o, ind=0):
    pad = "  " * ind
    if isinstance(o, dict):
        if o and all(not isinstance(v, (dict, list)) or (isinstance(v, list) and all(not isinstance(x, (dict, list)) for x in v))
                     for v in o.values()):
            return "{" + ", ".join(f"{json.dumps(k)}: {dump(v)}" for k, v in o.items()) + "}"
        inner = (",\n").join(f"{pad}  {json.dumps(k)}: {dump(v, ind + 1)}" for k, v in o.items())
        return "{\n" + inner + "\n" + pad + "}"
    if isinstance(o, list):
        if all(not isinstance(x, (dict, list)) for x in o):
            return "[" + ", ".join(dump(x) for x in o) + "]"
        inner = (",\n").join(f"{pad}  {dump(x, ind + 1)}" for x in o)
        return "[\n" + inner + "\n" + pad + "]"
    return json.dumps(o)


def main():
    anims = build()
    doc = {
        "format": "qrm.anim/1",
        "model": "qrm:item/police_pistol",
        "rig": "qrm:rigs/police_pistol",
        "playback": "vedi tools/police_pistol_pose.py: position in unita' Blockbench, rotation in gradi (X poi Y poi Z) "
                    "attorno al pivot del bone, scale moltiplicativa; la curva di un keyframe e' quella del tratto che ci arriva",
        "events": "type: sound{id} | muzzle_flash{bone,offset,size} | eject_shell / eject_live_round{bone,offset,velocity} | "
                  "camera_kick{pitch,yaw} | arm{pose}; gli id dei suoni sono chiavi logiche, da mappare agli asset",
        "animations": anims,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(dump(doc) + "\n", encoding="utf-8")
    names = ", ".join(f"{n} ({a['length']}s)" for n, a in anims.items())
    print(f"{len(anims)} animazioni: {names}")
    # controllo: ogni bone usato esiste nel rig
    rig = json.loads((ASSETS / "rigs/police_pistol.json").read_text(encoding="utf-8"))
    for n, a in anims.items():
        for b in a["bones"]:
            assert b in rig["bones"], (n, b)
        for b, chs in a["bones"].items():
            for ch in chs.values():
                for k in ch:
                    assert k["ease"] in pose.EASE, (n, b, k)
        assert a["length"] > 0, n


if __name__ == "__main__":
    main()
