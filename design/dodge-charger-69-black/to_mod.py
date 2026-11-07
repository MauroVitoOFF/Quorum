"""Copia l'esportazione di Blockbench nel mod (qrm-vehicles), dopo aver controllato che ci sia tutto quello che serve.

Uso: python to_mod.py            (dalla cartella del progetto o da questa)

Il modello Blockbench contiene già tutto quello che il mod richiede, senza ritocchi a mano:
  - `body` con il perno a (0, 12, 0): beccheggio, rollio e sospensioni ruotano attorno a quell'altezza;
  - `steer_fl` / `steer_fr`, genitori delle ruote anteriori con il perno sul mozzo;
  - vetri come ossa `pane_*` (parabrezza, lunotto, vetri delle portiere), disegnati translucidi;
  - locator di sedili, occhio del guidatore, scarico e suono del motore (in Blockbench l'asse X dei locator è opposto
    a quello del file esportato: il valore nel file è quello che conta);
  - ossa `hitbox` e `hit_*`: hitbox e zone di interazione.

Flusso: modifica il .bbmodel in Blockbench, esporta (File > Esporta > Bedrock Geometry) su
`dodge_charger_69_black.geo.json` in questa cartella, poi lancia questo script.
"""
import json
import pathlib
import shutil
import sys

HERE = pathlib.Path(__file__).resolve().parent
MOD = HERE.parents[1] / 'qrm-vehicles' / 'src' / 'main' / 'resources' / 'assets' / 'qrm_vehicles'
NAME = 'dodge_charger_69_black'

REQUIRED_BONES = ['root', 'body', 'steer_fl', 'steer_fr', 'wheel_fl', 'wheel_fr', 'wheel_rl', 'wheel_rr', 'hood',
                  'pane_windshield', 'pane_rear_window', 'hitbox']
REQUIRED_LOCATORS = ['driver_eye', 'driver_hip', 'passenger_hip', 'exhaust_tip', 'engine_sound']


def check(geo):
    bones = {b['name']: b for b in geo['minecraft:geometry'][0]['bones']}
    problems = [f'manca l\'osso {n}' for n in REQUIRED_BONES if n not in bones]
    if 'body' in bones and bones['body'].get('pivot') != [0, 12, 0]:
        problems.append(f"il perno di body deve essere [0, 12, 0], non {bones['body'].get('pivot')}")
    for side in ('fl', 'fr'):
        w = bones.get('wheel_' + side)
        if w and w.get('parent') != 'steer_' + side:
            problems.append(f'wheel_{side} deve stare dentro steer_{side}')
    found = {k for b in bones.values() for k in b.get('locators', {})}
    problems += [f'manca il locator {n}' for n in REQUIRED_LOCATORS if n not in found]
    if not any(n.startswith('hit_') for n in bones):
        problems.append('nessuna zona hit_*')
    return problems


def main():
    src = HERE / f'{NAME}.geo.json'
    with open(src, encoding='utf-8') as f:
        geo = json.load(f)
    problems = check(geo)
    if problems:
        print('Esportazione non valida per il mod:')
        for p in problems:
            print('  -', p)
        sys.exit(1)
    shutil.copyfile(src, MOD / 'geo' / f'{NAME}.geo.json')
    shutil.copyfile(HERE / f'{NAME}.png', MOD / 'textures' / 'entity' / f'{NAME}.png')
    print('Copiati geo e texture nel mod.')


if __name__ == '__main__':
    main()
