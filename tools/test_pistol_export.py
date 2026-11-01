#!/usr/bin/env python3
"""Test di regressione dell'esportatore della pistola (solo libreria standard).

Il .bbmodel e gli asset di riferimento sono fissati al commit 855d1e29, cosi' il test
resta valido anche quando il modello o gli asset vecchi cambiano nei commit successivi.

Uso (dalla radice del repo): python -m unittest discover -s tools -p "test_pistol_export.py" -v
"""
import json
import math
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import pistol_export  # noqa: E402

REF = "855d1e29"
REF_ASSETS = "qrm-neoforge/src/main/resources/assets/qrm/"
ROOT = Path(__file__).resolve().parent.parent


def git_show(path):
    return subprocess.check_output(["git", "show", f"{REF}:{path}"], cwd=ROOT)


def cubes(elements):
    """Geometria dei cubi in forma confrontabile: nome -> (from, to, rotation, facce/UV)."""
    out = {}
    for e in elements:
        faces = {n: [round(c, 4) for c in f["uv"]] for n, f in e["faces"].items()}
        out[e["name"]] = (e["from"], e["to"], e.get("rotation"), faces)
    return out


class HeadExportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls._tmp = tempfile.TemporaryDirectory()
        tmp = Path(cls._tmp.name)
        bb = tmp / "police_pistol.bbmodel"
        bb.write_bytes(git_show("design/police-pistol/police_pistol.bbmodel"))
        cls.out = tmp / "out"
        pistol_export.export(bb, cls.out)

    @classmethod
    def tearDownClass(cls):
        cls._tmp.cleanup()

    def ref_json(self, rel):
        return json.loads(git_show(REF_ASSETS + rel).decode("utf-8"))

    def out_json(self, rel):
        return json.loads((self.out / rel).read_text(encoding="utf-8"))

    def test_head_bbmodel_matches_head_assets(self):
        # modello completo
        ref = self.ref_json("models/item/police_pistol.json")
        got = self.out_json("models/item/police_pistol.json")
        self.assertEqual(cubes(ref["elements"]), cubes(got["elements"]))
        self.assertEqual(ref["texture_size"], got["texture_size"])
        self.assertEqual(ref["display"], got["display"])
        self.assertEqual(got["textures"]["0"], "qrm_police:item/police_pistol")

        # rig: bone, parent, pivot
        ref_rig = self.ref_json("rigs/police_pistol.json")
        got_rig = self.out_json("rigs/police_pistol.json")
        self.assertEqual(set(ref_rig["bones"]), set(got_rig["bones"]))
        for name, rb in ref_rig["bones"].items():
            gb = got_rig["bones"][name]
            self.assertEqual(rb["parent"], gb["parent"], name)
            self.assertEqual(rb["pivot"], gb["pivot"], name)
            self.assertEqual(rb["has_model"], gb["has_model"], name)
            self.assertEqual(rb["note"], gb["note"], name)
        for key in ("baked_rotations", "magazine_axis", "suggested_travel"):
            self.assertEqual(ref_rig[key], got_rig[key], key)
        self.assertEqual(got_rig["model"], "qrm_police:item/police_pistol")
        self.assertEqual(got_rig["parts_prefix"], "qrm_police:item/police_pistol_parts/")

        # modelli delle parti: stessi file e stessi cubi
        ref_parts = {n for n, b in ref_rig["bones"].items() if b["has_model"]}
        got_files = {p.stem for p in (self.out / "models/item/police_pistol_parts").glob("*.json")}
        self.assertEqual(ref_parts, got_files)
        for part in ref_parts:
            rel = f"models/item/police_pistol_parts/{part}.json"
            self.assertEqual(cubes(self.ref_json(rel)["elements"]),
                             cubes(self.out_json(rel)["elements"]), part)

    def test_texture_is_written(self):
        png = (self.out / "textures/item/police_pistol.png").read_bytes()
        self.assertEqual(png[:8], b"\x89PNG\r\n\x1a\n")

    def test_rejects_unsupported_rotation(self):
        bb = json.loads(git_show("design/police-pistol/police_pistol.bbmodel"))
        bb["elements"][0]["rotation"] = [10, 0, 0]  # non multiplo di 7,5
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "bad.bbmodel"
            p.write_text(json.dumps(bb), encoding="utf-8")
            with self.assertRaises(ValueError):
                pistol_export.export(p, Path(t) / "out")
        bb["elements"][0]["rotation"] = [15, 15, 0]  # due assi
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "bad.bbmodel"
            p.write_text(json.dumps(bb), encoding="utf-8")
            with self.assertRaises(ValueError):
                pistol_export.export(p, Path(t) / "out")


