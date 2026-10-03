import { Router } from 'express';
import jwt from 'jsonwebtoken';
import { pool } from './db.js';
import { loadCharacters } from './characters.js';

const r = Router();

function auth(req, res, next) {
  const h = req.headers.authorization || '';
  const token = h.startsWith('Bearer ') ? h.slice(7) : '';
  try {
    req.user = jwt.verify(token, process.env.JWT_SECRET);
    next();
  } catch {
    return res.status(401).json({ error: '未登录' });
  }
}

// 全部可聊角色 = 官方角色 + 用户自建 AI 角色
r.get('/characters', async (req, res) => {
  const chars = loadCharacters();
  const fixed = Object.keys(chars).map(name => ({
    name,
    type: 'official',
    species: chars[name].species || '',
    job: chars[name].job || '',
    ability: chars[name].ability || '',
  }));
  const [agents] = await pool.query(`SELECT id, name, persona, avatar FROM agents ORDER BY id DESC`);
  const custom = agents.map(a => ({
    name: a.name,
    type: 'custom',
    species: '自定义角色',
    job: '',
    ability: a.persona || '',
    avatar: a.avatar || '',
  }));
  res.json({ ok: true, characters: [...custom, ...fixed] });
});

// 用户自建 AI 角色
r.get('/agents', auth, async (req, res) => {
  const [rows] = await pool.query(`SELECT id, name, persona, avatar, created_at FROM agents WHERE owner_id=? ORDER BY id DESC`, [req.user.id]);
  res.json({ ok: true, agents: rows });
});

r.post('/agents', auth, async (req, res) => {
  const { name, persona, avatar } = req.body || {};
  const n = String(name || '').trim().slice(0, 64);
  const p = String(persona || '').trim().slice(0, 2000);
  if (!n) return res.status(400).json({ error: '角色名不能为空' });
  if (!p) return res.status(400).json({ error: '角色设定不能为空' });
  // 名字不能与官方角色冲突
  if (loadCharacters()[n]) return res.status(409).json({ error: '该名字与官方角色冲突，换个名字吧' });
  // 每日创建上限（admin 除外）
  const [u] = await pool.query(`SELECT is_admin FROM users WHERE id=?`, [req.user.id]);
  const isAdmin = u[0]?.is_admin === 1;
  const [cnt] = await pool.query(`SELECT COUNT(*) AS c FROM agents WHERE owner_id=? AND created_at >= CURDATE()`, [req.user.id]);
  if (!isAdmin && Number(cnt[0].c) >= Number(process.env.AGENT_DAILY_LIMIT || 3)) {
    return res.status(429).json({ error: `每天最多创建 ${process.env.AGENT_DAILY_LIMIT || 3} 个角色，明天再试吧` });
  }
  try {
    const [r2] = await pool.query(
      `INSERT INTO agents (owner_id, name, persona, avatar) VALUES (?,?,?,?)`,
      [req.user.id, n, p, String(avatar || '')]
    );
    res.json({ ok: true, id: r2.insertId });
  } catch (e) {
    if (String(e.code) === 'ER_DUP_ENTRY') return res.status(409).json({ error: '这个角色名已经被占用了' });
    throw e;
  }
});

r.delete('/agents/:id', auth, async (req, res) => {
  await pool.query(`DELETE FROM agents WHERE id=? AND owner_id=?`, [req.params.id, req.user.id]);
  res.json({ ok: true });
});

export default r;
