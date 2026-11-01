/** Configuratore: interfaccia sopra i moduli di src/lib/config. Nessuna logica di formato qui. */
import { initHeader } from './header';
import { schemaFor, findField } from '../lib/config/schema.mjs';
import {
  ConfigFileError, openFile, newSession, getField, setField, resetField, issues, render, changedLines, downloadName,
} from '../lib/config/session.mjs';
import type { Session } from '../lib/config/session.mjs';
import type { Field } from '../lib/config/schema.mjs';
import type { Issue } from '../lib/config/validate.mjs';

type Value = string | boolean | bigint | number | string[];
const el = <T extends HTMLElement = HTMLElement>(id: string) => document.getElementById(id) as T;
const esc = (s: unknown) =>
  String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c] as string);
const idOf = (p: string) => `in-${p.replace(/\W/g, '_')}`;
const eq = (a: Value, b: Value) =>
  Array.isArray(a) && Array.isArray(b) ? a.length === b.length && a.every((x, i) => x === b[i]) : a === b;
const show = (v: Value) => (Array.isArray(v) ? (v.length ? v.join(', ') : 'vuoto') : typeof v === 'boolean' ? (v ? 'sì' : 'no') : v === '' ? 'vuoto' : String(v));

let session: Session | null = null;
let currentSection = '';
let dirty = false;
/** Testo digitato che non è ancora un valore valido (campo → messaggio). Non entra nel file. */
const bad = new Map<string, string>();
/** Testo digitato nei campi con un valore non valido: sopravvive ai ridisegni, così non si perde nulla senza avviso. */
const drafts = new Map<string, string>();

// ---------- apertura ----------
function confirmDiscard(): boolean {
  return !dirty || window.confirm('Hai modifiche non scaricate: aprendo un altro file andranno perse. Continuare?');
}

function openError(message: string | null) {
  const box = el('open-error');
  box.textContent = message ?? '';
  box.hidden = !message;
  if (message && session) window.alert(message);
}

async function loadFile(file: File) {
  if (!confirmDiscard()) return;
  try {
    startSession(openFile(file.name, await file.text()));
  } catch (e) {
    openError(e instanceof ConfigFileError ? e.message : 'Non riesco a leggere questo file.');
  }
}

function startSession(s: Session) {
  session = s;
  dirty = false;
  bad.clear();
  drafts.clear();
  currentSection = sectionsOf(s)[0].id;
  openError(null);
  el('start').hidden = true;
  el('editor').hidden = false;
  el('file-name').textContent = s.isNew ? `${s.fileName} (nuovo, dai valori predefiniti)` : s.fileName;
  el('schema-note').textContent = `Schema per QRM ${el('main').dataset.version ?? ''}`;
  renderNav();
  renderFields();
  refreshChrome();
  window.scrollTo({ top: 0 });
}

// ---------- sezioni ----------
const CASH_SECTION = { id: 'cash', label: 'Tagli del contante' };
function sectionsOf(s: Session) {
  if (s.id === 'cash') return [CASH_SECTION];
  const list = schemaFor(s.id).sections.map((x: { id: string; label: string }) => ({ id: x.id, label: x.label }));
  if (s.unknownKeys.length) list.push({ id: 'unknown', label: 'Non riconosciute' });
  return list;
}

function allIssues(): Issue[] {
  if (!session) return [];
  return [...issues(session), ...[...bad].map(([path, message]) => ({ path, severity: 'error' as const, message }))];
}

function sectionErrors(secId: string): number {
  if (!session || secId === 'unknown') return 0;
  if (session.id === 'cash') return allIssues().filter((i) => i.severity === 'error').length;
  const paths = new Set(schemaFor(session.id).sections.find((x: { id: string }) => x.id === secId)?.fields.map((f: Field) => f.path));
  return allIssues().filter((i) => i.severity === 'error' && paths.has(i.path)).length;
}

function renderNav() {
  if (!session) return;
  el('sections').innerHTML = sectionsOf(session)
    .map((x) => {
      const n = sectionErrors(x.id);
      return `<button type="button" data-sec="${x.id}" aria-current="${x.id === currentSection}">${esc(x.label)}${n ? `<span class="count" aria-label="${n} errori">${n}</span>` : ''}</button>`;
    })
    .join('');
}

// ---------- campi ----------
function rangeNote(f: Field): string {
  if (f.type !== 'int' && f.type !== 'float') return '';
  const unbounded = f.max === undefined || f.max === 9223372036854775807n;
  if (f.min === undefined) return '';
  return unbounded ? ` Minimo ${f.min}.` : ` Da ${f.min} a ${f.max}.`;
}

