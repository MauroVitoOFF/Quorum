# Illustrazioni di QRM: prompt per Gemini

File generato da `build_prompts.py`: non si modifica a mano, si cambia lo script e si rigenera.

## Come procedere

1. **Immagine di riferimento.** Allega a ogni conversazione con Gemini la tua immagine di riferimento (il poliziotto che insegue il fuggitivo). I prompt la citano come «the attached reference image».
2. **Un'unica conversazione, in quest'ordine.** Prima le due schede dei personaggi (`sheet-1`, `sheet-2`). Quando sei soddisfatto, **allegale a ogni prompt successivo** e aggiungi in cima: *«Use the attached character sheets so every character looks exactly the same.»* È l'unico modo per tenere i personaggi uguali da un'immagine all'altra.
3. **Un prompt alla volta.** Incolla il blocco intero. Se una griglia di pixel è irregolare o sfocata, rigenera aggiungendo: *«Make the pixel grid perfectly regular: every art pixel is a clean square block, no blur, no smoothing.»*
4. **Salva ogni immagine come PNG, alla dimensione massima, senza ritoccarla,** in `design/illustrations/incoming/` con il nome indicato qui sotto (per esempio `hero.png`).
5. **Quando le immagini sono lì,** le rifinisco io: allineo i pixel alla griglia, riduco la palette ai colori scelti, e le esporto per il sito.

Priorità: **1** serve per prima (intestazione, Modules, chiusura), **2** la storia, **3** sistemi e passi di avvio. Le schede dei personaggi vengono sempre per prime.

Le schermate finali non devono contenere testo: l'etichetta e le scritte le mette il sito. Gemini tende a inventare lettere: se ne compaiono, rigenera con *«no text of any kind»*.

---

# Schede dei personaggi (da fare per prime)

