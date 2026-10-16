(function () {
const fs = require('fs');
const OUT = 'C:/Users/MrMadaOffice/Desktop/QRM/qrm-neoforge/src/main/resources/assets/qrm/textures/gui/boss';
const PREV = 'C:/Users/MrMadaOffice/Desktop/QRM/design/pixel-art/boss';
fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(PREV, { recursive: true });

const C = {
  K: '#2b0f08', k: '#4a2012', m: '#5f2a18', b: '#7a3b24', B: '#a05530', L: '#bf7040',
  w: '#f2c9a0', W: '#fbe3c6', p: '#e6b088', P: '#cf9468',
  r: '#d93a1c', R: '#8e1f10', q: '#ff6a3a', o: '#ff7a1a', O: '#c8661a',
  y: '#ffb830', Y: '#ffe27a', d: '#b8700e',
  g: '#4fae3c', G: '#2a6b24', l: '#9be072',
  s: '#aab3bd', S: '#5b6470', h: '#f0b080', H: '#c8825a', n: '#6b3a1e', x: '#fff6e0'
};
const col = (c) => C[c] || c;

function cv(w, h) {
  const c = document.createElement('canvas');
  c.width = w; c.height = h;
  const g = c.getContext('2d');
  g.imageSmoothingEnabled = false;
  return { c, g, w, h };
}
function px(t, x, y, c) { if (x < 0 || y < 0 || x >= t.w || y >= t.h) return; t.g.fillStyle = col(c); t.g.fillRect(x, y, 1, 1); }
function rect(t, x, y, w, h, c) { t.g.fillStyle = col(c); t.g.fillRect(x, y, w, h); }
function clear(t, x, y) { t.g.clearRect(x, y, 1, 1); }
function circle(t, cx, cy, r, c) {
  for (let y = 0; y < t.h; y++) for (let x = 0; x < t.w; x++) {
    const dx = x + 0.5 - cx, dy = y + 0.5 - cy;
    if (dx * dx + dy * dy <= r * r) px(t, x, y, c);
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
  r = m(r); g = m(g); b = m(b);
  return '#' + ((1 << 24) | (r << 16) | (g << 8) | b).toString(16).slice(1);
}

const out = {}; // name -> canvas wrapper

// ---------------------------------------------------------------- frame 48x48 (9-slice 8)
(function () {
  const t = cv(48, 48);
  rect(t, 0, 0, 48, 48, 'K');
  const TL = ['L', 'B', 'b', 'b', 'm', 'k'], BR = ['m', 'b', 'b', 'b', 'm', 'k'];
  for (let i = 0; i < 6; i++) {
    for (let x = 1 + i; x <= 46 - i; x++) { px(t, x, 1 + i, TL[i]); px(t, x, 46 - i, BR[i]); }
    for (let y = 1 + i; y <= 46 - i; y++) { px(t, 1 + i, y, TL[i]); px(t, 46 - i, y, BR[i]); }
  }
  rect(t, 7, 7, 34, 34, 'K');
  rect(t, 8, 8, 32, 32, 'w');
  rect(t, 8, 8, 32, 1, 'P'); rect(t, 8, 8, 1, 32, 'P');
  rect(t, 9, 9, 31, 1, 'p'); rect(t, 9, 9, 1, 31, 'p');
  rect(t, 8, 39, 32, 1, 'W'); rect(t, 39, 8, 1, 32, 'W');
  [[3, 3], [43, 3], [3, 43], [43, 43]].forEach(([x, y]) => {
    rect(t, x, y, 2, 2, 's'); px(t, x, y, 'x'); px(t, x + 1, y + 1, 'S');
  });
  for (let i = 0; i < 3; i++) { clear(t, i, 0); clear(t, 0, i); clear(t, 47 - i, 0); clear(t, 47, i); clear(t, i, 47); clear(t, 0, 47 - i); clear(t, 47 - i, 47); clear(t, 47, 47 - i); }
  clear(t, 1, 1); clear(t, 46, 1); clear(t, 1, 46); clear(t, 46, 46);
  out['frame'] = t;
})();

// ---------------------------------------------------------------- title plate 48x16 (9-slice 6)
(function () {
  const t = cv(48, 16);
  rect(t, 0, 0, 48, 16, 'K');
  rect(t, 1, 1, 46, 14, 'b');
  rect(t, 1, 1, 46, 1, 'L'); rect(t, 1, 1, 1, 14, 'L');
  rect(t, 1, 2, 46, 1, 'B'); rect(t, 2, 2, 1, 12, 'B');
  rect(t, 1, 14, 46, 1, 'm'); rect(t, 46, 1, 1, 14, 'm');
  rect(t, 3, 4, 42, 8, 'K');
  rect(t, 4, 5, 40, 6, 'k');
  rect(t, 4, 5, 40, 1, 'K');
  rect(t, 4, 10, 40, 1, 'm');
  [[2, 7], [45, 7]].forEach(([x, y]) => { px(t, x, y, 'Y'); });
  corners(t, 2);
  out['title_plate'] = t;
})();

// ---------------------------------------------------------------- tabs 28x20
function tab(main, hi, lo, outline, trim) {
  const t = cv(28, 20);
  for (let yy = 0; yy < 18; yy++) {
    const lx = Math.round(5 * (1 - Math.abs(yy - 8.5) / 8.5));
    for (let x = 1 + lx; x <= 26; x++) px(t, x, 1 + yy, main);
    px(t, 1 + lx, 1 + yy, hi);
    if (lx < 5) px(t, 2 + lx, 1 + yy, hi);
  }
  rect(t, 1, 1, 26, 1, hi);
  rect(t, 1, 17, 26, 2, lo);
  rect(t, 25, 2, 2, 16, lo);
  for (let x = 6; x <= 24; x++) px(t, x, 3, trim);
  outlineify(t, outline);
  return t;
}
out['tab'] = tab('#d93a1c', '#ff6a3a', '#8e1f10', 'K', '#ffb830');
out['tab_hover'] = tab('#ee4a26', '#ff8a5a', '#a82410', 'K', '#ffe27a');
out['tab_selected'] = tab('#f55a2a', '#ffa070', '#b02a12', '#fff6e0', '#ffe27a');

// ---------------------------------------------------------------- inset 24x24 (9-slice 6)
(function () {
  const t = cv(24, 24);
  rect(t, 0, 0, 24, 24, 'K');
  rect(t, 1, 1, 22, 22, 'k');
  rect(t, 1, 1, 22, 2, '#3a180e'); rect(t, 1, 1, 2, 22, '#3a180e');
  rect(t, 1, 22, 22, 1, 'm'); rect(t, 22, 1, 1, 22, 'm');
  corners(t, 2);
  out['inset'] = t;
})();

// ---------------------------------------------------------------- rows 32x16 (9-slice 4)
function row(outline, fill, hi, lo) {
  const t = cv(32, 16);
  rect(t, 0, 0, 32, 16, outline);
  rect(t, 1, 1, 30, 14, fill);
  rect(t, 1, 1, 30, 1, hi);
  rect(t, 1, 14, 30, 1, lo);
  corners(t, 2);
  return t;
}
out['row'] = row('#b57a52', '#ecc096', '#f6d8b8', '#d9a77c');
out['row_hover'] = row('#e07a20', '#f8d6b0', '#fde6cc', '#e8b88c');
out['row_selected'] = row('#c8661a', '#ffd27a', '#ffe9a8', '#f0b040');

// ---------------------------------------------------------------- buttons 32x20 (9-slice 4)
function button(theme, state) {
  const t = cv(32, 20);
  let { main, hi, lo, border } = theme;
  if (state === 'hover') { main = shade(main, 0.12); hi = shade(hi, 0.14); lo = shade(lo, 0.08); border = shade(border, 0.25); }
  if (state === 'pressed') { main = shade(main, -0.18); hi = shade(lo, -0.2); lo = shade(main, 0.1); border = shade(border, -0.15); }
  rect(t, 0, 0, 32, 20, 'K');
  rect(t, 1, 1, 30, 18, border);
  rect(t, 2, 2, 28, 16, main);
  if (state === 'pressed') { rect(t, 2, 2, 28, 3, hi); rect(t, 2, 17, 28, 1, lo); }
  else { rect(t, 2, 2, 28, 2, hi); rect(t, 2, 16, 28, 2, lo); }
  corners(t, 2);
  px(t, 1, 1, 'K'); px(t, 30, 1, 'K'); px(t, 1, 18, 'K'); px(t, 30, 18, 'K');
  return t;
}
const THEMES = {
  button: { main: '#6b3220', hi: '#8a4529', lo: '#4a2012', border: '#c8661a' },
  button_confirm: { main: '#2f6b2a', hi: '#3f8a38', lo: '#1f4a1c', border: '#6fc04a' },
  button_danger: { main: '#8e2a1c', hi: '#b03a28', lo: '#5e170d', border: '#ff6a4a' }
};
Object.keys(THEMES).forEach((k) => ['normal', 'hover', 'pressed'].forEach((s) => { out[k + '_' + s] = button(THEMES[k], s); }));
(function () {
  const t = cv(32, 20);
  rect(t, 0, 0, 32, 20, 'K'); rect(t, 1, 1, 30, 18, '#7a6a62'); rect(t, 2, 2, 28, 16, '#5a4a42');
  rect(t, 2, 2, 28, 2, '#6b5b52'); rect(t, 2, 16, 28, 2, '#463830');
  corners(t, 2); px(t, 1, 1, 'K'); px(t, 30, 1, 'K'); px(t, 1, 18, 'K'); px(t, 30, 18, 'K');
  out['button_disabled'] = t;
})();

// ---------------------------------------------------------------- balance plate 32x24 (9-slice 6)
(function () {
  const t = cv(32, 24);
  rect(t, 0, 0, 32, 24, 'K');
  rect(t, 1, 1, 30, 22, 'b');
  rect(t, 1, 1, 30, 1, 'L'); rect(t, 1, 1, 1, 22, 'L');
  rect(t, 1, 2, 30, 1, 'B'); rect(t, 2, 2, 1, 20, 'B');
  rect(t, 1, 22, 30, 1, 'm'); rect(t, 30, 1, 1, 22, 'm');
  rect(t, 3, 3, 26, 18, 'd');
  rect(t, 4, 4, 24, 16, 'K');
  rect(t, 5, 5, 22, 14, 'k');
  rect(t, 5, 5, 22, 1, '#3a180e');
  rect(t, 5, 18, 22, 1, 'm');
  corners(t, 2);
  out['balance_plate'] = t;
})();

// ---------------------------------------------------------------- icons 16x16
function icon(name, fn) { const t = cv(16, 16); fn(t); outlineify(t, 'K'); out['icon_' + name] = t; }
function coin(t, cx, cy, r) {
  circle(t, cx, cy, r, 'd');
  circle(t, cx, cy, r - 1, 'y');
  for (let y = 0; y < t.h; y++) for (let x = 0; x < t.w; x++) {
    const dx = x + 0.5 - cx, dy = y + 0.5 - cy;
    if (dx * dx + dy * dy <= (r - 1) * (r - 1) && dx + dy < -r * 0.45) px(t, x, y, 'Y');
  }
  circle(t, cx, cy, r - 2.6, 'd');
  circle(t, cx, cy, r - 3.4, 'y');
}
function badge(t, cx, cy, fill, dark, plus) {
  circle(t, cx, cy, 3.6, dark); circle(t, cx, cy, 2.6, fill);
  rect(t, Math.floor(cx) - 2, Math.floor(cy), 5, 1, 'x');
  if (plus) rect(t, Math.floor(cx), Math.floor(cy) - 2, 1, 5, 'x');
}
icon('account', (t) => coin(t, 8, 8, 6.4));
icon('deposit', (t) => { coin(t, 6.5, 6.5, 5); badge(t, 11.5, 11.5, 'g', 'G', true); });
icon('withdraw', (t) => { coin(t, 6.5, 6.5, 5); badge(t, 11.5, 11.5, 'r', 'R', false); });
icon('overview', (t) => {
  rect(t, 3, 3, 10, 11, 'w'); rect(t, 3, 3, 10, 1, 'W'); rect(t, 12, 3, 1, 11, 'p'); rect(t, 3, 13, 10, 1, 'P');
  rect(t, 5, 1, 6, 3, 'S'); rect(t, 6, 1, 4, 1, 's'); px(t, 8, 2, 'k');
  for (const y of [6, 8, 10]) rect(t, 5, y, y === 10 ? 4 : 6, 1, 'b');
});
icon('members', (t) => {
  function person(g, cx, top, shirt, shirtDark, hair) {
    rect(g, cx - 2, top, 4, 4, 'h'); rect(g, cx - 2, top, 4, 1, hair); px(g, cx - 2, top + 1, hair); rect(g, cx + 1, top + 2, 1, 2, 'H');
    rect(g, cx - 3, top + 5, 6, 5, shirt); rect(g, cx - 3, top + 9, 6, 1, shirtDark);
  }
  person(t, 10, 2, '#3a6fd0', '#24489a', '#2b0f08');
  const f = cv(16, 16);
  person(f, 6, 4, '#d93a1c', '#8e1f10', 'n');
  outlineify(f, 'K');
  t.g.drawImage(f.c, 0, 0);
});
icon('promote', (t) => {
  for (let r = 0; r <= 4; r++) { rect(t, 7 - r, 2 + r, 2 + 2 * r, 1, 'g'); px(t, 7 - r, 2 + r, 'l'); px(t, 8 + r, 2 + r, 'G'); }
  rect(t, 6, 7, 4, 7, 'g'); rect(t, 6, 7, 1, 7, 'l'); rect(t, 9, 7, 1, 7, 'G');
});
icon('fire', (t) => {
  rect(t, 2, 2, 6, 12, 'b'); rect(t, 3, 3, 4, 10, 'B'); rect(t, 3, 3, 4, 1, 'L'); px(t, 6, 8, 'y');
  rect(t, 8, 7, 4, 2, 'r'); rect(t, 8, 7, 4, 1, 'q');
  for (let i = 0; i < 3; i++) rect(t, 11 + i, 5 + i, 1, 6 - 2 * i, i === 0 ? 'q' : 'r');
});
icon('add', (t) => {
  rect(t, 6, 2, 4, 12, 'g'); rect(t, 2, 6, 12, 4, 'g');
  rect(t, 6, 2, 1, 4, 'l'); rect(t, 2, 6, 4, 1, 'l'); rect(t, 9, 10, 1, 4, 'G'); rect(t, 10, 9, 4, 1, 'G');
});
icon('desk', (t) => {
  rect(t, 1, 6, 14, 3, 'B'); rect(t, 1, 6, 14, 1, 'L'); rect(t, 1, 8, 14, 1, 'b');
  rect(t, 2, 9, 4, 5, 'b'); rect(t, 10, 9, 4, 5, 'b'); rect(t, 2, 9, 4, 1, 'm'); rect(t, 10, 9, 4, 1, 'm');
  px(t, 4, 11, 'y'); px(t, 12, 11, 'y');
  rect(t, 3, 3, 5, 3, 'w'); px(t, 4, 4, 'P'); px(t, 6, 4, 'P');
  rect(t, 10, 2, 3, 4, 'y'); rect(t, 10, 2, 3, 1, 'Y');
});

// ---------------------------------------------------------------- export + project textures
const names = Object.keys(out);
names.forEach((n) => {
  const t = out[n];
  const url = t.c.toDataURL('image/png');
  fs.writeFileSync(OUT + '/' + n + '.png', Buffer.from(url.split(',')[1], 'base64'));
  try { new Texture({ name: n + '.png' }).fromDataURL(url).add(false); } catch (e) { }
});

// ---------------------------------------------------------------- mockup (preview only)
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
const M = cv(200, 134);
rect(M, 0, 0, 200, 134, '#2a1d17');
nine(M, out.frame, 8, 26, 12, 156, 114);
nine(M, out.title_plate, 6, 64, 4, 80, 16);
text(M, 'BOSS MENU', 104, 9, '#f2c9a0', 7, 'center');
const tabs = [['tab', 'overview'], ['tab', 'members'], ['tab_selected', 'account']];
tabs.forEach(([k, ic], i) => { put(M, out[k], i === 2 ? -2 : 2, 34 + i * 24); put(M, out['icon_' + ic], i === 2 ? 4 : 8, 36 + i * 24); });
text(M, 'Conto aziendale', 42, 24, '#5f2a18', 7);
nine(M, out.balance_plate, 6, 40, 34, 120, 24);
put(M, out.icon_account, 44, 38);
text(M, '12.500 $', 154, 41, '#ffe27a', 9, 'right');
text(M, 'Deposita', 40, 63, '#5f2a18', 6);
['500', '25%', '50%', 'TUTTO'].forEach((l, i) => { nine(M, out['button_confirm_' + (i === 1 ? 'hover' : 'normal')], 4, 40 + i * 30, 71, 28, 14); text(M, l, 54 + i * 30, 75, '#e8ffd8', 6, 'center'); });
text(M, 'Preleva', 40, 89, '#5f2a18', 6);
['500', '25%', '50%', 'TUTTO'].forEach((l, i) => { nine(M, out['button_danger_' + (i === 3 ? 'pressed' : 'normal')], 4, 40 + i * 30, 97, 28, 14); text(M, l, 54 + i * 30, 101, '#ffe0d8', 6, 'center'); });
nine(M, out.button_disabled, 4, 40, 115, 40, 8);
// members list row sample
nine(M, out.row_selected, 4, 100, 115, 56, 8);

const M2 = cv(200, 134);
rect(M2, 0, 0, 200, 134, '#2a1d17');
nine(M2, out.frame, 8, 26, 12, 156, 114);
nine(M2, out.title_plate, 6, 64, 4, 80, 16);
text(M2, 'BOSS MENU', 104, 9, '#f2c9a0', 7, 'center');
[['tab', 'overview'], ['tab_selected', 'members'], ['tab', 'account']].forEach(([k, ic], i) => { put(M2, out[k], i === 1 ? -2 : 2, 34 + i * 24); put(M2, out['icon_' + ic], i === 1 ? 4 : 8, 36 + i * 24); });
text(M2, 'Membri', 42, 24, '#5f2a18', 7);
nine(M2, out.inset, 6, 40, 32, 130, 74);
['Mario Rossi', 'Anna Verdi', 'Luca Bianchi'].forEach((n, i) => {
  nine(M2, out[i === 1 ? 'row_selected' : (i === 2 ? 'row_hover' : 'row')], 4, 44, 36 + i * 22, 122, 20);
  text(M2, n, 62, 40 + i * 22, '#4a2012', 6);
  text(M2, ['Boss', 'Manager', 'Dipendente'][i], 62, 47 + i * 22, '#8e5a30', 5);
  put(M2, out.icon_members, 46, 38 + i * 22);
});
nine(M2, out.button_confirm_normal, 4, 40, 110, 40, 12); put(M2, out.icon_add, 42, 108);
nine(M2, out.button_normal, 4, 84, 110, 40, 12); put(M2, out.icon_promote, 86, 108);
nine(M2, out.button_danger_normal, 4, 128, 110, 40, 12); put(M2, out.icon_fire, 130, 108);

function scaled(t, k) { const s = cv(t.w * k, t.h * k); s.g.imageSmoothingEnabled = false; s.g.drawImage(t.c, 0, 0, t.w * k, t.h * k); return s; }
fs.writeFileSync(PREV + '/mockup_account.png', Buffer.from(scaled(M, 5).c.toDataURL('image/png').split(',')[1], 'base64'));
fs.writeFileSync(PREV + '/mockup_members.png', Buffer.from(scaled(M2, 5).c.toDataURL('image/png').split(',')[1], 'base64'));

// contact sheet of every texture, scaled 4x
const sheet = cv(900, 640); rect(sheet, 0, 0, 900, 640, '#6a5a50');
let sx = 8, sy = 8, rowH = 0;
names.forEach((n) => { const s = scaled(out[n], 4); if (sx + s.w > 892) { sx = 8; sy += rowH + 8; rowH = 0; } put(sheet, s, sx, sy); sx += s.w + 8; rowH = Math.max(rowH, s.h); });
fs.writeFileSync(PREV + '/sheet.png', Buffer.from(sheet.c.toDataURL('image/png').split(',')[1], 'base64'));

return names.length + ' textures: ' + names.join(',');
})()