function controlHtml(f: Field, v: Value): string {
  const id = idOf(f.path);
  const p = esc(f.path);
  const described = `aria-describedby="d-${id} e-${id}"`;
  switch (f.type) {
    case 'bool':
      return `<label class="switch"><input type="checkbox" role="switch" id="${id}" data-kind="bool" data-path="${p}" ${v ? 'checked' : ''} ${described}><span class="switch__ui"></span><span class="switch__txt">${v ? 'Attivo' : 'Disattivato'}</span></label>`;
    case 'int':
    case 'float':
      return `<input type="text" id="${id}" data-kind="${f.type}" data-path="${p}" inputmode="${f.type === 'int' ? 'numeric' : 'decimal'}" value="${esc(drafts.get(f.path) ?? v)}" ${described}>`;
    case 'string':
    case 'regex':
      return `<input type="text" id="${id}" class="${f.type === 'regex' ? 'mono' : ''}" data-kind="str" data-path="${p}" value="${esc(v)}" autocomplete="off" spellcheck="false" ${described}>`;
    case 'list': {
      const items = v as string[];
      const free = !f.onlyKnown;
      const left = (f.knownItems ?? []).filter((k) => !items.includes(k));
      const rows = items
        .map((it, i) => `<li class="row"><input type="text" class="mono" data-kind="item" data-path="${p}" data-idx="${i}" value="${esc(it)}" aria-label="Voce ${i + 1}" spellcheck="false">
          <span class="row__btns">
            <button type="button" class="ibtn" data-act="up" data-path="${p}" data-idx="${i}" aria-label="Sposta su ${esc(it)}" ${i === 0 ? 'disabled' : ''}>↑</button>
            <button type="button" class="ibtn" data-act="down" data-path="${p}" data-idx="${i}" aria-label="Sposta giù ${esc(it)}" ${i === items.length - 1 ? 'disabled' : ''}>↓</button>
            <button type="button" class="ibtn" data-act="del" data-path="${p}" data-idx="${i}" aria-label="Rimuovi ${esc(it)}">×</button>
          </span></li>`)
        .join('');
      return `<ol class="rows" id="${id}">${rows}</ol>
        ${items.length === 0 && f.emptyMeaning ? `<p class="empty">Elenco vuoto: ${esc(f.emptyMeaning)}.</p>` : ''}
        <div class="add">
          ${left.length ? `<select data-add="select" data-path="${p}" aria-label="Voce nota da aggiungere">${left.map((k) => `<option>${esc(k)}</option>`).join('')}</select>` : ''}
          ${free ? `<input type="text" class="mono" data-add="text" data-path="${p}" placeholder="modulo:riga" aria-label="Riga di un modulo, nella forma modulo:riga" spellcheck="false">` : ''}
          <button type="button" class="btn btn--ghost" data-act="add" data-path="${p}" ${!left.length && !free ? 'disabled' : ''}>Aggiungi</button>
        </div>`;
    }
    case 'limitOverrides': {
      const items = v as string[];
      const rows = items
        .map((it, i) => {
          const [c = '', a = '', b = ''] = it.split(':');
          const cell = (part: number, val: string, label: string, ph: string) =>
            `<input type="text" class="mono" data-kind="ov" data-path="${p}" data-idx="${i}" data-part="${part}" value="${esc(val)}" aria-label="${label}, voce ${i + 1}" placeholder="${ph}" inputmode="${part ? 'numeric' : 'text'}" spellcheck="false">`;
          return `<li class="row row--ov">${cell(0, c, 'Codice valuta', 'EUR')}${cell(1, a, 'Massimo per operazione', 'per operazione')}${cell(2, b, 'Massimo al giorno', 'al giorno')}
            <span class="row__btns"><button type="button" class="ibtn" data-act="del" data-path="${p}" data-idx="${i}" aria-label="Rimuovi limite ${i + 1}">×</button></span></li>`;
        })
        .join('');
      return `<ol class="rows" id="${id}">${rows}</ol>${items.length ? '' : '<p class="empty">Nessun limite per singola valuta: valgono i massimi qui sopra.</p>'}
        <div class="add"><button type="button" class="btn btn--ghost" data-act="add" data-path="${p}">Aggiungi limite</button></div>`;
    }
    default:
      return '';
  }
}

