import { $ } from './lib';

/**
 * Mostra l'ultima release da GitHub, se esiste. Nessun token, una sola richiesta;
 * in caso di errore, rate limit o repository privata il badge resta nascosto.
 */
export async function initRelease() {
  const li = $('[data-release]');
  const data = $('#site-data');
  if (!li || !data) return;
  const { repo } = JSON.parse(data.textContent ?? '{}') as { repo: string | null };
  if (!repo) return;
  try {
    const ctl = new AbortController();
    const to = window.setTimeout(() => ctl.abort(), 4000);
    const res = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, { signal: ctl.signal, headers: { Accept: 'application/vnd.github+json' } });
    window.clearTimeout(to);
    if (!res.ok) return;
    const json = (await res.json()) as { tag_name?: string; html_url?: string };
    if (!json.tag_name) return;
    const a = li.querySelector('a')!;
    a.textContent = json.tag_name;
    if (json.html_url?.startsWith('https://github.com/')) a.href = json.html_url;
    li.hidden = false;
  } catch { /* offline o bloccato: la pagina funziona senza */ }
}
