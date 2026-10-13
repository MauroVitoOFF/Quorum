/** Link pubblici. Modulo senza dipendenze da Node: importabile anche dal browser. */
export const links = {
  repo: 'https://github.com/MauroVitoOFF/Quorum',
  issues: 'https://github.com/MauroVitoOFF/Quorum/issues',
  license: 'https://github.com/MauroVitoOFF/Quorum/blob/main/LICENSE',
  security: 'https://github.com/MauroVitoOFF/Quorum/blob/main/SECURITY.md',
  /** La wiki: stesso sito, sotto /docs/. */
  docs: `${import.meta.env.BASE_URL.replace(/\/$/, '')}/docs/`
};

/** Link a una pagina della wiki; il percorso è quello del file in src/content/docs/docs ('' = pagina iniziale). */
export const docs = (path: string) => `${links.docs}${path.replace(/^\//, '')}`;
