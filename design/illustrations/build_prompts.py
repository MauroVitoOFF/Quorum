"""Genera PROMPTS.md: i prompt per Gemini delle illustrazioni di QRM, ognuno completo e pronto da incollare.

Uso: python build_prompts.py   (scrive PROMPTS.md accanto allo script)
Per cambiare stile o personaggi si modificano STYLE e CHARACTERS qui sotto, poi si rigenera.
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))

STYLE = """Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas ({grid}) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio {ratio}."""

CHARACTERS = {
    "OFFICER": "THE OFFICER: a blocky police officer with a pale skin tone and short brown hair, a flat navy-blue peaked cap with a black visor and a small gold badge on the front, a navy-blue uniform shirt (#2f5fb3 with darker blue shading), gold shoulder patches, white cuffs, a black belt with a small gold buckle, dark blue trousers, brown boots and a short black baton. Angry, determined face with a thick frowning eyebrow and gritted teeth.",
    "THIEF": "THE THIEF: a blocky thief with a black eye mask, short brown hair, a black-and-white horizontally striped prison shirt with bare hands, light-grey trousers with black stripes, black shoes, a gritted-teeth grin, and a brown burlap sack with a small gold dollar sign on it.",
    "ANNA": "ANNA: a blocky young woman with a long brown ponytail, a light denim jacket over a white t-shirt, dark blue jeans and white sneakers, friendly curious face with big eyes.",
    "MARCO": "MARCO: a blocky young man with short black hair and a short beard, a grey hooded sweatshirt, dark trousers and brown work boots, relaxed smile.",
    "STAFF": "THE STAFF MEMBER: a blocky adult in a black blazer with gold trim on the lapels and a gold armband (no text on it), short silver-grey hair, round glasses, calm confident expression.",
}

SHEETS = [
    ("sheet-1", "16:9", "320x180 pixel canvas",
     "Character sheet. On a flat deep navy background (#0d1526) with a thin lighter band of ground at the bottom, show THE OFFICER on the left and THE THIEF on the right, each drawn three times in a neat row: standing in a side view facing right, running in a side view facing right, and a front view. Large, clear, same scale for all poses, with plenty of empty space between them.", ["OFFICER", "THIEF"]),
    ("sheet-2", "16:9", "320x180 pixel canvas",
     "Character sheet. On a flat deep navy background (#0d1526) with a thin lighter band of ground at the bottom, show ANNA, MARCO and THE STAFF MEMBER standing next to each other in a row in a side view facing right, then the same three in a front view in a second row. Same scale for all, plenty of empty space.", ["ANNA", "MARCO", "STAFF"]),
]

# (id, priorita, rapporto, griglia, titolo, scena, personaggi)
SCENES = [
    ("hero", 1, "16:9", "320x180 pixel canvas", "Intestazione",
     "A wide night street of a small blocky roleplay town, flat 2D side view with simple parallax depth. In the center and right half THE OFFICER chases THE THIEF running to the right, exactly like in the reference image, with small clouds of pixel dust behind their feet. In the background: a bank building with an ATM whose screen glows light blue, a police station with a blue lamp above the door, a gold street lamp, a few lit square windows, a starry sky made of single pixels and a square moon. The ground is a strip of dark stone blocks. Keep the left 35% of the image calm and mostly dark (dark buildings, very few details) because a title will be placed there.", ["OFFICER", "THIEF"]),
    ("module-bank", 1, "1:1", "192x192 pixel canvas", "Module Bank",
     "A single blocky ATM machine standing on a street at night, with a light-blue glowing screen made of flat pixel bands, a slot with a stack of banknotes coming out, and a few gold coins on the ground. Small, simple, iconic composition centered in the frame, deep navy background with a few pixel stars.", []),
    ("module-locker", 1, "1:1", "192x192 pixel canvas", "Module Locker",
     "A row of three blocky metal lockers (steel blue) in a dim room, the middle one open and showing a few simple items inside: a folded jacket, a small box and a flashlight. Small, simple, iconic composition centered in the frame, deep navy background.", []),
    ("module-boss", 1, "1:1", "192x192 pixel canvas", "Module Boss",
     "A blocky boss office: a wooden desk with a leather chair behind it, a ledger book, a stack of gold coins and a small gold crown-shaped ornament on the desk, a lamp with a warm gold light. Small, simple, iconic composition centered in the frame, deep navy background.", []),
    ("closing", 1, "16:9", "320x180 pixel canvas", "Chiusura",
     "The same small blocky town at dawn, with a sky made of flat bands from deep navy to blue to pale gold and a rising square sun. THE OFFICER and THE THIEF now walk calmly side by side on the street, to the right, tired but friendly, the officer carrying the burlap sack over the shoulder, the thief eating a sandwich. Warm, funny and peaceful. Leave the top 30% of the image calm for a title.", ["OFFICER", "THIEF"]),

    ("beat-1", 2, "4:3", "240x180 pixel canvas", "Anna crea il suo personaggio",
     "ANNA stands alone on a lit square platform in a night plaza, looking down at a small blank ID card she holds with a tiny portrait square and a few gold bars instead of text. A soft spotlight made of two flat pixel bands falls on her. A few small outlines of other characters wait in the dark background.", ["ANNA"]),
    ("beat-2", 2, "4:3", "240x180 pixel canvas", "Lo staff le dà un lavoro",
     "Inside a simple police station, THE STAFF MEMBER hands a large gold police badge to ANNA across a wooden counter. ANNA is already wearing a navy police cap. On the wall behind: a wooden board with colored pins and a blue lamp.", ["ANNA", "STAFF"]),
    ("beat-3", 2, "4:3", "240x180 pixel canvas", "Un blocco diventa il timbracartellino",
     "A blocky wall-mounted time clock (a stone block with a round clock face and a small lever) on the wall of a police station corridor. ANNA in a navy police uniform presses the lever; a small green indicator light on the block is on and a few gold pixel sparks fly out. Simple, clear composition.", ["ANNA"]),
    ("beat-4", 2, "4:3", "240x180 pixel canvas", "Anna paga un altro personaggio",
     "On a street at night, ANNA hands a folded paper banknote to MARCO. Both smile. A few small gold coins sparkle in the air between them. Street lamp with a gold glow in the background.", ["ANNA", "MARCO"]),
    ("beat-5", 2, "4:3", "240x180 pixel canvas", "Passa allo sportello",
     "ANNA stands at a blocky ATM with a light-blue glowing screen at night. A neat stack of banknotes is coming out of the slot. A small gold coin floats above the stack. Simple, centered composition.", ["ANNA"]),
    ("beat-6", 2, "4:3", "240x180 pixel canvas", "Lo staff modera senza uscire dal gioco",
     "THE STAFF MEMBER stands in the middle of the blocky town at night, with a floating translucent light-blue panel in front of them made of flat pixel rectangles and rows of small bars (a log list, no text), a small magnifying glass above one row. The street and buildings are visible behind the panel, to show that they never left the game.", ["STAFF"]),

    ("feat-characters", 3, "1:1", "192x192 pixel canvas", "Personaggi",
     "One blocky player avatar in the foreground and, standing behind and above it in a row of three small cards or frames, three different blocky characters: a police officer, a mechanic with a wrench, a shopkeeper with an apron. It suggests one person who plays many characters. Simple and iconic.", []),
    ("feat-economy", 3, "1:1", "192x192 pixel canvas", "Economia",
     "A tidy still life: a stack of gold coins, a fan of paper banknotes, and an open ledger book with rows of small horizontal bars (no numbers, no text), on a dark wooden table, with a small balance scale beside them. Simple and iconic.", []),
    ("feat-jobs", 3, "1:1", "192x192 pixel canvas", "Lavori e organizzazioni",
     "A staircase of four blocks going up to the right, with a blocky worker standing on each step wearing a slightly more elaborate version of the same navy uniform (more gold on the cap and shoulders as they go up), and a small flag on a pole at the top. It suggests ranks. Simple and iconic.", []),
    ("feat-points", 3, "1:1", "192x192 pixel canvas", "Punti di interazione",
     "Three world blocks in a row (a stone block, a wooden block, a metal block), each with a small glowing floating symbol above it made of gold pixels: a clock, a coin, and a door. They represent places where things happen. Simple and iconic.", []),
    ("feat-staff", 3, "1:1", "192x192 pixel canvas", "Pannello staff",
     "A blocky gold shield in front of a floating translucent light-blue panel made of flat pixel rectangles with rows of small bars (no text), a small blocky hand-held tablet shape in the corner. It suggests a control panel. Simple and iconic.", []),

    ("start-1", 3, "4:3", "240x180 pixel canvas", "Installa",
     "A small blocky treasure chest with a gold ribbon, lid open, with a glowing square file icon (a plain cube) hovering above it, and a wooden shelf labeled only by a folder shape (no text). It suggests dropping one file into a folder. Deep navy background.", []),
    ("start-2", 3, "4:3", "240x180 pixel canvas", "Avvia",
     "A blocky server cabinet with rows of small lit lights, one big green power light just turned on, a few gold pixel sparks around it, deep navy background.", []),
    ("start-3", 3, "4:3", "240x180 pixel canvas", "Crea il primo lavoro",
     "A blocky officer in a navy uniform standing next to a large parchment scroll that shows a vertical list of three small badge icons of increasing size on the left and horizontal bars on the right (no text). It suggests a job with ranks. Deep navy background.", ["OFFICER"]),
]


def build(style_args, characters, body):
    parts = [STYLE.format(**style_args)]
    for key in characters:
        parts.append(CHARACTERS[key])
    parts.append(body)
    return "\n\n".join(parts)


def main():
    out = []
    w = out.append
    w("# Illustrazioni di QRM: prompt per Gemini\n")
    w("File generato da `build_prompts.py`: non si modifica a mano, si cambia lo script e si rigenera.\n")
    w("## Come procedere\n")
    w("1. **Immagine di riferimento.** Allega a ogni conversazione con Gemini la tua immagine di riferimento (il poliziotto che insegue il fuggitivo). I prompt la citano come «the attached reference image».")
    w("2. **Un'unica conversazione, in quest'ordine.** Prima le due schede dei personaggi (`sheet-1`, `sheet-2`). Quando sei soddisfatto, **allegale a ogni prompt successivo** e aggiungi in cima: *«Use the attached character sheets so every character looks exactly the same.»* È l'unico modo per tenere i personaggi uguali da un'immagine all'altra.")
    w("3. **Un prompt alla volta.** Incolla il blocco intero. Se una griglia di pixel è irregolare o sfocata, rigenera aggiungendo: *«Make the pixel grid perfectly regular: every art pixel is a clean square block, no blur, no smoothing.»*")
    w("4. **Salva ogni immagine come PNG, alla dimensione massima, senza ritoccarla,** in `design/illustrations/incoming/` con il nome indicato qui sotto (per esempio `hero.png`).")
    w("5. **Quando le immagini sono lì,** le rifinisco io: allineo i pixel alla griglia, riduco la palette ai colori scelti, e le esporto per il sito.\n")
    w("Priorità: **1** serve per prima (intestazione, Modules, chiusura), **2** la storia, **3** sistemi e passi di avvio. Le schede dei personaggi vengono sempre per prime.\n")
    w("Le schermate finali non devono contenere testo: l'etichetta e le scritte le mette il sito. Gemini tende a inventare lettere: se ne compaiono, rigenera con *«no text of any kind»*.\n")
    w("---\n")

    def block(id_, title, prio, ratio, grid, text):
        w(f"## `{id_}` · {title}" + (f" · priorità {prio}" if prio else ""))
        w(f"File: `incoming/{id_}.png` · rapporto {ratio}\n")
        w("```text")
        w(text)
        w("```\n")

    w("# Schede dei personaggi (da fare per prime)\n")
    for id_, ratio, grid, body, chars in SHEETS:
        block(id_, "Scheda personaggi", 0, ratio, grid, build({"grid": grid, "ratio": ratio}, chars, body))

    w("# Scene\n")
    for id_, prio, ratio, grid, title, body, chars in sorted(SCENES, key=lambda s: s[1]):
        block(id_, title, prio, ratio, grid, build({"grid": grid, "ratio": ratio}, chars, body))

    with open(os.path.join(HERE, "PROMPTS.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")
    print("ok", len(SHEETS) + len(SCENES), "prompt")


if __name__ == "__main__":
    main()
