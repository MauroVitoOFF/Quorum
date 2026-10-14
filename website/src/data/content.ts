/**
 * Contenuti del sito: i Modules. Ogni affermazione viene dal codice (versione 0.20.x)
 * ed è spiegata per esteso nella wiki. Il lessico è quello della wiki: personaggio, lavoro, organizzazione, permesso.
 */

/* ---------- QRM e i suoi Modules ---------- */

export interface ModuleCard {
  id: string;
  name: string;
  /** illustrazione in public/art/ (senza estensione né larghezza), oppure null per il logo di QRM */
  art: string | null;
  need: 'Necessario' | 'Facoltativo';
  text: string;
  /** dove va installato */
  where: string;
  doc: string;
  link: string;
}

export const modules: ModuleCard[] = [
  {
    id: 'qrm', name: 'QRM', art: null, need: 'Necessario',
    text: 'Personaggi, economia, lavori, organizzazioni, permessi, punti fisici nel mondo e pannello staff.',
    where: 'Obbligatorio sul server. Consigliato sui client per abilitare le interfacce grafiche.',
    doc: 'per-iniziare/installazione/', link: 'Installazione',
  },
  {
    id: 'bank', name: 'Bank', art: 'module-bank', need: 'Facoltativo',
    text: 'Sportelli ATM posizionabili nel mondo, banconote fisiche nell’inventario e bonifici con causale.',
    where: 'Sul server e su ogni client.',
    doc: 'modules/bank/', link: 'Bank',
  },
  {
    id: 'locker', name: 'Locker', art: 'module-locker', need: 'Facoltativo',
    text: 'Un armadietto personale da 27 slot, separato per ogni personaggio, accessibile dai blocchi che scegli tu.',
    where: 'Obbligatorio sul server. Facoltativo sui client.',
    doc: 'modules/locker/', link: 'Locker',
  },
  {
    id: 'boss', name: 'Boss', art: 'module-boss', need: 'Facoltativo',
    text: 'Un’interfaccia dedicata a chi dirige un’attività per assumere, promuovere, licenziare e gestire il conto aziendale.',
    where: 'Sul server e sui client di chi usa il menu.',
    doc: 'modules/boss/', link: 'Boss',
  },
];
