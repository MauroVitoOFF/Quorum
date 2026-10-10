/**
 * Configurazione centrale del sito: identità, link pubblici, versioni.
 * Ogni valore che compare nella pagina e può cambiare vive qui.
 *
 * Fonti:
 *  - `qrm_version`, `minecraft_version`, `neo_version` sono letti da ../gradle.properties
 *    al momento della build, quindi non possono divergere dal mod.
 *  - Java 25 e le regole di compatibilità vengono dalla wiki (Requisiti).
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

function gradleProps(): Record<string, string> {
  const file = fileURLToPath(new URL('../../../gradle.properties', import.meta.url));
  const out: Record<string, string> = {};
  for (const line of readFileSync(file, 'utf8').split(/\r?\n/)) {
    const m = /^\s*([\w.]+)\s*=\s*(.*?)\s*$/.exec(line);
    if (m) out[m[1]] = m[2];
  }
  return out;
}

import { links, docs } from './links';

const props = gradleProps();

export const site = {
  name: 'QRM',
  fullName: 'QRM (Quorum)',
  tagline: 'Le fondamenta per mondi roleplay su NeoForge',
  description:
    'QRM (Quorum) è un framework NeoForge per server Minecraft di roleplay: Characters, Economy, Jobs, Organizations, Permissions e un’API per scrivere Modules.',
  lang: 'it',
  license: 'Apache-2.0',

  /** URL pubblico del sito. Sovrascrivibile con SITE_URL / SITE_BASE in build. */
  url: (process.env.SITE_URL ?? 'https://maurovitooff.github.io') + (process.env.SITE_BASE ?? '/QRM'),

  version: props.qrm_version ?? '0.0.0',
  minecraft: props.minecraft_version ?? '',
  neoforge: props.neo_version ?? '',
  java: '25',

  links,

  /**
   * Repository usata per il badge "ultima release" (GitHub REST API, senza token).
   * Se la richiesta fallisce o la repo è privata il badge non compare.
   * Metti `null` per disattivare del tutto la chiamata.
   */
  githubRepo: 'MauroVitoOFF/QRM' as string | null,
};

export { docs };

/** Prefisso del base path di Astro, per gli asset in public/. */
export const asset = (path: string) =>
  `${import.meta.env.BASE_URL.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