function fieldHtml(s: Session, f: Field): string {
  const g = getField(s, f.path);
  const id = idOf(f.path);
  if (g.unreadable) {
    return `<div class="fld" data-field="${esc(f.path)}" data-locked>
      <div class="fld__head"><span class="fld__label">${esc(f.label)}</span><code class="fld__path">${esc(f.path)}</code></div>
      <p class="fld__desc">${esc(f.description)}</p>
      <p class="fld__err is-warn">Nel file questo valore è scritto in una forma che il configuratore non legge (per esempio su più righe). Resta com’è: se vuoi cambiarlo, modificalo a mano nel file.</p>
    </div>`;
  }
  return `<div class="fld" data-field="${esc(f.path)}">
    <div class="fld__head">
      <label class="fld__label" for="${id}">${esc(f.label)}</label>
      <code class="fld__path">${esc(f.path)}</code>
      <span class="badge" data-badge hidden></span>
    </div>
    <p class="fld__desc" id="d-${id}">${esc(f.description)}${esc(rangeNote(f))} Predefinito: ${esc(show(f.default))}.${f.unused ? ' Non usata nella versione corrente.' : ''}</p>
    ${controlHtml(f, g.value)}
    <p class="fld__err" id="e-${id}" data-err></p>
    <div class="fld__tools">
      <button type="button" class="link" data-act="default" data-path="${esc(f.path)}">Ripristina predefinito</button>
      <button type="button" class="link" data-act="undo" data-path="${esc(f.path)}">Annulla modifica</button>
    </div>
  </div>`;
}

function cashHtml(s: Session): string {
  const rows = s.cash!.currencies
    .map((c: { code: string; decimals: number; denominations: number[] }, i: number) => `<li class="fld" data-cash-row="${i}">
      <div class="row row--cash">
        <input type="text" class="mono" data-kind="cash-code" data-idx="${i}" value="${esc(c.code)}" placeholder="Codice (es. EUR)" aria-label="Codice valuta ${i + 1}" spellcheck="false">
        <input type="text" class="mono" data-kind="cash-dec" data-idx="${i}" value="${esc(drafts.get(`cashdec:${i}`) ?? c.decimals)}" placeholder="Decimali" aria-label="Decimali della valuta ${i + 1}" inputmode="numeric">
        <input type="text" class="mono row__den" data-kind="cash-den" data-idx="${i}" value="${esc(drafts.get(`cash:${i}`) ?? c.denominations.join(', '))}" placeholder="Tagli (es. 100, 500, 1000)" aria-label="Tagli della valuta ${i + 1}" inputmode="numeric">
        <span class="row__btns"><button type="button" class="ibtn" data-act="cash-del" data-idx="${i}" aria-label="Rimuovi la valuta ${esc(c.code || i + 1)}">×</button></span>
      </div>
      <p class="fld__err" data-cash-err="${i}"></p>
    </li>`)
    .join('');
  return `<h3>Tagli del contante</h3>
    <p class="fld__desc">Una riga per valuta: codice, decimali (da 0 a 8) e tagli in unità minime, separati da virgole. Con 2 decimali, 100 vale 1,00. Ogni taglio deve essere multiplo del precedente più piccolo.</p>
    <ul class="rows">${rows || '<li class="empty">Nessuna valuta.</li>'}</ul>
    <div class="add"><button type="button" class="btn btn--ghost" data-act="cash-add">Aggiungi valuta</button></div>`;
}

function renderFields() {
  if (!session) return;
  const s = session;
  const box = el('fields');
  if (s.id === 'cash') box.innerHTML = cashHtml(s);
  else if (currentSection === 'unknown') {
    box.innerHTML = `<h3>Chiavi non riconosciute</h3>
      <p class="fld__desc">Il configuratore non conosce queste chiavi, forse perché vengono da un’altra versione di QRM. Restano nel file così come sono.</p>
      <div class="unk">${s.unknownKeys.map((k: string) => `<code>${esc(k)}</code>`).join('')}</div>`;
  } else {
    const sec = schemaFor(s.id).sections.find((x: { id: string }) => x.id === currentSection)!;
    box.innerHTML = `<h3>${esc(sec.label)}</h3>${sec.fields.map((f: Field) => fieldHtml(s, f)).join('')}`;
  }
  refreshAllFields();
}

// ---------- aggiornamento senza ridisegnare ----------
function issueFor(path: string): Issue | undefined {
  return allIssues().find((i) => i.path === path);
}