def mini_bbmodel(interp="linear"):
    """Un .bbmodel minimo: un gruppo "root", un cubo, una clip "fire" con tutte le interpolazioni."""
    def kf(ch, t, xyz, interpolation):
        x, y, z = xyz
        return {"channel": ch, "data_points": [{"x": x, "y": y, "z": z}], "uuid": f"{ch}{t}",
                "time": t, "color": -1, "interpolation": interpolation}
    face = {"uv": [0, 0, 2, 2], "texture": 0}
    anim = {"uuid": "a1", "name": "fire", "loop": "hold", "length": 0.5, "animators": {
        "g1": {"name": "root", "type": "bone", "keyframes": [
            kf("position", 0, [1, 2, 3], "linear"), kf("position", 0.25, [2, 4, 6], interp),
            kf("rotation", 0.1, [10, 20, 30], "step"), kf("rotation", 0.2, [0, 0, 0], "catmullrom"),
            kf("scale", 0.0, [1, 1, 1], "linear")]},
        "effects": {"name": "Effetti", "type": "effect", "keyframes": [
            {"channel": "timeline", "uuid": "e2", "time": 0.2, "color": -1, "interpolation": "linear",
             "data_points": [{"script": "sound pistol.fire\ncommit mag_out"}]},
            {"channel": "timeline", "uuid": "e1", "time": 0.05, "color": -1, "interpolation": "linear",
             "data_points": [{"script": "camera_kick 1.4 0.25\n\n  arm trigger_squeeze  "}]},
            {"channel": "timeline", "uuid": "e3", "time": 0.3, "color": -1, "interpolation": "linear",
             "data_points": [{"script": "sound('pistol.mag_in'); commit('mag_in');\nmuzzle_flash(); camera_kick(1.4, 0.25);"}]}]}}}
    return {"resolution": {"width": 16, "height": 16},
            "elements": [{"uuid": "c1", "name": "cubo", "from": [0, 0, 0], "to": [1, 1, 1], "origin": [0, 0, 0],
                          "faces": {"north": face}}],
            "outliner": [{"name": "root", "uuid": "g1", "origin": [0, 0, 0], "children": ["c1"]}],
            "textures": [{"source": "data:image/png;base64,iVBORw0KGgo="}],
            "animations": [anim]}


class AnimationExportTest(unittest.TestCase):
    def run_export(self, bb):
        t = tempfile.TemporaryDirectory()
        self.addCleanup(t.cleanup)
        p = Path(t.name) / "mini.bbmodel"
        p.write_text(json.dumps(bb), encoding="utf-8")
        pistol_export.export(p, Path(t.name) / "out")
        return json.loads((Path(t.name) / "out/animations/police_pistol.json").read_text(encoding="utf-8"))

    def test_animation_keys_and_events(self):
        out = self.run_export(mini_bbmodel())
        self.assertEqual(out["format"], "qrm.anim/2")
        fire = out["animations"]["fire"]
        self.assertEqual((fire["length"], fire["loop"]), (0.5, False))
        self.assertIs(fire["hold_last"], True)
        root = fire["bones"]["root"]
        # X della posizione e X/Y della rotazione sono invertiti rispetto a Blockbench
        self.assertEqual(root["position"], [{"t": 0, "v": [-1, 2, 3], "interp": "linear"},
                                            {"t": 0.25, "v": [-2, 4, 6], "interp": "linear"}])
        self.assertEqual(root["rotation"], [{"t": 0.1, "v": [-10, -20, 30], "interp": "step"},
                                            {"t": 0.2, "v": [0, 0, 0], "interp": "catmullrom"}])
        self.assertEqual(root["scale"], [{"t": 0, "v": [1, 1, 1], "interp": "linear"}])
        self.assertEqual(fire["events"], [
            {"t": 0.05, "type": "camera_kick", "args": ["1.4", "0.25"]},
            {"t": 0.05, "type": "arm", "args": ["trigger_squeeze"]},
            {"t": 0.2, "type": "sound", "args": ["pistol.fire"]},
            {"t": 0.2, "type": "commit", "args": ["mag_out"]},
        # sintassi Molang: sound('..'); commit('..'); muzzle_flash(); camera_kick(a, b);
        {"t": 0.3, "type": "sound", "args": ["pistol.mag_in"]},
        {"t": 0.3, "type": "commit", "args": ["mag_in"]},
        {"t": 0.3, "type": "muzzle_flash", "args": []},
        {"t": 0.3, "type": "camera_kick", "args": ["1.4", "0.25"]}])

    def test_loop_modes_always_carry_both_flags(self):
        for mode, loop, hold in (("loop", True, False), ("hold", False, True), ("once", False, False)):
            bb = mini_bbmodel()
            bb["animations"][0]["loop"] = mode
            clip = self.run_export(bb)["animations"]["fire"]
            self.assertIs(clip["loop"], loop, mode)
            self.assertIs(clip["hold_last"], hold, mode)

    def test_bezier_fails_with_clip_name(self):
        with self.assertRaises(ValueError) as cm:
            self.run_export(mini_bbmodel(interp="bezier"))
        self.assertIn("fire", str(cm.exception))

    def test_export_is_deterministic(self):
        a = self.run_export(mini_bbmodel())
        b = self.run_export(mini_bbmodel())
        self.assertEqual(json.dumps(a), json.dumps(b))


