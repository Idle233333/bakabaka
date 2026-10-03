const fs = require('fs');
const src = 'C:/Windows/Fonts/msyh.ttc';
const out = 'C:/Users/Administrator/touhou-aichat/app/src/main/res/font/msyh.ttf';

const buf = fs.readFileSync(src);
if (buf.toString('ascii', 0, 4) !== 'ttcf') { console.error('not a TTC'); process.exit(1); }
const numFonts = buf.readUInt32BE(8);
const firstOffset = buf.readUInt32BE(12);

// sfnt table directory at firstOffset
let p = firstOffset;
const sfntVersion = buf.readUInt32BE(p); p += 4;
const numTables = buf.readUInt16BE(p); p += 2;
p += 6; // searchRange + entrySelector + rangeShift

const tables = [];
for (let i = 0; i < numTables; i++) {
  const tag = buf.toString('ascii', p, p + 4); p += 4;
  const checksum = buf.readUInt32BE(p); p += 4;
  const offset = buf.readUInt32BE(p); p += 4;
  const length = buf.readUInt32BE(p); p += 4;
  tables.push({ tag, checksum, offset, length });
}

tables.sort((a, b) => a.tag < b.tag ? -1 : (a.tag > b.tag ? 1 : 0));

const maxPow = Math.floor(Math.log2(numTables));
const searchRange = Math.pow(2, maxPow) * 16;
const entrySelector = maxPow;
const rangeShift = numTables * 16 - searchRange;

const headerSize = 12 + 16 * numTables;
const placed = [];
let dataOffset = headerSize;
for (const t of tables) {
  while (dataOffset % 4 !== 0) dataOffset++;
  placed.push({ tag: t.tag, checksum: t.checksum, offset: dataOffset, length: t.length });
  dataOffset += t.length;
}
const total = dataOffset;

const outBuf = Buffer.alloc(total);
outBuf.writeUInt32BE(sfntVersion, 0);
outBuf.writeUInt16BE(numTables, 4);
outBuf.writeUInt16BE(searchRange, 6);
outBuf.writeUInt16BE(entrySelector, 8);
outBuf.writeUInt16BE(rangeShift, 10);
let rp = 12;
for (const t of placed) {
  outBuf.write(t.tag, rp, 'ascii'); rp += 4;
  outBuf.writeUInt32BE(t.checksum, rp); rp += 4;
  outBuf.writeUInt32BE(t.offset, rp); rp += 4;
  outBuf.writeUInt32BE(t.length, rp); rp += 4;
}
for (let i = 0; i < placed.length; i++) {
  const t = tables[i];
  buf.copy(outBuf, placed[i].offset, t.offset, t.offset + t.length);
}
fs.writeFileSync(out, outBuf);
console.log('extracted TTF: ' + out);
console.log('numFonts=' + numFonts + ' numTables=' + numTables + ' size=' + total);
console.log('sfntMagic=' + outBuf.subarray(0, 4).toString('hex'));
