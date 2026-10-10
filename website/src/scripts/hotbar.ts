import { $, $$ } from './lib';

/** Barra rapida delle funzioni: schede accessibili (frecce, Home/End, tasti 1–5 con il focus sulla barra). */
export function initHotbar() {
  const bar = $('[data-hotbar]');
  const wrap = $('[data-panels]');
  if (!bar || !wrap) return;
  const tabs = $$<HTMLButtonElement>('[role=tab]', bar);
  const panels = $$<HTMLElement>('[data-panel]', wrap);
  document.documentElement.classList.add('js-tabs');

  const select = (i: number, focus = false) => {
    tabs.forEach((t, k) => {
      const on = k === i;
      t.setAttribute('aria-selected', String(on));
      t.tabIndex = on ? 0 : -1;
    });
    panels.forEach((p, k) => {
      p.hidden = k !== i;
      p.classList.remove('is-in');
    });
    void panels[i].offsetWidth;
    panels[i].classList.add('is-in');
    if (focus) tabs[i].focus();
  };

  tabs.forEach((t, i) => {
    t.addEventListener('click', () => select(i));
    t.addEventListener('keydown', (e) => {
      const n = tabs.length;
      const next: Record<string, number> = { ArrowRight: (i + 1) % n, ArrowLeft: (i + n - 1) % n, Home: 0, End: n - 1 };
      if (e.key in next) { e.preventDefault(); select(next[e.key], true); return; }
      const d = Number(e.key);
      if (d >= 1 && d <= n && !e.ctrlKey && !e.metaKey && !e.altKey) { e.preventDefault(); select(d - 1, true); }
    });
  });
  select(0);
}
