#!/usr/bin/env python3
"""Test di regressione dell'esportatore della pistola (solo libreria standard).

Il .bbmodel e gli asset di riferimento sono fissati al commit 855d1e29, cosi' il test
resta valido anche quando il modello o gli asset vecchi cambiano nei commit successivi.

Uso (dalla radice del repo): python -m unittest discover -s tools -p "test_pistol_export.py" -v
"""
import json
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


if __name__ == "__main__":
    unittest.main()
