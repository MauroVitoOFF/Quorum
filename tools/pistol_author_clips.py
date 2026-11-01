#!/usr/bin/env python3
"""Crea nel progetto Blockbench della pistola le clip slide_release, slide_rack e ammo_check.

Il movimento non e' inventato da zero: si riprendono pezzi delle clip esistenti, cosi' le nuove
somigliano alle vecchie.

Nota: le clip di partenza clear_misfire & co. sono state tolte dal progetto (inutilizzate) e le tre clip sono ormai
rifinite a mano nel .bbmodel: questo script e' conservato solo come storico e non va rieseguito.

  slide_release (0,4 s): il carrello che scatta in avanti e' quello di reload_empty (2,03-2,18 s,
      commit slide a 2,08), spostato a 0,15-0,30 s; commit slide e suono pistol.slide_release a 0,2 s.
      Il carrello parte dalla posa bloccata finale di fire_last (la stessa che il gioco impone come stato).
      Il colpo in canna non e' animato: con il caricatore vuoto il carrello va avanti a camera vuota.
  slide_rack (0,5 s): la tirata del carrello di clear_misfire (0,38-0,64 s) compressa in 0,10-0,22 s,
      poi lo scatto in avanti di clear_misfire (1,12-1,27 s) da 0,24 s: l'impatto cade a 0,30 s,
      insieme a commit slide. Suono pistol.slide_rack all'inizio della tirata. L'azione parte solo con
      camera vuota e caricatore con colpi, quindi il colpo che entra in canna (da reload_empty) e' animato.
  ammo_check (1,0 s): l'estrazione parziale del caricatore di inspect (1,90-2,55 s, mag_peek a 2,05,
      mag_in) compressa in un secondo, con una piccola inclinazione di root verso la camera.
      Suoni pistol.mag_peek (caricatore fuori) e pistol.mag_in (rientro), commit check a 0,6 s.

Tempi (regola R2, costanti di PistolAction): durata ceil(lunghezza*20) tick, commit round(t*20).
Interpolazioni solo linear/catmullrom. Lo script e' idempotente: le tre clip vengono sostituite,
mai duplicate, e gli uuid sono deterministici.

Uso (dalla radice del repo): python tools/pistol_author_clips.py [progetto.bbmodel]
"""
import json
import math
import sys
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_BBMODEL = ROOT / "design/police-pistol/police_pistol.bbmodel"
NS = uuid.UUID("6f2d1c1e-3a57-4b8e-9b1a-5c0e7d9f2a41")  # spazio fisso per uuid ripetibili

# nome clip -> (lunghezza s, tick attesi, {tick del commit: fase})
TIMINGS = {
    "slide_release": (0.4, 8, {4: "slide"}),
    "slide_rack": (0.5, 10, {6: "slide"}),
    "ammo_check": (1.0, 20, {12: "check"}),
}


def r4(v):
    return round(float(v), 4)


