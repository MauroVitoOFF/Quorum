// Generatore delle texture dell'ATM (stile "banca d'epoca": marmo smeraldo, ottone, carta da registro).
// Si esegue dentro Blockbench (risky_eval) con un progetto aperto:
//   (function(){ return eval(require('fs').readFileSync('<questo file>', 'utf8')); })()
// Scrive i PNG in qrm-bank/.../textures/gui/atm/ e le anteprime in pixel-art/bank/.
(function () {
const fs = require('fs');
const OUT = 'C:/Users/MrMadaOffice/Desktop/QRM/qrm-bank/src/main/resources/assets/qrm_bank/textures/gui/atm';
const PREV = 'C:/Users/MrMadaOffice/Desktop/QRM/pixel-art/bank';
fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(PREV, { recursive: true });

const C = {
  K: '#0a1812',                       // contorno scuro verdastro
  e0: '#0e3a2a', e1: '#23704f', e2: '#34936b', e3: '#5cc496', ev: '#9cecc4', ed: '#0b3023', // marmo
  f0: '#06231a', f1: '#0d3a2b', f2: '#17573f',                                              // panno (baize)
  d: '#7c5410', b: '#b88420', y: '#dcab38', Y: '#f6d572', h: '#fff2b4',                      // ottone
  p0: '#b79c5e', p1: '#e3d2a0', p2: '#f4e8c4', p3: '#fff7de',                                // carta
  i: '#2a1c10', i2: '#5a4528',                                                               // inchiostro
  r0: '#5c1713', r1: '#8c2a24', r2: '#b4443a', r3: '#e0705a',                                // rosso bue
  g0: '#2c7a3c', g1: '#46a85a', g2: '#6fcf7e', g3: '#b6f0be',                                // verde banconota
  x: '#fff6e0', s: '#9aa8a0', S: '#4a5a52'
};
const col = (c) => C[c] || c;

function cv(w, h) {
  const c = document.createElement('canvas');
  c.width = w; c.height = h;
  const g = c.getContext('2d');
  g.imageSmoothingEnabled = false;
  return { c, g, w, h };
}
function px(t, x, y, c) { x = Math.round(x); y = Math.round(y); if (x < 0 || y < 0 || x >= t.w || y >= t.h) return; t.g.fillStyle = col(c); t.g.fillRect(x, y, 1, 1); }
function rect(t, x, y, w, h, c) { t.g.fillStyle = col(c); t.g.fillRect(x, y, w, h); }
function clear(t, x, y) { t.g.clearRect(x, y, 1, 1); }
function ell(t, cx, cy, rx, ry, c, cond) {
  for (let y = 0; y < t.h; y++) for (let x = 0; x < t.w; x++) {
    const dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
    if (dx * dx + dy * dy <= 1 && (!cond || cond(dx, dy))) px(t, x, y, c);
  }
}
function corners(t, n) {
  for (let i = 0; i < n; i++) for (let j = 0; j < n - i; j++) {
    clear(t, i, j); clear(t, t.w - 1 - i, j); clear(t, i, t.h - 1 - j); clear(t, t.w - 1 - i, t.h - 1 - j);
  }
}
function outlineify(t, oc) {
  const d = t.g.getImageData(0, 0, t.w, t.h).data;
  const a = (x, y) => (x < 0 || y < 0 || x >= t.w || y >= t.h) ? 0 : d[(y * t.w + x) * 4 + 3];
  const todo = [];
  for (let y = 0; y < t.h; y++) for (let x = 0; x < t.w; x++) {
    if (a(x, y) === 0 && (a(x - 1, y) || a(x + 1, y) || a(x, y - 1) || a(x, y + 1))) todo.push([x, y]);
  }
  todo.forEach(([x, y]) => px(t, x, y, oc || 'K'));
}
function shade(hex, f) {
  const n = parseInt(hex.slice(1), 16);
  let r = (n >> 16) & 255, g = (n >> 8) & 255, b = n & 255;
  const m = (v) => Math.max(0, Math.min(255, Math.round(f >= 0 ? v + (255 - v) * f : v * (1 + f))));
  return '#' + ((1 << 24) | (m(r) << 16) | (m(g) << 8) | m(b)).toString(16).slice(1);
}
let seed = 7;
function rnd() { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff; }

const out = {};

// ---------------------------------------------------------------- frame 48x48 (9-slice 8): marmo smeraldo, filetto d'ottone, panno
(function () {
  const t = cv(48, 48);
  rect(t, 0, 0, 48, 48, 'K');
  rect(t, 1, 1, 46, 46, 'e1');
  // marmo: venature a caso e un bisello chiaro/scuro
  for (let y = 1; y < 47; y++) for (let x = 1; x < 47; x++) {
    const inner = x >= 7 && x <= 40 && y >= 7 && y <= 40;
    if (inner) continue;
    const r = rnd();
    if (r < 0.10) px(t, x, y, 'e2'); else if (r < 0.14) px(t, x, y, 'ev'); else if (r < 0.2) px(t, x, y, 'e0');
  }
  for (let x = 1; x < 47; x++) { px(t, x, 1, 'e3'); px(t, x, 46, 'e0'); }
  for (let y = 1; y < 47; y++) { px(t, 1, y, 'e3'); px(t, 46, y, 'e0'); }
  // filetto d'ottone attorno alla finestra
  for (let x = 6; x <= 41; x++) { px(t, x, 6, 'Y'); px(t, x, 41, 'd'); }
  for (let y = 6; y <= 41; y++) { px(t, 6, y, 'Y'); px(t, 41, y, 'd'); }
  for (let x = 7; x <= 40; x++) { px(t, x, 7, 'K'); px(t, x, 40, 'K'); }
  for (let y = 7; y <= 40; y++) { px(t, 7, y, 'K'); px(t, 40, y, 'K'); }
  // panno
  rect(t, 8, 8, 32, 32, 'f1');
  rect(t, 8, 8, 32, 1, 'f0'); rect(t, 8, 8, 1, 32, 'f0');
  rect(t, 8, 39, 32, 1, 'f2'); rect(t, 39, 8, 1, 32, 'f2');
  // borchie d'ottone agli angoli
  [[2, 2], [44, 2], [2, 44], [44, 44]].forEach(([x, y]) => {
    rect(t, x, y, 2, 2, 'y'); px(t, x, y, 'h'); px(t, x + 1, y + 1, 'd');
  });
  for (let i = 0; i < 3; i++) { clear(t, i, 0); clear(t, 0, i); clear(t, 47 - i, 0); clear(t, 47, i); clear(t, i, 47); clear(t, 0, 47 - i); clear(t, 47 - i, 47); clear(t, 47, 47 - i); }
  clear(t, 1, 1); clear(t, 46, 1); clear(t, 1, 46); clear(t, 46, 46);
  out['frame'] = t;
})();

// ---------------------------------------------------------------- targa del titolo 48x16 (9-slice 6): ottone inciso
(function () {
  const t = cv(48, 16);
  rect(t, 0, 0, 48, 16, 'K');
  rect(t, 1, 1, 46, 14, 'y');
  rect(t, 1, 1, 46, 1, 'h'); rect(t, 1, 1, 1, 14, 'h');
  rect(t, 1, 2, 46, 1, 'Y'); rect(t, 2, 2, 1, 12, 'Y');
  rect(t, 1, 13, 46, 2, 'b'); rect(t, 46, 1, 1, 14, 'b');
  rect(t, 2, 14, 45, 1, 'd');
  // filetto inciso
  rect(t, 4, 4, 40, 1, 'b'); rect(t, 4, 11, 40, 1, 'Y');
  rect(t, 4, 4, 1, 8, 'b'); rect(t, 43, 4, 1, 8, 'Y');
  [[2, 7], [45, 7]].forEach(([x, y]) => { px(t, x, y, 'd'); px(t, x, y - 1, 'h'); });
  corners(t, 2);
  out['title_plate'] = t;
})();

// ---------------------------------------------------------------- linguette 28x20: tasti di registratore di cassa
function tab(main, hi, lo, rim, lamp) {
  const t = cv(28, 20);
  for (let yy = 0; yy < 18; yy++) {
    const lx = Math.round(4 * (1 - Math.abs(yy - 8.5) / 8.5));
    for (let x = 1 + lx; x <= 26; x++) px(t, x, 1 + yy, main);
    px(t, 1 + lx, 1 + yy, hi);
    if (lx < 4) px(t, 2 + lx, 1 + yy, hi);
  }
  rect(t, 1, 1, 26, 1, hi);
  rect(t, 1, 17, 26, 2, lo);
  rect(t, 25, 2, 2, 16, lo);
  rect(t, 6, 3, 19, 1, rim);
  if (lamp) { px(t, 24, 6, lamp); px(t, 24, 13, lamp); }
  outlineify(t, 'K');
  return t;
}
out['tab'] = tab('#b88420', '#f6d572', '#7c5410', '#dcab38');
out['tab_hover'] = tab('#d09a2c', '#fff2b4', '#8f6314', '#f6d572');
out['tab_selected'] = tab('#2c8f66', '#8fe8bf', '#16553c', '#b6f0d5', '#fff6e0');

// ---------------------------------------------------------------- pannello scuro 24x24 (9-slice 6): registro e contanti
(function () {
  const t = cv(24, 24);
  rect(t, 0, 0, 24, 24, 'K');
  rect(t, 1, 1, 22, 22, '#2c7d5c');
  rect(t, 2, 2, 20, 20, '#0b241a');
  rect(t, 2, 2, 20, 2, '#061510'); rect(t, 2, 2, 2, 20, '#061510');
  corners(t, 2);
  out['inset'] = t;
})();

// ---------------------------------------------------------------- pulsanti 32x20 (9-slice 4): smalto con bordo d'ottone
function button(theme, state, W, H) {
  W = W || 32; H = H || 20;
  const t = cv(W, H);
  let { main, hi, lo, border } = theme;
  if (state === 'hover') { main = shade(main, 0.14); hi = shade(hi, 0.16); lo = shade(lo, 0.1); border = shade(border, 0.25); }
  if (state === 'pressed') { const m0 = main; main = shade(main, -0.18); hi = shade(lo, -0.2); lo = shade(m0, 0.1); border = shade(border, -0.2); }
  rect(t, 0, 0, W, H, 'K');
  rect(t, 1, 1, W - 2, H - 2, border);
  rect(t, 2, 2, W - 4, H - 4, main);
  if (state === 'pressed') { rect(t, 2, 2, W - 4, 3, hi); rect(t, 2, H - 3, W - 4, 1, lo); }
  else { rect(t, 2, 2, W - 4, 2, hi); rect(t, 2, H - 4, W - 4, 2, lo); }
  px(t, 1, 1, 'Y'); px(t, W - 2, H - 2, 'd');
  corners(t, 2);
  px(t, 1, 1, 'K'); px(t, W - 2, 1, 'K'); px(t, 1, H - 2, 'K'); px(t, W - 2, H - 2, 'K');
  return t;
}
function disabledButton(W, H) {
  const t = cv(W, H);
  rect(t, 0, 0, W, H, 'K'); rect(t, 1, 1, W - 2, H - 2, '#6f7d76'); rect(t, 2, 2, W - 4, H - 4, '#46524c');
  rect(t, 2, 2, W - 4, 2, '#56635c'); rect(t, 2, H - 4, W - 4, 2, '#39433e');
  corners(t, 2); px(t, 1, 1, 'K'); px(t, W - 2, 1, 'K'); px(t, 1, H - 2, 'K'); px(t, W - 2, H - 2, 'K');
  return t;
}
const THEMES = {
  button: { main: '#1f7050', hi: '#2f9a6c', lo: '#14503a', border: '#dcab38' },
  button_confirm: { main: '#2a8a44', hi: '#46b062', lo: '#1b6030', border: '#f6d572' },
  button_danger: { main: '#a8322a', hi: '#d4564a', lo: '#6c1a14', border: '#f6d572' }
};
Object.keys(THEMES).forEach((k) => ['normal', 'hover', 'pressed'].forEach((s) => { out[k + '_' + s] = button(THEMES[k], s); }));
(function () {
  const t = cv(32, 20);
  rect(t, 0, 0, 32, 20, 'K'); rect(t, 1, 1, 30, 18, '#7c8a82'); rect(t, 2, 2, 28, 16, '#56635c');
  rect(t, 2, 2, 28, 2, '#667369'); rect(t, 2, 16, 28, 2, '#434e48');
  corners(t, 2); px(t, 1, 1, 'K'); px(t, 30, 1, 'K'); px(t, 1, 18, 'K'); px(t, 30, 18, 'K');
  out['button_disabled'] = t;
})();

// ---------------------------------------------------------------- campi di testo 32x20 (9-slice 4)
function field(focus) {
  const t = cv(32, 20);
  rect(t, 0, 0, 32, 20, 'K');
  rect(t, 1, 1, 30, 18, focus ? '#f6d572' : '#2c7d5c');
  rect(t, 2, 2, 28, 16, focus ? '#12382a' : '#0b241a');
  rect(t, 2, 2, 28, 2, focus ? '#0b261c' : '#061510'); rect(t, 2, 2, 1, 16, focus ? '#0b261c' : '#061510');
  corners(t, 2);
  return t;
}
out['field'] = field(false);
out['field_focus'] = field(true);

// ---------------------------------------------------------------- targa del saldo 32x24 (9-slice 6): vetro scuro in cornice d'ottone
(function () {
  const t = cv(32, 24);
  rect(t, 0, 0, 32, 24, 'K');
  rect(t, 1, 1, 30, 22, 'y');
  rect(t, 1, 1, 30, 1, 'h'); rect(t, 1, 1, 1, 22, 'h');
  rect(t, 1, 2, 30, 1, 'Y'); rect(t, 2, 2, 1, 20, 'Y');
  rect(t, 1, 22, 30, 1, 'd'); rect(t, 30, 1, 1, 22, 'd');
  rect(t, 3, 3, 26, 18, 'b');
  rect(t, 4, 4, 24, 16, 'K');
  rect(t, 5, 5, 22, 14, '#0c261c');
  rect(t, 5, 5, 22, 1, '#06150f');
  rect(t, 5, 18, 22, 1, '#164a37');
  corners(t, 2);
  out['balance_plate'] = t;
})();

// ---------------------------------------------------------------- targa a vetro 32x22 (9-slice 3): messaggi e saldo in basso
(function () {
  const t = cv(32, 22);
  rect(t, 0, 0, 32, 22, 'K');
  rect(t, 1, 1, 30, 20, 'b');
  rect(t, 1, 1, 30, 1, 'Y'); rect(t, 1, 1, 1, 20, 'Y');
  rect(t, 1, 20, 30, 1, 'd'); rect(t, 30, 1, 1, 20, 'd');
  rect(t, 2, 2, 28, 18, '#0b241a');
  rect(t, 2, 2, 28, 1, '#050f0a'); rect(t, 2, 19, 28, 1, '#14402f');
  corners(t, 2);
  out['lcd_plate'] = t;
})();

// ---------------------------------------------------------------- icone 16x16
function icon(name, fn) { const t = cv(16, 16); fn(t); outlineify(t, 'K'); out['icon_' + name] = t; }
function note(t, x, y, w, h) { // banconota
  rect(t, x, y, w, h, 'g1'); rect(t, x, y, w, 1, 'g2'); rect(t, x, y + h - 1, w, 1, 'g0');
  rect(t, x + 1, y + 1, w - 2, 1, 'g3');
  ell(t, x + w / 2, y + h / 2, 2.2, 2.2, 'x'); px(t, x + w / 2 - 1, y + h / 2 - 1, 'b');
  px(t, x + 1, y + 2, 'g3'); px(t, x + w - 2, y + h - 3, 'g0');
}
icon('account', (t) => { // libro mastro aperto
  rect(t, 1, 3, 7, 10, 'p2'); rect(t, 8, 3, 7, 10, 'p2');
  rect(t, 1, 3, 14, 1, 'p3'); rect(t, 1, 12, 14, 1, 'p0');
  rect(t, 7, 3, 2, 10, 'b');
  for (const yy of [5, 7, 9]) { rect(t, 2, yy, 4, 1, 'i2'); rect(t, 10, yy, 4, 1, 'i2'); }
  rect(t, 1, 13, 14, 1, 'e1'); rect(t, 1, 14, 14, 1, 'e0');
});
icon('cash', (t) => { note(t, 2, 6, 11, 7); note(t, 3, 3, 11, 7); });
icon('transfer', (t) => {
  rect(t, 2, 4, 9, 2, 'y'); rect(t, 2, 4, 9, 1, 'Y');
  rect(t, 10, 2, 1, 6, 'y'); rect(t, 11, 3, 1, 4, 'y'); rect(t, 12, 4, 1, 2, 'y'); rect(t, 10, 2, 1, 1, 'Y');
  rect(t, 5, 10, 9, 2, 'g2'); rect(t, 5, 10, 9, 1, 'g3');
  rect(t, 5, 8, 1, 6, 'g2'); rect(t, 4, 9, 1, 4, 'g2'); rect(t, 3, 10, 1, 2, 'g2'); rect(t, 5, 13, 1, 1, 'g1');
});
icon('withdraw', (t) => { // banconota che esce
  note(t, 1, 7, 10, 7);
  rect(t, 8, 2, 6, 2, 'r3'); rect(t, 12, 1, 1, 4, 'r3'); rect(t, 13, 2, 1, 2, 'r3'); px(t, 8, 2, 'x');
});
icon('deposit', (t) => { // banconota che entra
  note(t, 5, 7, 10, 7);
  rect(t, 2, 2, 6, 2, 'g2'); rect(t, 2, 1, 1, 4, 'g2'); rect(t, 1, 2, 1, 2, 'g2'); px(t, 7, 2, 'g3');
});
icon('send', (t) => { // busta
  rect(t, 2, 4, 12, 9, 'p2'); rect(t, 2, 4, 12, 1, 'p3'); rect(t, 2, 12, 12, 1, 'p0');
  for (let k = 0; k < 6; k++) { px(t, 3 + k, 5 + k * 0.7, 'p0'); px(t, 12 - k, 5 + k * 0.7, 'p0'); }
  rect(t, 7, 9, 2, 2, 'r2'); px(t, 7, 9, 'r3');
});

icon('coin', (t) => {
  ell(t, 8, 8, 6.4, 6.4, 'd'); ell(t, 8, 8, 5.4, 5.4, 'y');
  ell(t, 8, 8, 5.4, 5.4, 'Y', (dx, dy) => dx + dy < -0.7);
  ell(t, 8, 8, 3.4, 3.4, 'd'); ell(t, 8, 8, 2.6, 2.6, 'y'); px(t, 7, 6, 'h');
});
icon('person', (t) => { // testa a blocco del cassiere: bombetta smeraldo, capelli blu-verde, guance rosa
  const hat = '#1c6b4a', hatH = '#34a070', hair = '#1f4f5a', skin = '#f2b88c', skinL = '#d98f68', dark = '#2a1410';
  rect(t, 4, 1, 8, 4, hat); rect(t, 5, 1, 3, 1, hatH); rect(t, 4, 3, 8, 1, '#dcab38');
  rect(t, 2, 5, 12, 2, hat); rect(t, 2, 6, 12, 1, '#124a33');
  rect(t, 3, 7, 10, 7, skin); rect(t, 3, 13, 10, 1, skinL); rect(t, 12, 7, 1, 7, skinL);
  rect(t, 3, 7, 10, 2, hair); rect(t, 3, 9, 1, 3, hair); rect(t, 12, 9, 1, 3, hair);
  rect(t, 5, 10, 2, 2, dark); rect(t, 9, 10, 2, 2, dark); px(t, 5, 10, 'x'); px(t, 9, 10, 'x');
  px(t, 4, 12, '#ff9a7a'); px(t, 11, 12, '#ff9a7a');
  px(t, 6, 13, dark); px(t, 7, 13, 'x'); px(t, 8, 13, 'x'); px(t, 9, 13, dark);
});
icon('note', (t) => {
  rect(t, 3, 2, 10, 13, 'p2'); rect(t, 3, 2, 10, 1, 'p3'); rect(t, 3, 14, 10, 1, 'p0'); rect(t, 12, 2, 1, 13, 'p0');
  for (const yy of [5, 7, 9]) rect(t, 5, yy, 6, 1, 'i2'); rect(t, 5, 11, 3, 1, 'i2');
  px(t, 12, 2, 'p0'); px(t, 11, 2, 'p1');
});
icon('ok', (t) => {
  ell(t, 8, 8, 6.4, 6.4, 'g1'); ell(t, 8, 8, 6.4, 6.4, 'g2', (dx, dy) => dy < -0.5);
  [[4, 8], [5, 9], [6, 10], [7, 9], [8, 8], [9, 7], [10, 6], [11, 5]].forEach(([x, y]) => { px(t, x, y, 'x'); px(t, x, y + 1, 'x'); });
});
icon('error', (t) => {
  ell(t, 8, 8, 6.4, 6.4, 'r2'); ell(t, 8, 8, 6.4, 6.4, 'r3', (dx, dy) => dy < -0.5);
  for (let k = 0; k < 6; k++) { px(t, 5 + k, 5 + k, 'x'); px(t, 6 + k, 5 + k, 'x'); px(t, 10 - k, 5 + k, 'x'); px(t, 11 - k, 5 + k, 'x'); }
});
icon('in', (t) => {
  rect(t, 6, 1, 4, 6, 'g2'); rect(t, 6, 1, 1, 6, 'g3');
  for (let k = 0; k < 5; k++) rect(t, 3 + k, 7 + k, 10 - 2 * k, 1, 'g2');
  rect(t, 2, 13, 12, 2, 's'); rect(t, 2, 13, 12, 1, 'x');
});
icon('out', (t) => {
  rect(t, 6, 8, 4, 6, 'r3'); rect(t, 6, 8, 1, 6, 'x');
  for (let k = 0; k < 6; k++) rect(t, 7 - k, 2 + k, 2 + 2 * k, 1, 'r3');
  rect(t, 7, 2, 2, 1, 'x');
});
icon('admin', (t) => {
  ell(t, 8, 8, 6.6, 6.6, 'r1');
  for (let k = 0; k < 8; k++) { const a = k * Math.PI / 4; px(t, 8 + Math.cos(a) * 7, 8 + Math.sin(a) * 7, 'r1'); }
  ell(t, 8, 8, 5, 5, 'r2'); ell(t, 8, 8, 3.6, 3.6, 'r1');
  rect(t, 7, 5, 2, 6, 'Y'); rect(t, 5, 7, 6, 2, 'Y');
});
icon('cycle', (t) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const dx = x + 0.5 - 8, dy = y + 0.5 - 8, d = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
    const gap = Math.abs(a) < 0.45 || Math.abs(Math.abs(a) - Math.PI) < 0.45;
    if (d >= 3.8 && d <= 5.8 && !gap) px(t, x, y, dy < 0 ? 'Y' : 'y');
  }
  rect(t, 11, 2, 4, 1, 'Y'); rect(t, 13, 2, 2, 4, 'Y'); rect(t, 1, 10, 2, 4, 'y'); rect(t, 1, 13, 4, 1, 'y');
});
icon('power', (t) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const dx = x + 0.5 - 8, dy = y + 0.5 - 9, d = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
    const gap = Math.abs(a + Math.PI / 2) < 0.62;
    if (d >= 4.2 && d <= 6.2 && !gap) px(t, x, y, 'x');
  }
  rect(t, 7, 1, 2, 8, 'x');
});

