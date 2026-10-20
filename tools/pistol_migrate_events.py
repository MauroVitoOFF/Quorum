#!/usr/bin/env python3
"""Uso una tantum: porta gli eventi di qrm.anim/1 nel .bbmodel come istruzioni di timeline.

Per ogni clip crea (o sostituisce) l'animatore effetti ("effects", type "effect") con un keyframe
per istante sul canale "timeline"; lo script del keyframe ha una istruzione per riga:

  sound <id> | muzzle_flash | eject_shell | camera_kick <pitch> <yaw> | arm <posa>

Mappatura da qrm.anim/1: sound -> sound <id>; muzzle_flash e eject_shell perdono bone/offset/size/velocity
(il client usa punti di ancoraggio fissi); eject_live_round diventa eject_shell; camera_kick e arm
mantengono i parametri. In piu' si aggiungono le istruzioni commit, che dicono al server quando la
ricarica cambia davvero lo stato: commit mag_out / commit mag_in agli istanti dei suoni pistol.mag_out /
pistol.mag_in di reload_tactical e reload_empty, commit slide al suono pistol.slide_forward di reload_empty.

Uso (dalla radice del repo):
  python tools/pistol_migrate_events.py <qrm.anim/1.json> [progetto.bbmodel]
"""
import json
import sys
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_BBMODEL = ROOT / "design/police-pistol/police_pistol.bbmodel"
COMMITS = {  # clip -> {id del suono: fase da confermare}
    "reload_tactical": {"pistol.mag_out": "mag_out", "pistol.mag_in": "mag_in"},
    "reload_empty": {"pistol.mag_out": "mag_out", "pistol.mag_in": "mag_in", "pistol.slide_forward": "slide"},
}


def fmt(x):
    return f"{float(x):g}"


def lines_for(clip, ev):
    """Istruzioni (in ordine) per un evento qrm.anim/1."""
    kind = ev["type"]
    if kind == "sound":
        lines = [f"sound {ev['id']}"]
        phase = COMMITS.get(clip, {}).get(ev["id"])
        if phase:
            lines.append(f"commit {phase}")
        return lines
    if kind == "muzzle_flash":
        return ["muzzle_flash"]
    if kind in ("eject_shell", "eject_live_round"):
        return ["eject_shell"]
    if kind == "camera_kick":
        return [f"camera_kick {fmt(ev['pitch'])} {fmt(ev['yaw'])}"]
    if kind == "arm":
        return [f"arm {ev['pose']}"]
    raise ValueError(f"{clip}: evento sconosciuto '{kind}' a t={ev['t']}")


def migrate(anim_v1, bb):
    by_name = {c["name"]: c for c in bb["animations"]}
    for name, a in anim_v1["animations"].items():
        clip = by_name[name]
        times = {}  # istante -> righe, nell'ordine di apparizione
        for ev in a.get("events", []):
            times.setdefault(round(ev["t"], 3), []).extend(lines_for(name, ev))
        keyframes = [{"channel": "timeline", "data_points": [{"script": "\n".join(lines)}],
                      "uuid": str(uuid.uuid4()), "time": t, "color": -1, "interpolation": "linear"}
                     for t, lines in sorted(times.items())]
        animators = {k: v for k, v in clip["animators"].items() if v.get("type") != "effect"}
        if keyframes:
            animators["effects"] = {"name": "Effetti", "type": "effect", "keyframes": keyframes}
        clip["animators"] = animators
    return bb


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    anim = json.loads(Path(argv[1]).read_text(encoding="utf-8"))
    path = Path(argv[2]) if len(argv) > 2 else DEFAULT_BBMODEL
    bb = migrate(anim, json.loads(path.read_text(encoding="utf-8")))
    path.write_text(json.dumps(bb), encoding="utf-8")
    n = sum(len(a.get("events", [])) for a in anim["animations"].values())
    print(f"migrati {n} eventi in {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
