import { $$ } from './lib';

/** Reveal allo scroll: una sola volta per elemento, poi l'observer lo lascia. */
export function initReveal() {
  const root = document.documentElement;
  const els = $$('[data-reveal]');
  if (!root.classList.contains('js-motion') || !('IntersectionObserver' in window)) {
    els.forEach((e) => e.classList.add('is-in'));
    return;
  }
  const io = new IntersectionObserver((entries) => {
    for (const e of entries) {
      if (!e.isIntersecting) continue;
      e.target.classList.add('is-in');
      io.unobserve(e.target);
    }
  }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
  els.forEach((e) => io.observe(e));
}
