import { $, $$, reducedMotion, whenVisible } from './lib';
import { inline } from '../config/md';
import { blocks } from '../data/content';
import { docs } from '../config/links';

/** Hero: selezione dei blocchi, parallasse leggero, pausa fuori viewport, ciclo giorno/notte. */
export function initScene() {
  const hero = $('[data-hero]');
  const svg = $<SVGSVGElement>('[data-scene]');
  const cap = $('[data-caption]');
  if (!hero || !svg || !cap) return;

  const kindLabel = { core: 'Core', framework: 'Framework', module: 'Module · mod separato' } as const;
  const els = $$<SVGGElement>('[data-block]', svg);

  function select(id: string, announce = true) {
    const b = blocks.find((x) => x.id === id);
    if (!b) return;
    els.forEach((g) => g.setAttribute('aria-pressed', String(g.dataset.block === id)));
    cap!.dataset.kind = b.kind;
    $('[data-cap-kind]', cap!)!.textContent = kindLabel[b.kind];
    $('[data-cap-title]', cap!)!.textContent = b.title;
    $('[data-cap-text]', cap!)!.innerHTML = inline(b.text);
    $<HTMLAnchorElement>('[data-cap-link]', cap!)!.href = docs(b.doc);
    if (announce && !reducedMotion()) { cap!.classList.remove('is-swap'); void cap!.offsetWidth; cap!.classList.add('is-swap'); }
  }

  els.forEach((g) => {
    g.addEventListener('click', () => select(g.dataset.block!));
    g.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); select(g.dataset.block!); }
    });
  });
  select('core', false);
  els[0]?.closest('[data-scene]')?.classList.add('scene-ready');
  document.documentElement.classList.add('scene-ready');

  // parallasse: i piani si spostano di poco con il puntatore; solo transform, un frame per volta
  const layers = $$<SVGGElement>('.layer', svg);
  let raf = 0, tx = 0, ty = 0;
  const apply = () => {
    raf = 0;
    layers.forEach((l) => {
      const d = Number(l.dataset.depth ?? 0);
      l.style.transform = `translate(${(tx * d).toFixed(2)}px, ${(ty * d).toFixed(2)}px)`;
    });
  };
  const fine = window.matchMedia('(hover: hover) and (pointer: fine)');
  const onMove = (e: PointerEvent) => {
    if (reducedMotion() || !fine.matches) return;
    const r = hero.getBoundingClientRect();
    tx = ((e.clientX - r.left) / r.width - 0.5) * 2;
    ty = ((e.clientY - r.top) / r.height - 0.5) * 2;
    if (!raf) raf = requestAnimationFrame(apply);
  };
  hero.addEventListener('pointermove', onMove, { passive: true });
  hero.addEventListener('pointerleave', () => { tx = ty = 0; if (!raf) raf = requestAnimationFrame(apply); });

  // animazione continua del Module: ferma fuori dallo schermo e a scheda in background
  let visible = true;
  const sync = () => {
    const p = !visible || document.hidden;
    svg.classList.toggle('paused', p);
    hero.classList.toggle('paused', p);
  };
  whenVisible(svg, (v) => { visible = v; sync(); });
  document.addEventListener('visibilitychange', sync);
}
