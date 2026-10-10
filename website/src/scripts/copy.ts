import { $, $$ } from './lib';

/** Pulsanti "Copia": clipboard API con fallback, feedback visivo e annuncio per screen reader. */
async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch { /* si passa al fallback */ }
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.setAttribute('readonly', '');
  ta.style.cssText = 'position:fixed;top:0;left:0;opacity:0';
  document.body.append(ta);
  ta.select();
  let ok = false;
  try { ok = document.execCommand('copy'); } catch { ok = false; }
  ta.remove();
  return ok;
}

export function initCopy() {
  const status = $('[data-copy-status]') ?? (() => {
    const s = document.createElement('p');
    s.className = 'sr-only';
    s.setAttribute('role', 'status');
    s.dataset.copyStatus = '';
    document.body.append(s);
    return s;
  })();
  $$<HTMLButtonElement>('[data-copy]').forEach((btn) => {
    const label = btn.querySelector('span') ?? btn;
    const original = label.textContent ?? 'Copia';
    let t = 0;
    btn.addEventListener('click', async () => {
      const ok = await copyText(btn.dataset.copy ?? '');
      label.textContent = ok ? 'Copiato' : 'Errore';
      btn.classList.toggle('is-done', ok);
      status.textContent = ok ? 'Copiato negli appunti' : 'Copia non riuscita: seleziona il testo e copialo a mano';
      window.clearTimeout(t);
      t = window.setTimeout(() => { label.textContent = original; btn.classList.remove('is-done'); status.textContent = ''; }, 1800);
    });
  });
}