// ---------------------------------------------------------------- tasti grafici: numeri e SEND disegnati a pixel
const GLY = {
  '0': ['01110', '10001', '10011', '10101', '11001', '10001', '01110'],
  '2': ['01110', '10001', '00001', '00010', '00100', '01000', '11111'],
  '5': ['11111', '10000', '11110', '00001', '00001', '10001', '01110'],
  '7': ['11111', '00001', '00010', '00100', '01000', '01000', '01000'],
  '%': ['11001', '11010', '00010', '00100', '01000', '01011', '10011'],
  'M': ['10001', '11011', '10101', '10101', '10001', '10001', '10001'],
  'A': ['01110', '10001', '10001', '11111', '10001', '10001', '10001'],
  'X': ['10001', '10001', '01010', '00100', '01010', '10001', '10001'],
  'S': ['01111', '10000', '10000', '01110', '00001', '00001', '11110'],
  'E': ['11111', '10000', '10000', '11110', '10000', '10000', '11111'],
  'N': ['10001', '11001', '10101', '10101', '10011', '10001', '10001'],
  'D': ['11110', '10001', '10001', '10001', '10001', '10001', '11110']
};
function glyphWidth(str, bold, gap) { return str.length * (5 + (bold ? 1 : 0)) + (str.length - 1) * gap; }
function glyphs(t, str, x, y, color, bold, gap) {
  let cx0 = x;
  for (const ch of str) {
    GLY[ch].forEach((rowBits, ry) => { for (let rx = 0; rx < 5; rx++) if (rowBits[rx] === '1') { px(t, cx0 + rx, y + ry, color); if (bold) px(t, cx0 + rx + 1, y + ry, color); } });
    cx0 += 5 + (bold ? 1 : 0) + gap;
  }
}
function labeled(base, str, dy, ink, shadow, bold, gap) {
  const w = glyphWidth(str, bold, gap);
  const x = Math.round((base.w - w) / 2), y = Math.round((base.h - 7) / 2) + dy;
  if (shadow) glyphs(base, str, x + 1, y + 1, shadow, bold, gap);
  glyphs(base, str, x, y, ink, bold, gap);
  return base;
}
// numeri: 25% 50% 75% e MAX, 56x14, ottone con inchiostro in rilievo
const PRESET_W = 56, PRESET_H = 14;
[['25', '25%'], ['50', '50%'], ['75', '75%'], ['max', 'MAX']].forEach(([id, label]) => {
  ['normal', 'hover', 'pressed'].forEach((st) => {
    out['preset_' + id + '_' + st] = labeled(button(THEMES.button, st, PRESET_W, PRESET_H), label, st === 'pressed' ? 1 : 0, '#fff6e0', null, true, 1);
  });
  out['preset_' + id + '_disabled'] = labeled(disabledButton(PRESET_W, PRESET_H), label, 0, '#cfd9d3', null, true, 1);
});
// SEND: verde banconota con busta, borchie e tre chevron, 120x20
const SEND_W = 120, SEND_H = 20;
function sendButton(st, disabled) {
  const t = disabled ? disabledButton(SEND_W, SEND_H) : button(THEMES.button_confirm, st, SEND_W, SEND_H);
  const dy = st === 'pressed' ? 1 : 0;
  if (!disabled) {
    [[5, 5], [SEND_W - 7, 5], [5, SEND_H - 7], [SEND_W - 7, SEND_H - 7]].forEach(([x, y]) => { px(t, x, y, 'Y'); px(t, x + 1, y + 1, 'd'); });
    for (let k = 0; k < 3; k++) { // chevron ">" verso destra
      const x = SEND_W - 26 + k * 6;
      [[0, 0], [1, 1], [2, 2], [1, 3], [0, 4]].forEach(([ox, oy]) => px(t, x + ox, 8 + oy - 1 + dy, k === 2 ? 'g3' : 'g2'));
    }
    [[44, 5], [78, 14]].forEach(([x, y]) => { px(t, x, y, 'h'); px(t, x - 1, y, 'g3'); px(t, x + 1, y, 'g3'); px(t, x, y - 1, 'g3'); px(t, x, y + 1, 'g3'); });
  }
  t.g.drawImage(out['icon_send'].c, 8, 2 + dy);
  labeled(t, 'SEND', dy, disabled ? '#cfd9d3' : '#fff6e0', null, true, 2);
  return t;
}
['normal', 'hover', 'pressed'].forEach((st) => { out['send_' + st] = sendButton(st, false); });
out['send_disabled'] = sendButton('normal', true);