function refreshField(path: string) {
  if (!session) return;
  const box = document.querySelector<HTMLElement>(`.fld[data-field="${CSS.escape(path)}"]`);
  const f = findField(session.id, path);
  if (!box || !f || box.dataset.locked !== undefined) return;
  const g = getField(session, path);
  const issue = issueFor(path);
  box.classList.toggle('is-mod', g.modified);
  box.classList.toggle('is-err', issue?.severity === 'error');
  const badge = box.querySelector<HTMLElement>('[data-badge]')!;
  badge.hidden = !(g.modified || !g.present);
  badge.className = g.modified ? 'badge badge--mod' : 'badge';
  badge.textContent = g.modified ? 'modificato' : 'non nel file';
  const err = box.querySelector<HTMLElement>('[data-err]')!;
  err.textContent = issue ? issue.message : '';
  err.classList.toggle('is-warn', issue?.severity === 'warning');
  box.querySelectorAll('input[type="text"]').forEach((i) => i.setAttribute('aria-invalid', String(issue?.severity === 'error')));
  const sw = box.querySelector<HTMLElement>('.switch__txt');
  if (sw) sw.textContent = g.value ? 'Attivo' : 'Disattivato';
  box.querySelector<HTMLButtonElement>('[data-act="default"]')!.disabled = eq(g.value as Value, f.default as Value);
  box.querySelector<HTMLButtonElement>('[data-act="undo"]')!.hidden = !g.modified;
}

function refreshCashRows() {
  if (!session || session.id !== 'cash') return;
  const all = allIssues();
  session.cash!.currencies.forEach((c: { code: string }, i: number) => {
    const out = all.filter((x) => x.path === c.code || x.path.startsWith(`${c.code}.`) || x.path === `cash:${i}`);
    const p = document.querySelector<HTMLElement>(`[data-cash-err="${i}"]`);
    if (p) p.textContent = out.map((x) => x.message).join(' ');
    document.querySelectorAll(`[data-cash-row="${i}"] input`).forEach((inp) => inp.setAttribute('aria-invalid', String(out.length > 0)));
  });
}

function refreshAllFields() {
  if (!session) return;
  if (session.id === 'cash') refreshCashRows();
  else document.querySelectorAll<HTMLElement>('.fld[data-field]').forEach((b) => refreshField(b.dataset.field!));
}

function modifiedCount(s: Session): number {
  if (s.id === 'cash') return changedLines(s).length ? 1 : 0;
  return schemaFor(s.id).sections.flatMap((x: { fields: Field[] }) => x.fields).filter((f: Field) => getField(s, f.path).modified).length;
}

function refreshChrome() {
  if (!session) return;
  const s = session;
  const all = allIssues();
  const errors = all.filter((i) => i.severity === 'error');
  const warns = all.filter((i) => i.severity === 'warning');
  const mods = modifiedCount(s);
  el('status').innerHTML = [
    s.isNew ? '<span>Nuovo file</span>' : `<span>${mods === 0 ? 'Nessuna modifica' : s.id === 'cash' ? 'File modificato' : `${mods} ${mods === 1 ? 'modifica' : 'modifiche'}`}</span>`,
    errors.length ? `<span class="is-err">${errors.length} ${errors.length === 1 ? 'errore' : 'errori'}</span>` : '<span>Nessun errore</span>',
    warns.length ? `<span class="is-warn">${warns.length} ${warns.length === 1 ? 'avviso' : 'avvisi'}</span>` : '',
  ].join('');
  const dl = el<HTMLButtonElement>('download');
  dl.disabled = errors.length > 0;
  dl.title = errors.length ? 'Correggi gli errori per poter scaricare il file.' : '';
  const label = (path: string) => findField(s.id, path)?.label ?? path;
  const problems = el('problems');
  problems.hidden = all.length === 0;
  problems.innerHTML = all.length
    ? `${errors.length ? '<p><strong>Da correggere prima di scaricare</strong></p>' : '<p><strong>Da controllare</strong></p>'}
       <ul>${all.map((i) => `<li class="${i.severity === 'warning' ? 'warn' : ''}"><strong>${esc(s.id === 'cash' ? i.path : label(i.path))}</strong>: ${esc(i.message)}</li>`).join('')}</ul>`
    : '';
  renderNav();
  const lines = render(s).split(/\r?\n/);
  if (lines[lines.length - 1] === '') lines.pop();
  const changed = s.isNew ? new Set<number>() : new Set(changedLines(s));
  el('preview').innerHTML = lines.map((l, i) => `<span class="ln${changed.has(i + 1) ? ' is-changed' : ''}" data-n="${i + 1}">${esc(l) || ' '}</span>`).join('');
}

