/** Link pubblici. Modulo senza dipendenze da Node: importabile anche dal browser. */
export const links = {
  repo: 'https://github.com/MauroVitoOFF/Quorum',
  issues: 'https://github.com/MauroVitoOFF/Quorum/issues',
  releases: 'https://github.com/MauroVitoOFF/Quorum/releases',
  license: 'https://github.com/MauroVitoOFF/Quorum/blob/main/LICENSE',
  contributing: 'https://github.com/MauroVitoOFF/Quorum/blob/main/CONTRIBUTING.md',
  security: 'https://github.com/MauroVitoOFF/Quorum/blob/main/SECURITY.md',
  docs: 'https://maurovitooff.github.io/QRM-docs/',
  docsRepo: 'https://github.com/MauroVitoOFF/QRM-docs'
};

/** Deep link verso pagine della wiki (pubblica). */
export const docs = (path: string) => `${links.docs}${path.replace(/^\//, '')}`;
