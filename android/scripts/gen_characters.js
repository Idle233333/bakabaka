const fs = require('fs');
const path = require('path');

const ICONS_DIR = 'C:/Users/Administrator/Desktop/LostWord/icons';
const DRAWABLE_DIR = 'C:/Users/Administrator/touhou-aichat/app/src/main/res/drawable';
const OUT_KT = 'C:/Users/Administrator/touhou-aichat/app/src/main/java/com/touhou/aichat/Characters.kt';
const CSV = 'C:/Users/Administrator/Desktop/thwiki/characters_relations.csv';

// [chineseName (exact CSV 名称), iconEnglishName (matches "Icon L1 <X>.png")]
const curated = [
  ['博丽灵梦', 'Reimu Hakurei'],
  ['雾雨魔理沙', 'Marisa Kirisame'],
  ['琪露诺', 'Cirno'],
  ['十六夜咲夜', 'Sakuya Izayoi'],
  ['蕾米莉亚·斯卡蕾特', 'Remilia Scarlet'],
  ['芙兰朵露·斯卡蕾特', 'Flandre Scarlet'],
  ['东风谷早苗', 'Sanae Kochiya'],
  ['魂魄妖梦', 'Youmu Konpaku'],
  ['八云紫', 'Yukari Yakumo'],
  ['八云蓝', 'Ran Yakumo'],
  ['橙', 'Chen'],
  ['爱丽丝·玛格特洛依德', 'Alice Margatroid'],
  ['红美铃', 'Hong Meiling'],
  ['帕秋莉·诺蕾姬', 'Patchouli Knowledge'],
  ['小恶魔', 'Koakuma'],
  ['古明地觉', 'Satori Komeiji'],
  ['古明地恋', 'Koishi Komeiji'],
  ['射命丸文', 'Aya Shameimaru'],
  ['八意永琳', 'Eirin Yagokoro'],
  ['藤原妹红', 'Fujiwara no Mokou'],
  ['铃仙·优昙华院·因幡', 'Reisen Udongein Inaba'],
  ['因幡帝', 'Tewi Inaba'],
  ['蓬莱山辉夜', 'Kaguya Houraisan'],
  ['洩矢诹访子', 'Suwako Moriya'],
  ['八坂神奈子', 'Kanako Yasaka'],
  ['比那名居天子', 'Tenshi Hinanawi'],
  ['西行寺幽幽子', 'Yuyuko Saigyouji'],
  ['伊吹萃香', 'Suika Ibuki'],
  ['露米娅', 'Rumia'],
  ['河城荷取', 'Nitori Kawashiro'],
];

// Build set of valid Chinese names from CSV (col 0)
const raw = fs.readFileSync(CSV, 'utf8').replace(/^\uFEFF/, '');
const csvNames = new Set();
for (const line of raw.split(/\r?\n/)) {
  if (!line.trim()) continue;
  csvNames.add(line.split(',')[0].trim());
}

function sanitize(name) {
  const base = name.replace(/\.png$/i, '');
  return base.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
}

fs.mkdirSync(DRAWABLE_DIR, { recursive: true });
const entries = [];
const warnings = [];

for (const [cn, en] of curated) {
  if (!csvNames.has(cn)) {
    warnings.push('CSV_NAME_MISMATCH: ' + cn);
  }
  const srcName = 'Icon L1 ' + en + '.png';
  const src = path.join(ICONS_DIR, srcName);
  if (!fs.existsSync(src)) {
    warnings.push('MISSING_ICON: ' + srcName);
    continue;
  }
  const drawableName = sanitize(srcName);
  fs.copyFileSync(src, path.join(DRAWABLE_DIR, drawableName + '.png'));
  entries.push({ cn, en, drawableName });
  console.log('OK ' + cn + ' -> ' + drawableName);
}

let kt = 'package com.touhou.aichat\n\n';
kt += 'import com.touhou.aichat.R\n\n';
kt += 'data class Character(\n';
kt += '    val chineseName: String,\n';
kt += '    val englishName: String,\n';
kt += '    val drawableRes: Int\n';
kt += ')\n\n';
kt += 'object Characters {\n';
kt += '    val all: List<Character> = listOf(\n';
for (const e of entries) {
  kt += '        Character("' + e.cn + '", "' + e.en + '", R.drawable.' + e.drawableName + '),\n';
}
kt += '    )\n';
kt += '}\n';
fs.writeFileSync(OUT_KT, kt, 'utf8');
console.log('Wrote Characters.kt with ' + entries.length + ' characters');
if (warnings.length) {
  console.log('WARNINGS:');
  warnings.forEach((w) => console.log('  ' + w));
}