// ---------------------------------------------------------------- mascotte: il cassiere (stile "personaggi a blocchi" con contorno scuro)
const FW = 32, FH = 40, FRAMES = 8;
const KID = { O: '#2a1410', sk: '#f2b88c', skH: '#ffd9b4', skL: '#d98f68', bl: '#ff9a7a',
  hr: '#1f4f5a', hrH: '#3d8c92', hrL: '#143640',
  hat: '#1c6b4a', hatH: '#34a070', hatL: '#124a33',
  sh: '#f4f0e6', shL: '#cfc8b8', vest: '#1c6b4a', vestH: '#2f9a6c', vestL: '#124a33', bow: '#c8443a', bowL: '#8e2a22',
  pant: '#2f5fb0', pantH: '#4a82d8', pantL: '#1f4080', shoe: '#6b3a1e', shoeH: '#a05c32', w: '#ffffff' };
function kid(p) {
  const t = cv(FW, FH);
  const by = p.body || 0;
  // gambe e scarpe
  rect(t, 11, 31, 4, 6, KID.pant); rect(t, 17, 31, 4, 6, KID.pant);
  rect(t, 11, 31, 1, 6, KID.pantH); rect(t, 17, 31, 1, 6, KID.pantH); rect(t, 14, 31, 1, 6, KID.pantL); rect(t, 20, 31, 1, 6, KID.pantL);
  rect(t, 10, 37, 6, 3, KID.shoe); rect(t, 16, 37, 6, 3, KID.shoe); rect(t, 10, 37, 6, 1, KID.shoeH); rect(t, 16, 37, 6, 1, KID.shoeH);
  // busto: panciotto smeraldo, camicia, papillon
  rect(t, 10, 23 + by, 12, 8, KID.vest); rect(t, 14, 23 + by, 4, 8, KID.sh); rect(t, 17, 23 + by, 1, 8, KID.shL);
  rect(t, 10, 23 + by, 1, 8, KID.vestH); rect(t, 18, 23 + by, 1, 8, KID.vestH); rect(t, 10, 30 + by, 12, 1, KID.vestL); rect(t, 21, 23 + by, 1, 8, KID.vestL);
  rect(t, 14, 23 + by, 4, 2, KID.bow); px(t, 15, 23 + by, '#e8705a'); px(t, 16, 24 + by, KID.bowL);
  px(t, 15, 27 + by, '#dcab38'); px(t, 15, 29 + by, '#dcab38');
  // braccio sinistro (di chi guarda) lungo il fianco
  rect(t, 7, 24 + by, 3, 5, KID.sh); rect(t, 7, 24 + by, 1, 5, KID.shL); rect(t, 7, 29 + by, 3, 3, KID.sk); rect(t, 7, 31 + by, 3, 1, KID.skL);
  // testa
  rect(t, 7, 8 + by, 18, 15, KID.sk); rect(t, 7, 22 + by, 18, 1, KID.skL); rect(t, 24, 8 + by, 1, 15, KID.skL);
  rect(t, 8, 12 + by, 4, 5, KID.skH); // luce sulla guancia
  rect(t, 7, 8 + by, 18, 4, KID.hr); rect(t, 7, 12 + by, 2, 5, KID.hr); rect(t, 23, 12 + by, 2, 5, KID.hr);
  rect(t, 9, 12 + by, 5, 2, KID.hr); rect(t, 17, 12 + by, 4, 1, KID.hr);
  rect(t, 10, 9 + by, 6, 1, KID.hrH); rect(t, 18, 9 + by, 4, 1, KID.hrH);
  // sopracciglia, occhi, naso, guance, bocca
  rect(t, 10, 13 + by, 3, 1, KID.hrL); rect(t, 19, 13 + by, 3, 1, KID.hrL);
  const ey = p.look === 'up' ? 14 : 15;
  if (p.eyes === 'happy') {
    [[10, 16], [11, 15], [12, 15], [13, 16], [18, 16], [19, 15], [20, 15], [21, 16]].forEach(([x, y]) => px(t, x, y + by, KID.O));
  } else if (p.eyes === 'blink') {
    rect(t, 10, 16 + by, 3, 1, KID.O); rect(t, 19, 16 + by, 3, 1, KID.O);
  } else {
    rect(t, 11, ey + by, 2, 3, KID.O); rect(t, 19, ey + by, 2, 3, KID.O); px(t, 11, ey + by, KID.w); px(t, 19, ey + by, KID.w);
  }
  px(t, 15, 18 + by, KID.skL); px(t, 16, 18 + by, KID.skL);
  rect(t, 9, 18 + by, 2, 1, KID.bl); rect(t, 21, 18 + by, 2, 1, KID.bl);
  if (p.mouth === 'grin') {
    rect(t, 13, 19 + by, 6, 2, KID.O); rect(t, 14, 19 + by, 4, 1, KID.w); px(t, 12, 19 + by, KID.O); px(t, 19, 19 + by, KID.O);
  } else {
    [[13, 19], [14, 20], [15, 20], [16, 20], [17, 20], [18, 19]].forEach(([x, y]) => px(t, x, y + by, KID.O));
  }
  // bombetta smeraldo con fascia d'ottone
  rect(t, 11, 2 + by, 10, 1, KID.hat); rect(t, 10, 3 + by, 12, 5, KID.hat);
  rect(t, 11, 3 + by, 4, 1, KID.hatH); rect(t, 10, 3 + by, 1, 4, KID.hatH); rect(t, 21, 3 + by, 1, 5, KID.hatL);
  rect(t, 10, 6 + by, 12, 2, '#dcab38'); rect(t, 10, 6 + by, 12, 1, '#f6d572'); px(t, 15, 7 + by, '#7c5410'); px(t, 16, 7 + by, '#7c5410');
  rect(t, 6, 8 + by, 20, 1, KID.hat); rect(t, 6, 9 + by, 20, 1, KID.hatL);
  // braccio destro (di chi guarda)
  const a = p.arm || 'down';
  if (a === 'down') {
    rect(t, 22, 24 + by, 3, 5, KID.sh); rect(t, 24, 24 + by, 1, 5, KID.shL); rect(t, 22, 29 + by, 3, 3, KID.sk); rect(t, 22, 31 + by, 3, 1, KID.skL);
  } else {
    rect(t, 22, 24 + by, 3, 3, KID.sh); rect(t, 24, 24 + by, 1, 3, KID.shL);
    const up = a === 'mid' ? 4 : 7;
    rect(t, 25, 26 + by - up, 3, up + 1, KID.sh); rect(t, 27, 26 + by - up, 1, up + 1, KID.shL);
    const hy = 23 + by - up;
    rect(t, 25, hy, 3, 3, KID.sk); rect(t, 25, hy + 2, 3, 1, KID.skL);
    if (a === 'thumb') { rect(t, 26, hy - 2, 1, 2, KID.sk); px(t, 26, hy - 2, KID.skH); }
  }
  // moneta
  if (p.coin) {
    const w = p.coin.w, cy = p.coin.y, cxm = p.coin.x || 26.5;
    ell(t, cxm, cy, w, 2.6, '#7c5410'); ell(t, cxm, cy, Math.max(0.5, w - 0.9), 1.8, '#dcab38');
    if (w > 1.5) { px(t, cxm - 1.5, cy - 1, '#fff2b4'); rect(t, cxm - 0.5, cy, 2, 1, '#7c5410'); }
  }
  outlineify(t, KID.O);
  return t;
}
const PIG_FRAMES = [
  { arm: 'down' },
  { arm: 'mid', body: 0 },
  { arm: 'up', coin: { y: 14, w: 2.6 } },
  { arm: 'up', coin: { y: 8, w: 2.6 }, look: 'up' },
  { arm: 'up', coin: { y: 3.5, w: 1.3 }, look: 'up', eyes: 'blink' },
  { arm: 'up', coin: { y: 8, w: 1.4 }, look: 'up' },
  { arm: 'up', coin: { y: 14, w: 2.6 }, body: 1 },
  { arm: 'thumb', eyes: 'happy', mouth: 'grin' }
];
(function () {
  const sheet = cv(FW, FH * FRAMES);
  PIG_FRAMES.forEach((f, i) => sheet.g.drawImage(kid(f).c, 0, i * FH));
  out['mascot'] = sheet;
})();