function edited(path?: string) {
  dirty = true;
  if (path) refreshField(path);
  refreshCashRows();
  refreshChrome();
}

// ---------- eventi ----------
function parseNumber(kind: string, text: string): bigint | number | null {
  const t = text.trim();
  if (kind === 'int') return /^[+-]?\d+$/.test(t) ? BigInt(t) : null;
  const n = Number(t.replace(',', '.'));
  return t !== '' && Number.isFinite(n) ? n : null;
}

const listOf = (path: string): string[] => [...(getField(session!, path).value as string[])];

function onInput(t: HTMLInputElement) {
  if (!session) return;
  const path = t.dataset.path ?? '';
  const idx = Number(t.dataset.idx);
  switch (t.dataset.kind) {
    case 'int':
    case 'float': {
      const n = parseNumber(t.dataset.kind, t.value);
      if (n === null) {
        bad.set(path, t.dataset.kind === 'int' ? 'Deve essere un numero intero.' : 'Deve essere un numero, per esempio 6 o 6.5.');
        drafts.set(path, t.value);
      } else {
        bad.delete(path);
        drafts.delete(path);
        setField(session, path, n);
      }
      return edited(path);
    }
    case 'str':
      setField(session, path, t.value);
      return edited(path);
    case 'item': {
      const list = listOf(path);
      list[idx] = t.value;
      setField(session, path, list);
      return edited(path);
    }
    case 'ov': {
      const row = t.closest('li')!;
      const parts = [...row.querySelectorAll<HTMLInputElement>('input[data-kind="ov"]')].map((i) => i.value.trim());
      const list = listOf(path);
      list[idx] = parts.join(':');
      setField(session, path, list);
      return edited(path);
    }
    case 'cash-code': {
      const pos = t.selectionStart ?? t.value.length;
      t.value = t.value.toUpperCase();
      t.setSelectionRange(pos, pos);
      session.cash!.currencies[idx].code = t.value;
      return edited();
    }
    case 'cash-dec': {
      const ok = /^\d+$/.test(t.value.trim());
      if (ok) drafts.delete(`cashdec:${idx}`);
      else drafts.set(`cashdec:${idx}`, t.value);
      session.cash!.currencies[idx].decimals = ok ? Number(t.value) : Number.NaN;
      return edited();
    }
    case 'cash-den': {
      const tokens = t.value.split(/[\s,;]+/).filter(Boolean);
      if (tokens.every((x) => /^\d+$/.test(x))) {
        bad.delete(`cash:${idx}`);
        drafts.delete(`cash:${idx}`);
        session.cash!.currencies[idx].denominations = tokens.map(Number);
      } else {
        bad.set(`cash:${idx}`, 'Nei tagli usa solo numeri interi separati da virgole.');
        drafts.set(`cash:${idx}`, t.value);
      }
      return edited();
    }
  }
}

function refocus(selector: string) {
  document.querySelector<HTMLElement>(selector)?.focus();
}

