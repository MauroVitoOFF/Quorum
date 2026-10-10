import { $, $$ } from './lib';

/** Filtro e ricerca dell'indice dei comandi. Nulla viene eseguito. */
export function initCommands() {
  const list = $('[data-cmd-list]');
  const q = $<HTMLInputElement>('[data-cmd-q]');
  const empty = $('[data-cmd-empty]');
  const count = $('[data-cmd-count]');
  if (!list || !q || !empty || !count) return;
  const rows = $$('li', list);
  const chips = $$<HTMLButtonElement>('.chip');
  let area = '';

  const apply = () => {
    const term = q.value.trim().toLowerCase();
    let n = 0;
    for (const r of rows) {
      const ok = (!area || r.dataset.area === area) && (!term || (r.dataset.hay ?? '').includes(term));
      r.hidden = !ok;
      if (ok) n++;
    }
    empty.hidden = n > 0;
    count.textContent = `${n} ${n === 1 ? 'comando' : 'comandi'} su ${rows.length}`;
  };
  q.addEventListener('input', apply);
  chips.forEach((c) => c.addEventListener('click', () => {
    area = c.dataset.area ?? '';
    chips.forEach((o) => o.setAttribute('aria-pressed', String(o === c)));
    apply();
  }));
  apply();
}
