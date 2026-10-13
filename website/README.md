# Sito di QRM

Sito statico di QRM, costruito con [Astro](https://astro.build/): la pagina di presentazione e, sotto `/docs/`, la wiki fatta con [Starlight](https://starlight.astro.build/). È indipendente dal progetto Gradle: non tocca i moduli del mod, legge soltanto `../gradle.properties` per mostrare versione di QRM, Minecraft e NeoForge.

Le pagine della wiki sono file Markdown in `src/content/docs/docs/`. Le decisioni editoriali e il piano sono in `../docs/superpowers/specs/2026-10-13-wiki-starlight-design.md`. La pagina di presentazione linka la wiki con `docs('percorso')` (`src/config/links.ts`); `scripts/check-docs.mjs` verifica che ogni percorso esista. Le sezioni per sviluppatori puntano ancora alla wiki precedente (`oldDocs`, repository `QRM-docs`), perché la nuova non ha quelle pagine.

## Comandi

Serve Node 22.12 o successivo.

```bash
npm install
npm run dev       # anteprima con ricarica
npm run build     # controllo dei tipi + build in dist/; un link interno rotto nella wiki fa fallire la build
npm run preview   # serve dist/
npm run og        # rigenera public/og.png
python scripts/make-textures.py   # rigenera le texture generate (serve Pillow)
```

## Configurazione

| Cosa | Dove |
|---|---|
| Link pubblici (repo, issues, release, wiki) | `src/config/links.ts` |
| Nome, descrizione, versioni, repository per il badge release | `src/config/site.ts` |
| Testi, comandi, parti dell'architettura, blocchi della scena | `src/data/content.ts` |
| Colori, tipografia, spaziature, durate e easing | `src/styles/tokens.css` |
| URL e percorso di pubblicazione | variabili `SITE_URL` e `SITE_BASE` (predefiniti: `https://maurovitooff.github.io` e `/Quorum`) |

Da confermare prima della pubblicazione: l'URL finale del sito, l'esistenza di release su GitHub (il pulsante «Release su GitHub» e il badge in hero funzionano solo se la repository è pubblica e ha una release; altrimenti il badge resta nascosto).

## Struttura

```text
src/
  config/      site.ts, links.ts (client-safe), md.ts
  content/     docs/docs/: le pagine della wiki (Starlight); la sidebar è in astro.config.mjs
  data/        content.ts (sviluppatori, comandi), owner.ts (contenuti per i server owner)
  layouts/     Base.astro: meta, Open Graph, motion
  components/  una sezione per file (Hero, Story, Owner, BankBlock, Features, ...)
  scripts/     un modulo per interazione, importati da main.ts
  styles/      tokens.css, base.css; gli stili di sezione stanno nel componente
public/        favicon, og.png, texture originali di QRM (pixel/)
```

## Stili della pagina di presentazione e Starlight

Starlight imposta `scopedStyleStrategy: 'where'` per tutto il progetto: gli stili con scope dei componenti non aggiungono più specificità. Una regola di un componente che deve vincere su una regola globale di `base.css` va quindi scritta con una specificità sufficiente (vedi i commenti in `Story.astro` e `Requirements.astro`). Riportare la strategia a `attribute` rompe alcuni dettagli dell'interfaccia di Starlight.

## Principi

- Senza JavaScript la pagina è completa e leggibile: il reveal allo scroll si attiva solo se JS gira e l'utente non ha chiesto movimento ridotto.
- Nessun renderer 3D: la scena è SVG calcolato al build. Le animazioni continue (Module che fluttua, nuvole, oggetti che ruotano) si fermano fuori viewport e a scheda in background, e con movimento ridotto non partono.
- Le animazioni dimostrative (flusso di un pagamento, blocco ATM) sono dichiarate come esempi nella pagina e non simulano il comportamento del mod.
- Le texture in `public/pixel/` sono quelle originali di QRM (Apache-2.0), copiate da `qrm-neoforge` e `qrm-bank`. Nessun asset di terzi.
- Le animazioni «alla Minecraft» (oggetti che ruotano e ondeggiano, nuvole a blocchi, il blocco ATM che si rompe, la barra rapida delle funzioni) usano le texture originali di QRM e blocchi di terreno disegnati per il sito con `scripts/make-textures.py`. Non sono texture di Minecraft.

QRM non è affiliato a Mojang Studios né a Microsoft.
