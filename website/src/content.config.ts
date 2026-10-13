import { defineCollection } from 'astro:content';
import { docsLoader } from '@astrojs/starlight/loaders';
import { docsSchema } from '@astrojs/starlight/schema';

// La wiki: le pagine stanno in src/content/docs/docs/, quindi escono sotto /docs/.
export const collections = {
  docs: defineCollection({ loader: docsLoader(), schema: docsSchema() }),
};