// ---------------------------------------------------------------- export + progetto Blockbench
const names = Object.keys(out);
names.forEach((n) => {
  const t = out[n];
  const url = t.c.toDataURL('image/png');
  fs.writeFileSync(OUT + '/' + n + '.png', Buffer.from(url.split(',')[1], 'base64'));
  try { new Texture({ name: n + '.png' }).fromDataURL(url).add(false); } catch (e) { }
});

// ---------------------------------------------------------------- anteprime
function nine(t, src, b, x, y, w, h) {
  const sw = src.w, sh = src.h, g = t.g, s = src.c;
  const xs = [0, b, sw - b, sw], ys = [0, b, sh - b, sh];
  const dx = [x, x + b, x + w - b, x + w], dy = [y, y + b, y + h - b, y + h];
  for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) {
    g.drawImage(s, xs[i], ys[j], xs[i + 1] - xs[i], ys[j + 1] - ys[j], dx[i], dy[j], dx[i + 1] - dx[i], dy[j + 1] - dy[j]);
  }
}
function put(t, src, x, y) { t.g.drawImage(src.c, x, y); }
function text(t, s, x, y, color, size, align) {
  t.g.fillStyle = col(color || 'x'); t.g.font = 'bold ' + (size || 7) + 'px monospace'; t.g.textAlign = align || 'left'; t.g.textBaseline = 'top';
  t.g.fillText(s, x, y);
}
function mascotAt(t, frame, x, y) { t.g.drawImage(out.mascot.c, 0, frame * FH, FW, FH, x, y, FW, FH); }
function scaled(t, k) { const s = cv(t.w * k, t.h * k); s.g.imageSmoothingEnabled = false; s.g.drawImage(t.c, 0, 0, t.w * k, t.h * k); return s; }
function save(t, name) { fs.writeFileSync(PREV + '/' + name, Buffer.from(t.c.toDataURL('image/png').split(',')[1], 'base64')); }

