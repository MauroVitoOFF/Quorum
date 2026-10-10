import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

// SITE_URL e SITE_BASE si possono sovrascrivere in CI. I valori predefiniti
// sono quelli di GitHub Pages per la repository: vedi src/config/site.ts.
const site = process.env.SITE_URL ?? 'https://maurovitooff.github.io';
const base = process.env.SITE_BASE ?? '/QRM';

export default defineConfig({
  site,
  base,
  trailingSlash: 'ignore',
  integrations: [sitemap()],
  build: { inlineStylesheets: 'auto' },
});
