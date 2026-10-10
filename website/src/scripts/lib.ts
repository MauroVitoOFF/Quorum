/** Utilità condivise dagli script della pagina. */

export const $ = <T extends Element = HTMLElement>(sel: string, root: ParentNode = document) => root.querySelector<T>(sel);
export const $$ = <T extends Element = HTMLElement>(sel: string, root: ParentNode = document) => [...root.querySelectorAll<T>(sel)];

const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
/** True se l'utente ha chiesto meno movimento (sistema) o lo ha disattivato dal sito (/motion off). */
export const reducedMotion = () => mq.matches || document.documentElement.dataset.motion === 'off';

/** True se il focus è in un campo dove la tastiera deve restare dell'utente. */
export function isTyping(t: EventTarget | null): boolean {
  const el = t as HTMLElement | null;
  if (!el || !el.tagName) return false;
  return el.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName);
}

/** Esegue `fn` quando l'elemento è visibile; la smette quando esce. Restituisce lo stop. */
export function whenVisible(el: Element, onChange: (visible: boolean) => void, margin = '120px'): () => void {
  const io = new IntersectionObserver((es) => onChange(es[es.length - 1].isIntersecting), { rootMargin: margin });
  io.observe(el);
  return () => io.disconnect();
}
