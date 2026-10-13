/** Interruttore chiaro/scuro: la scelta si salva con la stessa chiave della wiki (Starlight). */
export function initTheme() {
  const btn = document.querySelector<HTMLButtonElement>('[data-theme-toggle]');
  if (!btn) return;
  btn.addEventListener('click', () => {
    const next = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
    document.documentElement.dataset.theme = next;
    try { localStorage.setItem('starlight-theme', next); } catch { /* senza storage vale solo per la sessione */ }
  });
}
