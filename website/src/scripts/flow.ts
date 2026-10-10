import { $, $$, reducedMotion } from './lib';

/**
 * Esempio animato (non è il mod): la banconota percorre le quattro tappe di un pagamento
 * e il risultato cambia a seconda dello scenario scelto.
 */
type Scn = 'ok' | 'stop' | 'dup';
const TEXT: Record<Scn, { t: string; d: string }> = {
  ok: { t: 'Pagamento completato.', d: 'Nessun mod si è opposto: i soldi si sono spostati e chi ascoltava è stato avvisato.' },
  stop: { t: 'Pagamento annullato.', d: 'Un mod ha detto no nel controllo iniziale. Nessun soldo si è mosso e chi aveva chiesto il pagamento riceve l’esito «annullato», con il nome del mod e il motivo.' },
  dup: { t: 'Richiesta ignorata.', d: 'Quel pagamento era già stato eseguito: il registro riconosce la richiesta ripetuta e non addebita due volte. Nessun doppio addebito.' },
};
// stato di ogni tappa per scenario: on = riuscita, stop = bloccata, dup = già fatta, skip = non raggiunta
const PLAN: Record<Scn, Record<string, string>> = {
  ok: { ask: 'on', pre: 'on', write: 'on', post: 'on' },
  stop: { ask: 'on', pre: 'stop', write: 'skip', post: 'skip' },
  dup: { ask: 'on', pre: 'skip', write: 'dup', post: 'skip' },
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
    Object.values(steps).forEach((s) => s.classList.remove('is-on', 'is-stop', 'is-dup', 'is-skip'));
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
        if (st === 'on' || st === 'stop' || st === 'dup') reached = i;
        coin?.style.setProperty('--p', String(reached));
        coin?.classList.toggle('is-hidden', st === 'stop' || st === 'dup');
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
