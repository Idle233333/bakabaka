const fs = require('fs');
const raw = fs.readFileSync('C:/Users/Administrator/Desktop/thwiki/characters_relations.csv', 'utf8').replace(/^\uFEFF/, '');
const lines = raw.split(/\r?\n/);
console.log('HEADER:', lines[0]);
const out = [];
for (let i = 1; i < lines.length; i++) {
  const line = lines[i];
  if (!line.trim()) continue;
  const cols = line.split(',');
  const name = (cols[0] || '').trim();
  const en = (cols[2] || '').trim();
  if (!name) continue;
  out.push(name + '\t=>\t' + en);
}
console.log(out.join('\n'));
console.log('TOTAL_ROWS:', out.length);
