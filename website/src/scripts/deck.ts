import { $$ } from './lib';

/**
 * Rettangolo a pagine (`data-deck`): i pallini (`role="tab"`) mostrano una pagina (`role="tabpanel"`) alla volta.
 * Tastiera come i tab di ARIA: frecce, Home e Fine; il tocco scorre le pagine con uno swipe orizzontale.
 * Senza questo script le pagine restano una sotto l'altra: la classe `is-paged` è ciò che le impagina.
 */
export function initDeck() {
  const wide = window.matchMedia('(min-width: 860px)');

  $$('[data-deck]').forEach((deck) => {
    const tabs = $$<HTMLButtonElement>('[role="tab"]', deck);
    const pages = $$('[role="tabpanel"]', deck);
    if (tabs.length < 2 || tabs.length !== pages.length) return;

    let current = 0;
    const show = (index: number, focus = false) => {
      current = (index + tabs.length) % tabs.length;
      tabs.forEach((tab, i) => {
        const on = i === current;
        tab.setAttribute('aria-selected', String(on));
        tab.tabIndex = on ? 0 : -1;
      });
      pages.forEach((page, i) => page.classList.toggle('is-current', i === current));
      if (focus) tabs[current].focus();
    };

    const orient = () => $$('[role="tablist"]', deck).forEach((l) => l.setAttribute('aria-orientation', wide.matches ? 'vertical' : 'horizontal'));
    orient();
    wide.addEventListener('change', orient);

    deck.classList.add('is-paged');
    show(0);

    tabs.forEach((tab, i) => {
      tab.addEventListener('click', () => show(i));
      tab.addEventListener('keydown', (e) => {
        const next: Record<string, number> = {
          ArrowDown: current + 1, ArrowRight: current + 1, ArrowUp: current - 1, ArrowLeft: current - 1,
          Home: 0, End: tabs.length - 1,
        };
        if (!(e.key in next)) return;
        e.preventDefault();
        show(next[e.key], true);
      });
    });

    // swipe: un gesto orizzontale deciso cambia pagina, uno verticale resta lo scorrimento della pagina
    let x0 = 0;
    let y0 = 0;
    deck.addEventListener('touchstart', (e) => { x0 = e.touches[0].clientX; y0 = e.touches[0].clientY; }, { passive: true });
    deck.addEventListener('touchend', (e) => {
      const dx = e.changedTouches[0].clientX - x0;
      const dy = e.changedTouches[0].clientY - y0;
      if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.5) show(current + (dx < 0 ? 1 : -1));
    }, { passive: true });
  });
}
