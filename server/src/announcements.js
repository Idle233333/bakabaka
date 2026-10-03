import { Router } from 'express';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const r = Router();
const __dirname = path.dirname(fileURLToPath(import.meta.url));

r.get('/announcements', (req, res) => {
  try {
    const data = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'announcements.json'), 'utf8'));
    res.json({ ok: true, announcements: data });
  } catch (e) {
    res.json({ ok: true, announcements: [] });
  }
});

export default r;
