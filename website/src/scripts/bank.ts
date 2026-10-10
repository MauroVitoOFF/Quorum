import { $, reducedMotion } from './lib';

/**
 * Animazione decorativa: il blocco ATM si crepa, si rompe e lascia cadere banconote
 * che ruotano come gli oggetti a terra di Minecraft. Poi il blocco ricompare.
 * Non simula il mod: serve solo a dare un'idea del blocco.
 */
const PARTICLES = ['#8a9199', '#5e6670', '#3ec9e6', '#b4c3d3', '#2a3340'];

export function initBank() {
  const stage = $('[data-bank-stage]');
  const btn = $<HTMLButtonElement>('[data-break]');
  const block = $('[data-block-el]');
  const crack = $('[data-crack-el]');
  const items = $('[data-items]');
  const status = $('[data-break-status]');
  if (!stage || !btn || !block || !crack || !items || !status) return;

  crack.style.backgroundImage = `url(${stage.dataset.crack})`;
  const say = (t: string) => { status.textContent = t; };
  const later = (fn: () => void, ms: number) => window.setTimeout(fn, ms);
  let busy = false;

  function drop(n: number) {
    const els: HTMLElement[] = [];
    for (let i = 0; i < n; i++) {
      const it = document.createElement('div');
      it.className = 'it';
      const img = document.createElement('img');
      img.src = (block!.querySelector('img') as HTMLImageElement).src.replace('atm.png', 'cash.png');
      img.alt = '';
      const sh = document.createElement('span');
      sh.className = 'sh';
      it.append(sh, img);
      items!.append(it);
      els.push(it);
      const dx = (i - (n - 1) / 2) * 56 + (Math.random() - 0.5) * 16;
      it.animate([
        { transform: 'translate(0, -70px) scale(.6)', opacity: 0, offset: 0 },
        { transform: `translate(${dx * 0.6}px, -150px) scale(1)`, opacity: 1, offset: 0.35, easing: 'cubic-bezier(.5,0,1,.6)' },
        { transform: `translate(${dx}px, 0)`, offset: 0.7 },
        { transform: `translate(${dx}px, -22px)`, offset: 0.85, easing: 'cubic-bezier(.5,0,1,.6)' },
        { transform: `translate(${dx}px, 0)`, offset: 1 },
      ], { duration: 1100, delay: i * 90, fill: 'forwards' });
    }
    return els;
  }

  function particles() {
    const r = block!.getBoundingClientRect(), s = stage!.getBoundingClientRect();
    for (let i = 0; i < 16; i++) {
      const p = document.createElement('i');
      p.className = 'px';
      p.style.cssText = `left:${r.left - s.left + r.width / 2}px;top:${r.top - s.top + r.height / 2}px;background:${PARTICLES[i % PARTICLES.length]}`;
      stage!.append(p);
      const a = (Math.PI * 2 * i) / 16;
      const anim = p.animate([
        { transform: 'translate(0,0)', opacity: 1 },
        { transform: `translate(${Math.cos(a) * (50 + Math.random() * 60)}px, ${Math.sin(a) * 40 + 90}px)`, opacity: 0 },
      ], { duration: 700 + Math.random() * 300, easing: 'cubic-bezier(.2,.7,.4,1)', fill: 'forwards' });
      anim.finished.then(() => p.remove(), () => p.remove());
    }
  }

  function respawn() {
    items!.replaceChildren();
    stage!.querySelectorAll('.px').forEach((p) => p.remove());
    block!.classList.remove('is-gone');
    crack!.style.opacity = '0';
    if (!reducedMotion()) { block!.classList.add('is-back'); later(() => block!.classList.remove('is-back'), 400); }
    say('Il blocco ATM è tornato al suo posto.');
    btn!.disabled = false;
    busy = false;
  }

  btn.addEventListener('click', () => {
    if (busy) return;
    busy = true;
    btn.disabled = true;
    if (reducedMotion()) {
      block.classList.add('is-gone');
      say('Blocco rotto: cadono delle banconote.');
      drop(3).forEach((e) => e.getAnimations().forEach((a) => a.finish()));
      later(respawn, 2200);
      return;
    }
    say('Si sta rompendo…');
    block.classList.add('is-shaking');
    crack.style.opacity = '1';
    for (let i = 0; i < 10; i++) later(() => { crack.style.backgroundPosition = `${(i / 9) * 100}% 0`; }, i * 110);
    later(() => {
      block.classList.remove('is-shaking');
      block.classList.add('is-gone');
      particles();
      drop(3);
      say('Blocco rotto: cadono delle banconote.');
    }, 1150);
    later(() => {
      // raccolta: gli oggetti volano verso l'alto e svaniscono
      items.querySelectorAll<HTMLElement>('.it').forEach((it, i) => {
        it.animate([{ opacity: 1 }, { opacity: 0, translate: '0 -90px' }], { duration: 350, delay: i * 80, fill: 'forwards' });
      });
    }, 3100);
    later(respawn, 3800);
  });
}
