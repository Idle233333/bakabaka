import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
let cache = null;

export function loadCharacters() {
  if (!cache) {
    cache = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'characters.json'), 'utf8'));
  }
  return cache;
}

export function listCharacters() {
  const c = loadCharacters();
  return Object.keys(c).map(name => ({
    name,
    species: c[name].species || '',
    job: c[name].job || '',
    ability: c[name].ability || '',
  }));
}