function onClick(b: HTMLElement) {
  if (!session) return;
  const s = session;
  const path = b.dataset.path ?? '';
  const idx = Number(b.dataset.idx);
  const sel = (act: string, i: number) => `[data-act="${act}"][data-path="${CSS.escape(path)}"][data-idx="${i}"]`;
  const redraw = (focus: string) => {
    dirty = true;
    renderFields();
    refreshChrome();
    refocus(focus);
  };
  switch (b.dataset.act) {
    case 'default': {
      const f = findField(s.id, path)!;
      bad.delete(path);
      drafts.delete(path);
      setField(s, path, f.default);
      return redraw(`#${idOf(path)}`);
    }
    case 'undo':
      bad.delete(path);
      drafts.delete(path);
      resetField(s, path);
      return redraw(`#${idOf(path)}`);
    case 'up':
    case 'down': {
      const list = listOf(path);
      const to = b.dataset.act === 'up' ? idx - 1 : idx + 1;
      [list[idx], list[to]] = [list[to], list[idx]];
      setField(s, path, list);
      return redraw(`${sel(b.dataset.act, to)}:not(:disabled), ${sel(b.dataset.act === 'up' ? 'down' : 'up', to)}`);
    }
    case 'del': {
      const list = listOf(path);
      list.splice(idx, 1);
      setField(s, path, list);
      return redraw(`[data-act="add"][data-path="${CSS.escape(path)}"]`);
    }
    case 'add': {
      const f = findField(s.id, path)!;
      const list = listOf(path);
      if (f.type === 'limitOverrides') list.push('');
      else {
        const box = b.closest('.add')!;
        const text = box.querySelector<HTMLInputElement>('[data-add="text"]');
        const select = box.querySelector<HTMLSelectElement>('[data-add="select"]');
        const value = (text?.value.trim() || select?.value || '').trim();
        if (!value) return text?.focus();
        list.push(value);
      }
      setField(s, path, list);
      return redraw(f.type === 'limitOverrides' ? `[data-kind="ov"][data-path="${CSS.escape(path)}"][data-idx="${list.length - 1}"][data-part="0"]` : `[data-add][data-path="${CSS.escape(path)}"]`);
    }
    case 'cash-add':
      s.cash!.currencies.push({ code: '', decimals: 2, denominations: [100], extra: {} });
      dirty = true;
      renderFields();
      refreshChrome();
      return refocus(`[data-kind="cash-code"][data-idx="${s.cash!.currencies.length - 1}"]`);
    case 'cash-del':
      s.cash!.currencies.splice(idx, 1);
      for (const k of [...bad.keys()].filter((x) => x.startsWith('cash:'))) bad.delete(k);
      for (const k of [...drafts.keys()].filter((x) => x.startsWith('cash'))) drafts.delete(k);
      dirty = true;
      renderFields();
      refreshChrome();
      return refocus('[data-act="cash-add"]');
  }
}

function download() {
  if (!session || allIssues().some((i) => i.severity === 'error')) return;
  const blob = new Blob([render(session)], { type: 'text/plain;charset=utf-8' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = downloadName(session);
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  dirty = false;
}

function init() {
  initHeader();
  const fields = el('fields');
  fields.addEventListener('input', (e) => {
    const t = e.target as HTMLInputElement;
    if (t.dataset.kind && t.dataset.kind !== 'bool') onInput(t);
  });
  fields.addEventListener('change', (e) => {
    const t = e.target as HTMLInputElement;
    if (t.dataset.kind === 'bool' && session) {
      setField(session, t.dataset.path!, t.checked);
      edited(t.dataset.path);
    }
  });
  fields.addEventListener('click', (e) => {
    const b = (e.target as HTMLElement).closest<HTMLElement>('[data-act]');
    if (b) onClick(b);
  });
  fields.addEventListener('keydown', (e) => {
    const t = e.target as HTMLInputElement;
    if (e.key === 'Enter' && t.dataset.add) {
      e.preventDefault();
      t.closest('.add')?.querySelector<HTMLElement>('[data-act="add"]')?.click();
    }
  });
  el('sections').addEventListener('click', (e) => {
    const b = (e.target as HTMLElement).closest<HTMLElement>('[data-sec]');
    if (!b) return;
    currentSection = b.dataset.sec!;
    renderNav();
    renderFields();
    refocus(`[data-sec="${currentSection}"]`);
  });
  el('download').addEventListener('click', download);
  el('open-other').addEventListener('click', () => el<HTMLInputElement>('file').click());
  const input = el<HTMLInputElement>('file');
  input.addEventListener('change', () => {
    const f = input.files?.[0];
    input.value = '';
    if (f) void loadFile(f);
  });
  document.querySelectorAll<HTMLElement>('[data-new]').forEach((b) =>
    b.addEventListener('click', () => {
      if (confirmDiscard()) startSession(newSession(b.dataset.new as 'common' | 'bank' | 'cash'));
    }),
  );
  const drop = el('drop');
  const hasFiles = (e: DragEvent) => !!e.dataTransfer?.types.includes('Files');
  document.addEventListener('dragover', (e) => {
    if (!hasFiles(e)) return;
    e.preventDefault();
    drop.classList.add('is-over');
  });
  document.addEventListener('dragleave', (e) => {
    if (!e.relatedTarget) drop.classList.remove('is-over');
  });
  document.addEventListener('drop', (e) => {
    if (!hasFiles(e)) return;
    e.preventDefault();
    drop.classList.remove('is-over');
    const f = e.dataTransfer?.files[0];
    if (f) void loadFile(f);
  });
  window.addEventListener('beforeunload', (e) => {
    if (dirty) {
      e.preventDefault();
      e.returnValue = '';
    }
  });
}

init();