// Scheda Contanti
const M = cv(214, 148);
rect(M, 0, 0, 214, 148, '#241a14');
nine(M, out.frame, 8, 28, 24, 176, 112);
nine(M, out.title_plate, 6, 78, 16, 64, 16);
text(M, 'ATM', 110, 21, '#3a2608', 7, 'center');
mascotAt(M, 2, 160, 0);
const tabs = [['tab', 'account'], ['tab_selected', 'cash'], ['tab', 'transfer']];
tabs.forEach(([k, ic], i) => { put(M, out[k], i === 1 ? -2 : 2, 38 + i * 24); put(M, out['icon_' + ic], i === 1 ? 4 : 8, 40 + i * 24); });
nine(M, out.button_normal, 4, 40, 36, 152, 14); text(M, 'Valuta: EUR', 116, 40, '#fff6e0', 6, 'center');
nine(M, out.field_focus, 4, 40, 54, 152, 14); text(M, '250,00', 46, 58, '#2a1c10', 6);
nine(M, out.button_danger_normal, 4, 40, 74, 74, 14); put(M, out.icon_withdraw, 42, 73); text(M, 'Preleva', 84, 78, '#fff6e0', 6, 'center');
nine(M, out.button_confirm_hover, 4, 118, 74, 74, 14); put(M, out.icon_deposit, 120, 73); text(M, 'Versa tutto', 164, 78, '#fff6e0', 6, 'center');
text(M, 'Contanti addosso', 40, 94, '#a9cdb8', 6);
nine(M, out.inset, 6, 40, 102, 152, 24); text(M, '120,00 EUR', 46, 110, '#2a1c10', 7);
text(M, 'Fatto.', 40, 128, '#7fe39a', 6);
nine(M, out.button_danger_normal, 4, 150, 124, 42, 10);

