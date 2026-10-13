import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightLinksValidator from 'starlight-links-validator';

// SITE_URL e SITE_BASE si possono sovrascrivere in CI. I valori predefiniti
// sono quelli di GitHub Pages per la repository: vedi src/config/site.ts.
const site = process.env.SITE_URL ?? 'https://maurovitooff.github.io';
const base = process.env.SITE_BASE ?? '/Quorum';

export default defineConfig({
  site,
  base,
  trailingSlash: 'ignore',
  integrations: [
    // La wiki, sotto /docs/. La pagina iniziale e la 404 restano quelle del sito (src/pages/).
    starlight({
      title: 'QRM',
      locales: { root: { label: 'Italiano', lang: 'it' } },
      disable404Route: true,
      social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/MauroVitoOFF/Quorum' }],
      editLink: { baseUrl: 'https://github.com/MauroVitoOFF/Quorum/edit/main/website/' },
      customCss: ['./src/styles/docs.css'],
      // L'ordine è editoriale: le guide stanno prima dei sistemi.
      sidebar: [
        {
          label: 'Per iniziare',
          items: [
            { label: "Cos'è QRM", slug: 'docs' },
            'docs/per-iniziare/installazione',
            'docs/per-iniziare/primo-avvio',
          ],
        },
        {
          label: 'Guide',
          items: ['docs/guide/creare-un-lavoro', 'docs/guide/accesso-staff'],
        },
        {
          label: 'Sistemi',
          items: [
            'docs/sistemi/personaggi',
            'docs/sistemi/economia',
            'docs/sistemi/lavori',
            'docs/sistemi/organizzazioni',
            'docs/sistemi/permessi',
          ],
        },
      ],
      // Un link interno rotto fa fallire la build.
      plugins: [starlightLinksValidator()],
    }),
  ],
  build: { inlineStylesheets: 'auto' },
});
