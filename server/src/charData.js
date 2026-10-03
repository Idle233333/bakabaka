import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
let cache = null;

export function getCharacter(name) {
  if (!cache) {
    const p = path.join(__dirname, '..', 'characters.json');
    cache = JSON.parse(fs.readFileSync(p, 'utf8'));
  }
  if (!name) return null;
  return cache[name] || null;
}
