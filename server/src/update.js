import { Router } from 'express';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const r = Router();
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const updatesDir = path.join(__dirname, '..', 'updates');

function key() {
  const hex = process.env.UPDATE_KEY || '';
  if (hex.length !== 64) throw new Error('UPDATE_KEY 必须是 64 位十六进制（32 字节）');
  return Buffer.from(hex, 'hex');
}

// 最新版本信息
r.get('/update/latest', (req, res) => {
  try {
    const m = JSON.parse(fs.readFileSync(path.join(updatesDir, 'manifest.json'), 'utf8'));
    res.json({ ok: true, ...m });
  } catch {
    res.status(404).json({ error: '暂无可用更新' });
  }
});

// 加密 APK 下载（AES-256-GCM，载荷 = iv(12) + tag(16) + 密文）
r.get('/update/download', (req, res) => {
  try {
    const m = JSON.parse(fs.readFileSync(path.join(updatesDir, 'manifest.json'), 'utf8'));
    const data = fs.readFileSync(path.join(updatesDir, m.file));
    const iv = crypto.randomBytes(12);
    const cipher = crypto.createCipheriv('aes-256-gcm', key(), iv);
    const enc = Buffer.concat([cipher.update(data), cipher.final()]);
    const tag = cipher.getAuthTag();
    const payload = Buffer.concat([iv, tag, enc]);
    res.setHeader('Content-Type', 'application/octet-stream');
    res.setHeader('Content-Length', payload.length);
    res.send(payload);
  } catch (e) {
    res.status(500).json({ error: '下载失败: ' + e.message });
  }
});

export default r;
