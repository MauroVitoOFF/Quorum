import { $, $$, reducedMotion } from './lib';

/**
 * Esempio animato (non è il mod): la banconota percorre le quattro tappe di un pagamento
 * e il risultato cambia a seconda dello scenario scelto.
 */
type Scn = 'ok' | 'stop';
const TEXT: Record<Scn, { t: string; d: string }> = {
  ok: { t: 'Pagamento completato.', d: 'Tutti i controlli sono passati: il denaro si è spostato e la transazione è nel registro.' },
  stop: { t: 'Pagamento rifiutato.', d: 'Il conto di Marco è congelato. Non si è mosso nulla, e Anna legge «Conto congelato.»' },
};
// stato di ogni tappa per scenario: on = riuscita, stop = bloccata, skip = non raggiunta
const PLAN: Record<Scn, Record<string, string>> = {
  ok: { ask: 'on', pre: 'on', write: 'on', post: 'on' },
  stop: { ask: 'on', pre: 'stop', write: 'skip', post: 'skip' },
};
const ORDER = ['ask', 'pre', 'write', 'post'];

export function initFlow() {
  const root = $('[data-flow]');
  if (!root) return;
  const steps = Object.fromEntries($$('[data-st]', root).map((s) => [s.dataset.st!, s]));
  const coin = $('.coin', root);
  const opts = $$<HTMLButtonElement>('[data-scn]', root);
  const result = $('[data-result]', root)!;
  let timers: number[] = [];

  const clear = () => {
    timers.forEach((t) => clearTimeout(t));
    timers = [];
    Object.values(steps).forEach((s) => s.classList.remove('is-on', 'is-stop', 'is-skip'));
  };

  function run(scn: Scn) {
    clear();
    opts.forEach((o) => o.setAttribute('aria-pressed', String(o.dataset.scn === scn)));
    const plan = PLAN[scn];
    const gap = reducedMotion() ? 0 : 650;
    let reached = 0;
    ORDER.forEach((id, i) => {
      timers.push(window.setTimeout(() => {
        const st = plan[id];
        steps[id].classList.add(`is-${st}`);
        if (st === 'on' || st === 'stop') reached = i;
        coin?.style.setProperty('--p', String(reached));
        coin?.classList.toggle('is-hidden', st === 'stop');
      }, i * gap));
    });
    coin?.classList.remove('is-hidden');
    coin?.style.setProperty('--p', '0');
    timers.push(window.setTimeout(() => {
      result.dataset.state = scn;
      $('[data-result-t]', result)!.textContent = TEXT[scn].t;
      $('[data-result-d]', result)!.textContent = TEXT[scn].d;
    }, ORDER.length * gap));
  }

  opts.forEach((o) => o.addEventListener('click', () => run(o.dataset.scn as Scn)));
  run('ok');
}