// Scheda Conto
const M2 = cv(214, 148);
rect(M2, 0, 0, 214, 148, '#241a14');
nine(M2, out.frame, 8, 28, 24, 176, 112);
nine(M2, out.title_plate, 6, 78, 16, 64, 16);
text(M2, 'ATM', 110, 21, '#3a2608', 7, 'center');
mascotAt(M2, 7, 160, 0);
[['tab_selected', 'account'], ['tab', 'cash'], ['tab', 'transfer']].forEach(([k, ic], i) => { put(M2, out[k], i === 0 ? -2 : 2, 38 + i * 24); put(M2, out['icon_' + ic], i === 0 ? 4 : 8, 40 + i * 24); });
nine(M2, out.balance_plate, 6, 40, 34, 152, 26); text(M2, '1.250,00 EUR', 116, 43, '#ffd25a', 9, 'center');
text(M2, 'Attivita recente', 40, 64, '#a9cdb8', 6);
nine(M2, out.inset, 6, 40, 72, 152, 52);
[['12/10 Deposito', '+50,00'], ['11/10 Bonifico a Rossi', '-20,00'], ['10/10 Prelievo', '-100,00']].forEach(([a, b], i) => {
  text(M2, a, 46, 78 + i * 10, '#2a1c10', 5); text(M2, b, 186, 78 + i * 10, i === 0 ? '#1d6b34' : '#9c2a22', 5, 'right');
});