class Clip:
    """Costruttore di una clip nel formato del .bbmodel."""

    def __init__(self, name, length, bones):
        self.name, self.length, self.bones = name, length, bones
        self.channels = {}  # (bone, canale) -> {t: (x, y, z, interp)}
        self.events = {}    # t -> [righe]

    def key(self, bone, ch, t, v, interp="linear"):
        assert interp in ("linear", "catmullrom"), interp
        assert -1e-9 <= t <= self.length + 1e-9, (self.name, bone, ch, t)
        self.channels.setdefault((bone, ch), {})[r4(t)] = (r4(v[0]), r4(v[1]), r4(v[2]), interp)

    def event(self, t, *lines):
        self.events.setdefault(r4(t), []).extend(lines)

    def build(self):
        def uid(*parts):
            return str(uuid.uuid5(NS, "/".join(str(p) for p in parts)))

        animators = {}
        for (bone, ch), keys in self.channels.items():
            a = animators.setdefault(self.bones[bone], {"name": bone, "type": "bone", "keyframes": []})
            for t in sorted(keys):
                x, y, z, interp = keys[t]
                a["keyframes"].append({"channel": ch, "data_points": [{"x": x, "y": y, "z": z}],
                                       "uuid": uid(self.name, bone, ch, t), "time": t, "color": -1,
                                       "interpolation": interp})
        if self.events:
            eff = {"name": "Effetti", "type": "effect", "keyframes": []}
            for t in sorted(self.events):
                eff["keyframes"].append({"channel": "timeline",
                                         "data_points": [{"script": "\n".join(self.events[t])}],
                                         "uuid": uid(self.name, "effects", t), "time": t, "color": -1,
                                         "interpolation": "linear"})
            animators["effects"] = eff
        return {"uuid": uid(self.name), "name": self.name, "loop": "once", "override": False,
                "length": self.length, "snapping": 60, "selected": False, "anim_time_update": "",
                "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators}


def source_keys(bb, clip, bone, ch, t0, t1):
    """Keyframe [(t, (x, y, z))] di una clip esistente tra t0 e t1 (estremi compresi)."""
    a = next(c for c in bb["animations"] if c["name"] == clip)
    out = []
    for an in a["animators"].values():
        if an["name"] != bone:
            continue
        for k in an["keyframes"]:
            t = float(k["time"])
            if k["channel"] == ch and t0 - 1e-6 <= t <= t1 + 1e-6:
                dp = k["data_points"][0]
                out.append((t, (float(dp["x"]), float(dp["y"]), float(dp["z"]))))
    out.sort(key=lambda p: p[0])
    if not out:
        raise ValueError(f"{clip}/{bone}/{ch}: nessun keyframe tra {t0} e {t1}")
    return out


def last_value(bb, clip, bone, ch):
    a = next(c for c in bb["animations"] if c["name"] == clip)
    an = next(x for x in a["animators"].values() if x["name"] == bone)
    k = max((k for k in an["keyframes"] if k["channel"] == ch), key=lambda k: float(k["time"]))
    dp = k["data_points"][0]
    return (float(dp["x"]), float(dp["y"]), float(dp["z"]))


def copy(clip, bb, src, bone, ch, t0, t1, remap, every=1, interp="linear"):
    """Copia i keyframe di src tra t0 e t1 con il tempo trasformato da remap; every>1 ne tiene uno ogni N
    (primo e ultimo sempre)."""
    keys = source_keys(bb, src, bone, ch, t0, t1)
    for i, (t, v) in enumerate(keys):
        if i % every == 0 or i == len(keys) - 1:
            clip.key(bone, ch, remap(t), v, interp)


def shift(d):
    return lambda t: t + d


def stretch(a0, a1, b0, b1):
    return lambda t: b0 + (t - a0) * (b1 - b0) / (a1 - a0)


def slide_release(bb, bones):
    c = Clip("slide_release", 0.4, bones)
    zero = (0, 0, 0)
    # carrello: posa bloccata (fine di fire_last), poi lo scatto di reload_empty spostato di -1,88 s
    locked_pos = last_value(bb, "fire_last", "slide", "position")
    locked_rot = last_value(bb, "fire_last", "slide", "rotation")
    c.key("slide", "position", 0, locked_pos)
    c.key("slide", "rotation", 0, locked_rot)
    copy(c, bb, "reload_empty", "slide", "position", 2.03, 2.18, shift(-1.88))
    copy(c, bb, "reload_empty", "slide", "rotation", 2.03, 2.18, shift(-1.88))
    # tappo della finestra di espulsione: nascosto finche' il carrello e' indietro
    c.key("port_cover", "scale", 0, last_value(bb, "fire_last", "port_cover", "scale"))
    copy(c, bb, "reload_empty", "port_cover", "scale", 2.0767, 2.0917, shift(-1.88))
    # root: leggera inclinazione mentre la mano va al carrello, colpo all'impatto, ritorno morbido
    c.key("root", "position", 0, zero, "catmullrom")
    c.key("root", "position", 0.12, (-0.1, 0.3, 0.15), "catmullrom")
    c.key("root", "position", 0.15, (-0.1, 0.3, 0.15))
    c.key("root", "position", 0.2, (-0.1, 0.05, 0.4), "catmullrom")  # stesso scarto di reload_empty a 2,08
    c.key("root", "position", 0.28, (-0.05, 0.08, 0.2), "catmullrom")
    c.key("root", "position", 0.4, zero, "catmullrom")
    c.key("root", "rotation", 0, zero, "catmullrom")
    c.key("root", "rotation", 0.12, (-4, 3, 12), "catmullrom")
    c.key("root", "rotation", 0.15, (-4, 3, 12))
    c.key("root", "rotation", 0.2, (-1.5, 3, 12), "catmullrom")
    c.key("root", "rotation", 0.28, (-1, 1.5, 6), "catmullrom")
    c.key("root", "rotation", 0.4, zero, "catmullrom")
    c.event(0, "arm support_hand_to_slide")
    c.event(0.15, "arm support_hand_release_slide")
    c.event(0.2, "sound pistol.slide_release", "commit slide")
    return c


def slide_rack(bb, bones):
    c = Clip("slide_rack", 0.5, bones)
    zero = (0, 0, 0)
    # tirata (clear_misfire 0,38-0,64) compressa in 0,10-0,22: un keyframe ogni tre, catmullrom
    c.key("slide", "position", 0, zero)
    copy(c, bb, "clear_misfire", "slide", "position", 0.38, 0.64, stretch(0.38, 0.64, 0.10, 0.22),
         every=3, interp="catmullrom")
    c.key("slide", "position", 0.24, (0, 0, 2.5))
    # scatto in avanti (clear_misfire 1,12-1,27) da 0,24: impatto a 0,30 = commit slide
    copy(c, bb, "clear_misfire", "slide", "position", 1.135, 1.27, shift(0.24 - 1.12))
    c.key("slide", "rotation", 0, zero)
    copy(c, bb, "clear_misfire", "slide", "rotation", 1.12, 1.27, shift(0.24 - 1.12))
    # tappo: si apre all'inizio della tirata, si chiude all'impatto (come in clear_misfire)
    copy(c, bb, "clear_misfire", "port_cover", "scale", 0.385, 0.4, stretch(0.38, 0.64, 0.10, 0.22))
    c.key("port_cover", "scale", 0, (1, 1, 1))
    copy(c, bb, "clear_misfire", "port_cover", "scale", 1.1683, 1.1833, shift(0.24 - 1.12))
    # colpo che entra in canna (reload_empty 2,06-2,11, l'impatto li' e' a 2,09)
    c.key("chambered_round", "scale", 0, zero)
    copy(c, bb, "reload_empty", "chambered_round", "scale", 2.059, 2.06, shift(0.30 - 2.09))
    for ch in ("position", "rotation"):
        c.key("chambered_round", ch, 0, zero)
        c.key("chambered_round", ch, 0.30 - 2.09 + 2.059, zero)
        copy(c, bb, "reload_empty", "chambered_round", ch, 2.06, 2.11, shift(0.30 - 2.09))
    # root: inclinazione come in clear_misfire (ridotta), spinta in avanti durante la tirata, colpo all'impatto
    c.key("root", "position", 0, zero, "catmullrom")
    c.key("root", "position", 0.1, (-0.15, 0.35, 0.2), "catmullrom")
    c.key("root", "position", 0.22, (-0.15, 0.3, 0.05), "catmullrom")
    c.key("root", "position", 0.24, (-0.15, 0.3, 0.05))
    c.key("root", "position", 0.3, (-0.15, 0.2, 0.35), "catmullrom")
    c.key("root", "position", 0.38, (-0.08, 0.15, 0.2), "catmullrom")
    c.key("root", "position", 0.5, zero, "catmullrom")
    c.key("root", "rotation", 0, zero, "catmullrom")
    c.key("root", "rotation", 0.1, (-4, 3.5, 14), "catmullrom")
    c.key("root", "rotation", 0.24, (-4.5, 3.5, 14), "catmullrom")
    c.key("root", "rotation", 0.3, (-2, 3.5, 14), "catmullrom")
    c.key("root", "rotation", 0.38, (-1.5, 2, 8), "catmullrom")
    c.key("root", "rotation", 0.5, zero, "catmullrom")
    c.event(0, "arm support_hand_to_slide")
    c.event(0.1, "sound pistol.slide_rack")
    c.event(0.24, "arm support_hand_release_slide")
    c.event(0.3, "commit slide")
    return c


def ammo_check(bb, bones):
    c = Clip("ammo_check", 1.0, bones)
    zero = (0, 0, 0)
    # caricatore: estrazione di inspect 1,90-2,05 (stessa durata) a 0,25-0,40
    c.key("magazine", "position", 0, zero)
    copy(c, bb, "inspect", "magazine", "position", 1.9, 2.05, shift(0.25 - 1.9), every=2, interp="catmullrom")
    # resta fuori fino a 0,62 (commit check a 0,6), poi il rientro di inspect 2,35-2,55 a 0,62-0,82
    c.key("magazine", "position", 0.62, source_keys(bb, "inspect", "magazine", "position", 2.35, 2.35)[0][1])
    copy(c, bb, "inspect", "magazine", "position", 2.3643, 2.55, shift(0.62 - 2.35), every=2)
    # root: piccola inclinazione verso la camera (inspect arriva a 6/-28/-16), poi ritorno
    c.key("root", "position", 0, zero, "catmullrom")
    c.key("root", "position", 0.22, (-0.2, 0.35, 0.15), "catmullrom")
    c.key("root", "position", 0.7, (-0.2, 0.35, 0.15), "catmullrom")
    c.key("root", "position", 0.73, (-0.2, 0.42, 0.15), "catmullrom")  # spinta del palmo sul caricatore
    c.key("root", "position", 0.8, (-0.2, 0.35, 0.15), "catmullrom")
    c.key("root", "position", 1.0, zero, "catmullrom")
    c.key("root", "rotation", 0, zero, "catmullrom")
    c.key("root", "rotation", 0.22, (4, -14, -8), "catmullrom")
    c.key("root", "rotation", 0.8, (4, -14, -8), "catmullrom")
    c.key("root", "rotation", 1.0, zero, "catmullrom")
    c.event(0, "arm support_hand_to_magazine")
    c.event(0.4, "sound pistol.mag_peek")
    c.event(0.6, "commit check")
    c.event(0.72, "sound pistol.mag_in")
    return c


def check_timings(clip):
    length, ticks, commits = TIMINGS[clip["name"]]
    assert clip["length"] == length and math.ceil(length * 20 - 1e-6) == ticks, clip["name"]
    found = {}
    for an in clip["animators"].values():
        for k in an["keyframes"]:
            assert k["interpolation"] in ("linear", "catmullrom"), (clip["name"], k)
            if an["type"] == "effect":
                for line in k["data_points"][0]["script"].splitlines():
                    if line.startswith("commit "):
                        found[round(k["time"] * 20)] = line.split()[1]
    assert found == commits, (clip["name"], found, commits)


def bone_uuids(bb):
    out = {}

    def walk(nodes):
        for n in nodes:
            if isinstance(n, dict):
                out[n["name"]] = n["uuid"]
                walk(n.get("children", []))
    walk(bb["outliner"])
    return out


def main(argv):
    path = Path(argv[1]) if len(argv) > 1 else DEFAULT_BBMODEL
    raw = path.read_text(encoding="utf-8")
    bb = json.loads(raw)
    bones = bone_uuids(bb)
    new = [f(bb, bones).build() for f in (slide_release, slide_rack, ammo_check)]
    for clip in new:
        check_timings(clip)
    names = {c["name"] for c in new}
    kept = [c for c in bb["animations"] if c["name"] not in names]
    bb["animations"] = kept + new
    ascii_only = raw.isascii()
    path.write_text(json.dumps(bb, ensure_ascii=ascii_only), encoding="utf-8")
    print(f"clip scritte in {path.name}: " + ", ".join(f"{c['name']} ({c['length']} s)" for c in new))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
