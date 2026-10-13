/** Link pubblici. Modulo senza dipendenze da Node: importabile anche dal browser. */
export const links = {
  repo: 'https://github.com/MauroVitoOFF/Quorum',
  issues: 'https://github.com/MauroVitoOFF/Quorum/issues',
  releases: 'https://github.com/MauroVitoOFF/Quorum/releases',
  license: 'https://github.com/MauroVitoOFF/Quorum/blob/main/LICENSE',
  contributing: 'https://github.com/MauroVitoOFF/Quorum/blob/main/CONTRIBUTING.md',
  security: 'https://github.com/MauroVitoOFF/Quorum/blob/main/SECURITY.md',
  /** La wiki: stesso sito, sotto /docs/. */
  docs: `${import.meta.env.BASE_URL.replace(/\/$/, '')}/docs/`,
  docsRepo: 'https://github.com/MauroVitoOFF/Quorum/tree/main/website/src/content/docs/docs',
  /** La wiki precedente: resta online per le pagine per sviluppatori, che la nuova non ha. */
  oldDocs: 'https://maurovitooff.github.io/QRM-docs/'
};

/** Link a una pagina della wiki; il percorso è quello del file in src/content/docs/docs ('' = pagina iniziale). */
export const docs = (path: string) => `${links.docs}${path.replace(/^\//, '')}`;

/** Link a una pagina della wiki precedente. */
export const oldDocs = (path: string) => `${links.oldDocs}${path.replace(/^\//, '')}`;
