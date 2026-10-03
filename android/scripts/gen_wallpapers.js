const fs = require('fs');
const path = require('path');

const SRC_DIR = 'C:/Users/Administrator/Desktop/文件夹';
const DRAWABLE_DIR = 'C:/Users/Administrator/touhou-aichat/app/src/main/res/drawable';
const OUT_KT = 'C:/Users/Administrator/touhou-aichat/app/src/main/java/com/touhou/aichat/Wallpapers.kt';

function sanitize(filename) {
  let stem = filename.replace(/\.webp$/i, '');
  stem = stem.toLowerCase();
  stem = stem.replace(/[^\x00-\x7F]/g, '');   // 去中文/非 ASCII
  stem = stem.replace(/[^a-z0-9]+/g, '_');   // 去空格/括号/连字符
  stem = stem.replace(/^_+|_+$/g, '');
  if (!stem) stem = 'wp';
  if (/^[0-9]/.test(stem)) stem = 'wp_' + stem;
  return stem;
}

const files = fs.readdirSync(SRC_DIR)
  .filter((f) => f.toLowerCase().endsWith('.webp'))
  .sort();

fs.mkdirSync(DRAWABLE_DIR, { recursive: true });

const names = [];
for (const f of files) {
  const name = sanitize(f);
  fs.copyFileSync(path.join(SRC_DIR, f), path.join(DRAWABLE_DIR, name + '.webp'));
  names.push(name);
  console.log('OK ' + f + ' -> ' + name);
}

let kt = 'package com.touhou.aichat\n\n';
kt += 'import com.touhou.aichat.R\n\n';
kt += 'object Wallpapers {\n';
kt += '    val list: List<Int> = listOf(\n';
for (const n of names) {
  kt += '        R.drawable.' + n + ',\n';
}
kt += '    )\n';
kt += '}\n';
fs.writeFileSync(OUT_KT, kt, 'utf8');
console.log('Wrote Wallpapers.kt with ' + names.length + ' wallpapers');
