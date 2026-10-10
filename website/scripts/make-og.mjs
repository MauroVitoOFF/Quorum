// Genera public/og.png (1200×630) con gli stessi blocchi isometrici della hero.
// Uso: npm run og   (richiede sharp, già incluso da Astro)
import sharp from 'sharp';
import { fileURLToPath } from 'node:url';

const W = 56, H = 32, D = 52, OX = 920, OY = 215;
const P = (a, b, c) => [OX + (a - b) * W, OY + (a + b) * H - c * D];
const pts = (l) => l.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(' ');
const cube = (x, y, z, t) => {
  const top = [P(x, y, z + 1), P(x + 1, y, z + 1), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)];
  const left = [P(x, y + 1, z + 1), P(x + 1, y + 1, z + 1), P(x + 1, y + 1, z), P(x, y + 1, z)];
  const right = [P(x + 1, y, z + 1), P(x + 1, y + 1, z + 1), P(x + 1, y + 1, z), P(x + 1, y, z)];
  return `<polygon points="${pts(left)}" fill="${t[1]}"/><polygon points="${pts(right)}" fill="${t[2]}"/><polygon points="${pts(top)}" fill="${t[0]}"/>`;
};
const T = {
  core: ['#7be3f5', '#2aa5c0', '#1b7d96'], fw: ['#6d8cab', '#3d5977', '#2a4159'], gold: ['#f8d57e', '#cf9d34', '#94691a'],
  mod: ['#8a6dc4', '#563f84', '#392a5a'], fa: ['#1c2a3d', '#142031', '#0f1926'], fb: ['#182536', '#121c2b', '#0d1622'],
};
let g = '';
for (let x = 0; x < 5; x++) for (let y = 0; y < 5; y++) {
  if ((x === 0 && y === 4) || (x === 4 && y === 0)) continue;
  g += cube(x, y, -1, (x + y) % 2 ? T.fa : T.fb);
}
const blocks = [[0, 0, 0, T.fw], [0, 2, 0, T.fw], [2, 0, 0, T.gold], [4, 2, 0, T.fw], [2, 4, 0, T.fw], [2, 2, 0, T.core], [2, 2, 1, T.core], [4, 0, 1, T.mod]]
  .sort((a, b) => a[0] + a[1] + a[2] - (b[0] + b[1] + b[2]));
for (const [x, y, z, t] of blocks) g += cube(x, y, z, t);

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630">
<defs><radialGradient id="r" cx="72%" cy="40%" r="60%"><stop offset="0" stop-color="#3ec9e6" stop-opacity=".22"/><stop offset="1" stop-color="#3ec9e6" stop-opacity="0"/></radialGradient></defs>
<rect width="1200" height="630" fill="#0b121b"/><rect width="1200" height="630" fill="url(#r)"/>
${g}
<rect x="0" y="560" width="1200" height="70" fill="#efe8d6"/>
<text x="72" y="170" font-family="Segoe UI, Arial, sans-serif" font-weight="700" font-size="26" letter-spacing="4" fill="#3ec9e6">QRM · QUORUM</text>
<text x="72" y="270" font-family="Segoe UI, Arial, sans-serif" font-weight="700" font-size="84" fill="#eaf0f6">Il Player gioca.</text>
<text x="72" y="362" font-family="Segoe UI, Arial, sans-serif" font-weight="700" font-size="84" fill="#3ec9e6">Il Character vive.</text>
<text x="72" y="440" font-family="Segoe UI, Arial, sans-serif" font-size="30" fill="#b4c3d3">Framework per server roleplay su NeoForge</text>
<text x="72" y="604" font-family="Segoe UI, Arial, sans-serif" font-weight="700" font-size="26" fill="#17202c">Apache-2.0 · open source</text>
</svg>`;

const out = fileURLToPath(new URL('../public/og.png', import.meta.url));
await sharp(Buffer.from(svg)).png({ compressionLevel: 9 }).toFile(out);
console.log('scritto', out);
