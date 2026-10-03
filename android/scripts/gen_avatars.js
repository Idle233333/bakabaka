const fs = require('fs');
const path = require('path');

const ICONS_DIR = 'C:/Users/Administrator/Desktop/LostWord/icons';
const DRAWABLE_DIR = 'C:/Users/Administrator/touhou-aichat/app/src/main/res/drawable';
const OUT_KT = 'C:/Users/Administrator/touhou-aichat/app/src/main/java/com/touhou/aichat/CharacterAvatars.kt';
const CSV = 'C:/Users/Administrator/Desktop/thwiki/characters_relations.csv';

function sanitize(name) {
  const base = name.replace(/\.png$/i, '');
  return base.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
}

function norm(s) {
  return s.toLowerCase().replace(/[^a-z0-9]/g, '');
}

function words(s) {
  return s.toLowerCase().replace(/[^a-z ]/g, ' ').split(/\s+/).filter(Boolean);
}

// forms for a name (as-is norm, reversed-words norm, sorted-words)
function formsOf(name) {
  const ws = words(name);
  const set = new Set();
  set.add(norm(name));
  if (ws.length > 1) set.add(ws.slice().reverse().join(''));
  set.add(ws.slice().sort().join(''));
  return Array.from(set);
}

// parse CSV: chinese name -> english variants
const raw = fs.readFileSync(CSV, 'utf8').replace(/^\uFEFF/, '');
const csvRows = [];
for (const line of raw.split(/\r?\n/)) {
  if (!line.trim()) continue;
  const cols = line.split(',');
  const cn = (cols[0] || '').trim();
  const en = (cols[2] || '').trim();
  if (!cn) continue;
  const ens = en ? en.split(/[，/]/).map((s) => s.trim()).filter(Boolean) : [];
  csvRows.push({ cn, ens });
}

// build lookup: form -> cn
const lookup = new Map();
for (const row of csvRows) {
  for (const en of row.ens) {
    for (const f of formsOf(en)) {
      if (!lookup.has(f)) lookup.set(f, row.cn);
    }
  }
}

// manual overrides for romanization variants that exact match misses
const overrides = {
  'Chimata Tenkyuu': '天弓千亦',
  'Fujiwara no Mokou': '藤原妹红',
  'Hong Meiling': '红美铃',
  'Joon Yorigami': '依神女苑',
  'Mayumi Joutouguu': '杖刀偶磨弓',
  'Mike Goutokuji': '豪德寺三花',
  'Minamitsu Murasa': '村纱水蜜',
  'Momiji Inubashiri': '犬走椛',
  'Nitori Kawashiro': '河城荷取',
  'Shizuha Aki': '秋静叶',
  'Soga no Tojiko': '苏我屠自古',
  'Yachie Kicchou': '吉吊八千慧',
  'Yuugi Hoshiguma': '星熊勇仪',
  'Yuuka Kazami': '风见幽香',
  'Yuuma Toutetsu': '饕餮尤魔'
};

// list L1 icons
const iconFiles = fs.readdirSync(ICONS_DIR).filter((f) => /^Icon L1 .+\.png$/i.test(f));

fs.mkdirSync(DRAWABLE_DIR, { recursive: true });

const map = {};
const unmatched = [];
for (const f of iconFiles) {
  const iconName = f.replace(/^Icon L1 /i, '').replace(/\.png$/i, '').trim();
  let cn = overrides[iconName] || null;
  if (!cn) {
    for (const c of formsOf(iconName)) {
      if (lookup.has(c)) { cn = lookup.get(c); break; }
    }
  }
  const drawableName = sanitize(f);
  fs.copyFileSync(path.join(ICONS_DIR, f), path.join(DRAWABLE_DIR, drawableName + '.png'));
  if (cn) {
    if (!map[cn]) map[cn] = drawableName;
  } else {
    unmatched.push(f);
  }
}

console.log('Total L1 icons: ' + iconFiles.length);
console.log('Mapped to characters: ' + Object.keys(map).length);
console.log('Unmatched icons: ' + unmatched.length);
if (unmatched.length) console.log('UNMATCHED:\n' + unmatched.join('\n'));

let kt = 'package com.touhou.aichat\n\n';
kt += 'import com.touhou.aichat.R\n\n';
kt += 'object CharacterAvatars {\n';
kt += '    val map: Map<String, Int> = mapOf(\n';
for (const cn of Object.keys(map).sort()) {
  kt += '        "' + cn + '" to R.drawable.' + map[cn] + ',\n';
}
kt += '    )\n';
kt += '}\n';
fs.writeFileSync(OUT_KT, kt, 'utf8');
console.log('Wrote CharacterAvatars.kt');