# --- riferimento: qrm.anim/1 del commit 855d1e29 (curve con nome, copiate dallo script di posa) ---
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


_C1, _C3 = 1.70158, 2.70158
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
    "easeInBack": lambda x: _C3 * x ** 3 - _C1 * x ** 2,
    "easeOutBack": lambda x: 1 + _C3 * (x - 1) ** 3 + _C1 * (x - 1) ** 2,
    "easeOutElastic": _out_elastic,
    "easeOutBounce": _out_bounce,
}
# il .bbmodel campiona le curve con nome a ~60 fps in segmenti lineari: l'errore di quel campionamento
# (non delle convenzioni di segno/asse, che darebbero errori di ordine di grandezza maggiore) arriva a
# 0,055 unita' su position/scale e 0,7 gradi su rotation, nelle oscillazioni piu' rapide (fire.chambered_round)
TOL = {"position": 0.06, "rotation": 0.75, "scale": 0.06}
REST = {"position": [0, 0, 0], "rotation": [0, 0, 0], "scale": [1, 1, 1]}


def eval_v1(keys, rest, t):
    """Canale qrm.anim/1: riposo prima del primo keyframe, curva del tratto che arriva al keyframe."""
    keys = sorted(keys, key=lambda k: k["t"])
    if t < keys[0]["t"]:
        return list(rest)
    if t == keys[0]["t"]:
        return list(keys[0]["v"])
    for a, b in zip(keys, keys[1:]):
        if t <= b["t"]:
            span = b["t"] - a["t"]
            x = 0.0 if span <= 0 else (t - a["t"]) / span
            e = EASE[b.get("ease", "linear")](x)
            return [p + (q - p) * e for p, q in zip(a["v"], b["v"])]
    return list(keys[-1]["v"])


def eval_v2(keys, rest, t):
    """Canale qrm.anim/2 con keyframe lineari/step: riposo prima del primo, resta sull'ultimo dopo."""
    if t < keys[0]["t"]:
        return list(rest)
    for a, b in zip(keys, keys[1:]):
        if t <= b["t"]:
            if b["t"] == a["t"]:
                return list(b["v"])
            x = (t - a["t"]) / (b["t"] - a["t"])
            if a["interp"] == "step":
                x = 0.0 if x < 1 else 1.0
            return [p + (q - p) * x for p, q in zip(a["v"], b["v"])]
    return list(keys[-1]["v"])


class ReferenceAnimationRoundTripTest(unittest.TestCase):
    def test_reference_animations_roundtrip(self):
        with tempfile.TemporaryDirectory() as t:
            tmp = Path(t)
            bb = tmp / "ref.bbmodel"
            bb.write_bytes(git_show("design/police-pistol/police_pistol.bbmodel"))
            pistol_export.export(bb, tmp / "out")
            got = json.loads((tmp / "out/animations/police_pistol.json").read_text(encoding="utf-8"))["animations"]
        ref = json.loads(git_show(REF_ASSETS + "animations/police_pistol.json").decode("utf-8"))["animations"]
        self.assertEqual(list(ref), list(got))
        checked = 0
        for name, a in ref.items():
            self.assertEqual(a["length"], got[name]["length"], name)
            self.assertEqual(bool(a.get("loop")), got[name]["loop"], name)
            self.assertEqual(bool(a.get("hold_last")), got[name]["hold_last"], name)
            self.assertEqual(set(a["bones"]), set(got[name]["bones"]), name)
            for bone, chans in a["bones"].items():
                for ch, keys in chans.items():
                    kt = {k["t"] for k in keys}
                    for i in range(10):
                        ts = a["length"] * (i + 0.5) / 10
                        # niente campioni sui salti dei keyframe step (la conversione li sposta di 1 ms)
                        while any(abs(ts - k) < 0.003 for k in kt):
                            ts += 0.004
                        want = eval_v1(keys, REST[ch], ts)
                        have = eval_v2(got[name]["bones"][bone][ch], REST[ch], ts)
                        for w, h in zip(want, have):
                            self.assertAlmostEqual(w, h, delta=TOL[ch], msg=f"{name}.{bone}.{ch}@{ts:.3f}")
                        checked += 1
        self.assertGreater(checked, 100)


if __name__ == "__main__":
    unittest.main()
