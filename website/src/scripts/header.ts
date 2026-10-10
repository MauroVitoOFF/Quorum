import { $, $$ } from './lib';

export function initHeader() {
  const burger = $<HTMLButtonElement>('[data-burger]');
  const nav = $('#site-nav');
  if (!burger || !nav) return;
  const mqMobile = window.matchMedia('(max-width: 860px)');

  const set = (open: boolean) => {
    burger.setAttribute('aria-expanded', String(open));
    nav.classList.toggle('is-open', open);
  };
  burger.addEventListener('click', () => set(burger.getAttribute('aria-expanded') !== 'true'));
  nav.addEventListener('click', (e) => { if ((e.target as HTMLElement).closest('a')) set(false); });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && burger.getAttribute('aria-expanded') === 'true') { set(false); burger.focus(); }
  });
  mqMobile.addEventListener('change', () => set(false));

  // voce attiva in base alla sezione visibile
  const links = $$<HTMLAnchorElement>('a[href^="#"]', nav);
  const map = new Map(links.map((a) => [a.getAttribute('href')!.slice(1), a]));
  const io = new IntersectionObserver((entries) => {
    for (const e of entries) {
      if (e.target.id === 'top') { if (e.isIntersecting) links.forEach((l) => l.removeAttribute('aria-current')); continue; }
      const a = map.get(e.target.id);
      if (!a) continue;
      if (e.isIntersecting) { links.forEach((l) => l.removeAttribute('aria-current')); a.setAttribute('aria-current', 'true'); }
    }
  }, { rootMargin: '-45% 0px -50% 0px' });
  [...map.keys(), 'top'].forEach((id) => { const s = document.getElementById(id); if (s) io.observe(s); });
}
