import { $, $$, reducedMotion } from './lib';
import { inline } from '../config/md';

interface Part { id: string; name: string; where: 'jar' | 'module'; role: string; detail: string; install: string }

/** Diagramma dell'architettura: seleziona una parte e ne mostra il dettaglio. */
export function initArch() {
  const root = $('[data-arch]');
  const data = $('#arch-data');
  const panel = $('[data-arch-panel]');
  if (!root || !data || !panel) return;
  const parts: Part[] = JSON.parse(data.textContent ?? '[]');
  const cells = $$<HTMLButtonElement>('[data-part]', root);

  const show = (id: string) => {
    const p = parts.find((x) => x.id === id);
    if (!p) return;
    cells.forEach((c) => c.setAttribute('aria-pressed', String(c.dataset.part === id)));
    panel.dataset.where = p.where;
    $('[data-p-where]', panel)!.textContent = p.where === 'jar' ? 'Dentro il jar qrm' : 'Module · mod separato';
    $('[data-p-name]', panel)!.textContent = p.name;
    $('[data-p-role]', panel)!.textContent = p.role;
    $('[data-p-detail]', panel)!.innerHTML = inline(p.detail);
    $('[data-p-install]', panel)!.innerHTML = inline(p.install);
    if (!reducedMotion()) {
      panel.classList.remove('is-swap');
      void panel.offsetWidth;
      panel.classList.add('is-swap');
    }
  };
  cells.forEach((c) => c.addEventListener('click', () => show(c.dataset.part!)));
}