// Scheda Bonifico (campi, pulsanti, righe)
const M3 = cv(214, 148);
rect(M3, 0, 0, 214, 148, '#241a14');
nine(M3, out.frame, 8, 28, 24, 176, 112);
nine(M3, out.title_plate, 6, 78, 16, 64, 16);
text(M3, 'ATM', 110, 21, '#3a2608', 7, 'center');
mascotAt(M3, 4, 160, 0);
[['tab', 'account'], ['tab', 'cash'], ['tab_selected', 'transfer']].forEach(([k, ic], i) => { put(M3, out[k], i === 2 ? -2 : 2, 38 + i * 24); put(M3, out['icon_' + ic], i === 2 ? 4 : 8, 40 + i * 24); });
nine(M3, out.field, 4, 40, 34, 152, 14); text(M3, 'Destinatario', 46, 38, '#7a6a48', 6);
nine(M3, out.button_normal, 4, 40, 52, 152, 14); text(M3, 'Valuta: EUR', 116, 56, '#fff6e0', 6, 'center');
nine(M3, out.field, 4, 40, 70, 152, 14); text(M3, 'Importo', 46, 74, '#7a6a48', 6);
nine(M3, out.field, 4, 40, 88, 152, 14); text(M3, 'Causale (facoltativa)', 46, 92, '#7a6a48', 6);
nine(M3, out.button_confirm_normal, 4, 40, 108, 74, 14); put(M3, out.icon_send, 42, 107); text(M3, 'Invia', 90, 112, '#fff6e0', 6, 'center');
nine(M3, out.button_disabled, 4, 118, 108, 74, 14);

save(scaled(M, 5), 'mockup_cash.png');
save(scaled(M2, 5), 'mockup_account.png');
save(scaled(M3, 5), 'mockup_transfer.png');

// striscia della mascotte (tutti i frame, x6) e foglio di tutte le texture (x4)
const strip = cv(FW * FRAMES + 8 * (FRAMES + 1), FH + 16); rect(strip, 0, 0, strip.w, strip.h, '#2a2a30');
for (let i = 0; i < FRAMES; i++) mascotAt(strip, i, 8 + i * (FW + 8), 8);
save(scaled(strip, 4), 'mascot_big.png');
const sheet = cv(900, 640); rect(sheet, 0, 0, 900, 640, '#4a5a52');
let sx = 8, sy = 8, rowH = 0;
names.filter((n) => n !== 'mascot').forEach((n) => { const s = scaled(out[n], 4); if (sx + s.w > 892) { sx = 8; sy += rowH + 8; rowH = 0; } put(sheet, s, sx, sy); sx += s.w + 8; rowH = Math.max(rowH, s.h); });
save(sheet, 'sheet.png');

return names.length + ' textures: ' + names.join(',');
})()