## `sheet-1` · Scheda personaggi
File: `incoming/sheet-1.png` · rapporto 16:9

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (320x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 16:9.

THE OFFICER: a blocky police officer with a pale skin tone and short brown hair, a flat navy-blue peaked cap with a black visor and a small gold badge on the front, a navy-blue uniform shirt (#2f5fb3 with darker blue shading), gold shoulder patches, white cuffs, a black belt with a small gold buckle, dark blue trousers, brown boots and a short black baton. Angry, determined face with a thick frowning eyebrow and gritted teeth.

THE THIEF: a blocky thief with a black eye mask, short brown hair, a black-and-white horizontally striped prison shirt with bare hands, light-grey trousers with black stripes, black shoes, a gritted-teeth grin, and a brown burlap sack with a small gold dollar sign on it.

Character sheet. On a flat deep navy background (#0d1526) with a thin lighter band of ground at the bottom, show THE OFFICER on the left and THE THIEF on the right, each drawn three times in a neat row: standing in a side view facing right, running in a side view facing right, and a front view. Large, clear, same scale for all poses, with plenty of empty space between them.
```

## `sheet-2` · Scheda personaggi
File: `incoming/sheet-2.png` · rapporto 16:9

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (320x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 16:9.

ANNA: a blocky young woman with a long brown ponytail, a light denim jacket over a white t-shirt, dark blue jeans and white sneakers, friendly curious face with big eyes.

MARCO: a blocky young man with short black hair and a short beard, a grey hooded sweatshirt, dark trousers and brown work boots, relaxed smile.

THE STAFF MEMBER: a blocky adult in a black blazer with gold trim on the lapels and a gold armband (no text on it), short silver-grey hair, round glasses, calm confident expression.

Character sheet. On a flat deep navy background (#0d1526) with a thin lighter band of ground at the bottom, show ANNA, MARCO and THE STAFF MEMBER standing next to each other in a row in a side view facing right, then the same three in a front view in a second row. Same scale for all, plenty of empty space.
```

# Scene

## `hero` · Intestazione · priorità 1
File: `incoming/hero.png` · rapporto 16:9

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (320x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 16:9.

THE OFFICER: a blocky police officer with a pale skin tone and short brown hair, a flat navy-blue peaked cap with a black visor and a small gold badge on the front, a navy-blue uniform shirt (#2f5fb3 with darker blue shading), gold shoulder patches, white cuffs, a black belt with a small gold buckle, dark blue trousers, brown boots and a short black baton. Angry, determined face with a thick frowning eyebrow and gritted teeth.

THE THIEF: a blocky thief with a black eye mask, short brown hair, a black-and-white horizontally striped prison shirt with bare hands, light-grey trousers with black stripes, black shoes, a gritted-teeth grin, and a brown burlap sack with a small gold dollar sign on it.

A wide night street of a small blocky roleplay town, flat 2D side view with simple parallax depth. In the center and right half THE OFFICER chases THE THIEF running to the right, exactly like in the reference image, with small clouds of pixel dust behind their feet. In the background: a bank building with an ATM whose screen glows light blue, a police station with a blue lamp above the door, a gold street lamp, a few lit square windows, a starry sky made of single pixels and a square moon. The ground is a strip of dark stone blocks. Keep the left 35% of the image calm and mostly dark (dark buildings, very few details) because a title will be placed there.
```

## `module-bank` · Module Bank · priorità 1
File: `incoming/module-bank.png` · rapporto 1:1

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (192x192 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 1:1.

A single blocky ATM machine standing on a street at night, with a light-blue glowing screen made of flat pixel bands, a slot with a stack of banknotes coming out, and a few gold coins on the ground. Small, simple, iconic composition centered in the frame, deep navy background with a few pixel stars.
```

## `module-locker` · Module Locker · priorità 1
File: `incoming/module-locker.png` · rapporto 1:1

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (192x192 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 1:1.

A row of three blocky metal lockers (steel blue) in a dim room, the middle one open and showing a few simple items inside: a folded jacket, a small box and a flashlight. Small, simple, iconic composition centered in the frame, deep navy background.
```

## `module-boss` · Module Boss · priorità 1
File: `incoming/module-boss.png` · rapporto 1:1

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (192x192 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 1:1.

A blocky boss office: a wooden desk with a leather chair behind it, a ledger book, a stack of gold coins and a small gold crown-shaped ornament on the desk, a lamp with a warm gold light. Small, simple, iconic composition centered in the frame, deep navy background.
```

## `closing` · Chiusura · priorità 1
File: `incoming/closing.png` · rapporto 16:9

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (320x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 16:9.

THE OFFICER: a blocky police officer with a pale skin tone and short brown hair, a flat navy-blue peaked cap with a black visor and a small gold badge on the front, a navy-blue uniform shirt (#2f5fb3 with darker blue shading), gold shoulder patches, white cuffs, a black belt with a small gold buckle, dark blue trousers, brown boots and a short black baton. Angry, determined face with a thick frowning eyebrow and gritted teeth.

THE THIEF: a blocky thief with a black eye mask, short brown hair, a black-and-white horizontally striped prison shirt with bare hands, light-grey trousers with black stripes, black shoes, a gritted-teeth grin, and a brown burlap sack with a small gold dollar sign on it.

The same small blocky town at dawn, with a sky made of flat bands from deep navy to blue to pale gold and a rising square sun. THE OFFICER and THE THIEF now walk calmly side by side on the street, to the right, tired but friendly, the officer carrying the burlap sack over the shoulder, the thief eating a sandwich. Warm, funny and peaceful. Leave the top 30% of the image calm for a title.
```

## `beat-1` · Anna crea il suo personaggio · priorità 2
File: `incoming/beat-1.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

ANNA: a blocky young woman with a long brown ponytail, a light denim jacket over a white t-shirt, dark blue jeans and white sneakers, friendly curious face with big eyes.

ANNA stands alone on a lit square platform in a night plaza, looking down at a small blank ID card she holds with a tiny portrait square and a few gold bars instead of text. A soft spotlight made of two flat pixel bands falls on her. A few small outlines of other characters wait in the dark background.
```

## `beat-2` · Un operatore le dà un lavoro · priorità 2
File: `incoming/beat-2.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

ANNA: a blocky young woman with a long brown ponytail, a light denim jacket over a white t-shirt, dark blue jeans and white sneakers, friendly curious face with big eyes.

THE STAFF MEMBER: a blocky adult in a black blazer with gold trim on the lapels and a gold armband (no text on it), short silver-grey hair, round glasses, calm confident expression.

Inside a simple police station, THE STAFF MEMBER hands a large gold police badge to ANNA across a wooden counter. ANNA is already wearing a navy police cap. On the wall behind: a wooden board with colored pins and a blue lamp.
```

## `beat-3` · Anna timbra il cartellino · priorità 2
File: `incoming/beat-3.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

ANNA: a blocky young woman with a long brown ponytail, a light denim jacket over a white t-shirt, dark blue jeans and white sneakers, friendly curious face with big eyes.

A blocky wall-mounted time clock (a stone block with a round clock face and a small lever) on the wall of a police station corridor. ANNA in a navy police uniform presses the lever; a small green indicator light on the block is on and a few gold pixel sparks fly out. Simple, clear composition.
```

## `beat-4` · Anna paga Marco · priorità 2
File: `incoming/beat-4.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

ANNA: a blocky young woman with a long brown ponytail, a light denim jacket over a white t-shirt, dark blue jeans and white sneakers, friendly curious face with big eyes.

MARCO: a blocky young man with short black hair and a short beard, a grey hooded sweatshirt, dark trousers and brown work boots, relaxed smile.

On a street at night, ANNA hands a folded paper banknote to MARCO. Both smile. A few small gold coins sparkle in the air between them. Street lamp with a gold glow in the background.
```

## `beat-5` · Anna passa allo sportello · priorità 2
File: `incoming/beat-5.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

ANNA: a blocky young woman with a long brown ponytail, a light denim jacket over a white t-shirt, dark blue jeans and white sneakers, friendly curious face with big eyes.

ANNA stands at a blocky ATM with a light-blue glowing screen at night. A neat stack of banknotes is coming out of the slot. A small gold coin floats above the stack. Simple, centered composition.
```

## `beat-6` · Lo staff interviene · priorità 2
File: `incoming/beat-6.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

THE STAFF MEMBER: a blocky adult in a black blazer with gold trim on the lapels and a gold armband (no text on it), short silver-grey hair, round glasses, calm confident expression.

MARCO: a blocky young man with short black hair and a short beard, a grey hooded sweatshirt, dark trousers and brown work boots, relaxed smile.

On the blocky town street at night, a rowdy blocky player (MARCO) stands in the background with his mouth wide open and a jagged zigzag speech bubble above his head (no letters, only zigzag lines). In the foreground THE STAFF MEMBER looks calmly at a floating translucent light-blue panel made of flat pixel rectangles with rows of small bars (no text); on the panel two simple gold icons: a speaker with a diagonal slash (mute) and a warning triangle. The street and buildings are visible behind the panel, to show that the staff never left the game.
```

## `feat-characters` · Personaggi · priorità 3
File: `incoming/feat-characters.png` · rapporto 1:1

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (192x192 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 1:1.

One blocky player avatar in the foreground and, standing behind and above it in a row of three small cards or frames, three different blocky characters: a police officer, a mechanic with a wrench, a shopkeeper with an apron. It suggests one person who plays many characters. Simple and iconic.
```

## `feat-economy` · Economia · priorità 3
File: `incoming/feat-economy.png` · rapporto 1:1

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (192x192 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 1:1.

A tidy still life: a stack of gold coins, a fan of paper banknotes, and an open ledger book with rows of small horizontal bars (no numbers, no text), on a dark wooden table, with a small balance scale beside them. Simple and iconic.
```

## `feat-jobs` · Lavori e organizzazioni · priorità 3
File: `incoming/feat-jobs.png` · rapporto 1:1

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (192x192 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 1:1.

A staircase of four blocks going up to the right, with a blocky worker standing on each step wearing a slightly more elaborate version of the same navy uniform (more gold on the cap and shoulders as they go up), and a small flag on a pole at the top. It suggests ranks. Simple and iconic.
```

## `feat-points` · Punti di interazione · priorità 3
File: `incoming/feat-points.png` · rapporto 1:1

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (192x192 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 1:1.

Three world blocks in a row (a stone block, a wooden block, a metal block), each with a small glowing floating symbol above it made of gold pixels: a clock, a coin, and a door. They represent places where things happen. Simple and iconic.
```

## `feat-staff` · Pannello staff · priorità 3
File: `incoming/feat-staff.png` · rapporto 1:1

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (192x192 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 1:1.

A blocky gold shield in front of a floating translucent light-blue panel made of flat pixel rectangles with rows of small bars (no text), a small blocky hand-held tablet shape in the corner. It suggests a control panel. Simple and iconic.
```

## `start-1` · Installa · priorità 3
File: `incoming/start-1.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

A small blocky treasure chest with a gold ribbon, lid open, with a glowing square file icon (a plain cube) hovering above it, and a wooden shelf labeled only by a folder shape (no text). It suggests dropping one file into a folder. Deep navy background.
```

## `start-2` · Avvia · priorità 3
File: `incoming/start-2.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

A blocky server cabinet with rows of small lit lights, one big green power light just turned on, a few gold pixel sparks around it, deep navy background.
```

## `start-3` · Crea il primo lavoro · priorità 3
File: `incoming/start-3.png` · rapporto 4:3

```text
Pixel art illustration in the exact style of the attached reference image: chunky, blocky Minecraft-like characters (square head, rectangular torso, straight rectangular arms and legs), seen from the side in flat 2D, thick near-black navy outlines (#14101f), flat colors with two or three tones per color (base, light, shadow) and the light coming from the top left. Crisp square pixels only: draw it as if on a very low resolution pixel canvas (240x180 pixel canvas) upscaled with nearest-neighbor, so every single pixel block is clearly visible and the pixel grid is perfectly regular. No anti-aliasing, no blur, no gradients, no soft shadows, no dithering noise, no painterly texture, no glow effects made of smooth falloff (glows are made of a few flat pixel bands). Limited palette of about 24 colors dominated by deep navy night (#0d1526 and #15213a), uniform blue (#2f5fb3, light #5b8def), gold (#f6c634, shadow #b88416), warm off-white (#f2efe6), plus small accents of skin tones, brown, and black. Night-time but readable, moody and a little humorous. Absolutely no text, no letters, no numbers, no logos, no watermark, no frame or border. Aspect ratio 4:3.

THE OFFICER: a blocky police officer with a pale skin tone and short brown hair, a flat navy-blue peaked cap with a black visor and a small gold badge on the front, a navy-blue uniform shirt (#2f5fb3 with darker blue shading), gold shoulder patches, white cuffs, a black belt with a small gold buckle, dark blue trousers, brown boots and a short black baton. Angry, determined face with a thick frowning eyebrow and gritted teeth.

A blocky officer in a navy uniform standing next to a large parchment scroll that shows a vertical list of three small badge icons of increasing size on the left and horizontal bars on the right (no text). It suggests a job with ranks. Deep navy background.
```

