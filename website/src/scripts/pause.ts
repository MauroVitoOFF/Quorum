import { $$, whenVisible } from './lib';

/**
 * Le sezioni con `data-pause` ricevono la classe `paused` quando sono fuori vista o la scheda
 * è in background: i CSS la usano per fermare le animazioni continue.
 */
export function initPause() {
  $$('[data-pause]').forEach((el) => {
    let visible = true;
    const sync = () => el.classList.toggle('paused', !visible || document.hidden);
    whenVisible(el, (v) => { visible = v; sync(); });
    document.addEventListener('visibilitychange', sync);
  });
}
