import { Router } from 'express';
import jwt from 'jsonwebtoken';
import { pool } from './db.js';

const r = Router();

function auth(req, res, next) {
  const h = req.headers.authorization || '';
  const prefix = 'Bearer ';
  let token = '';
  if (h && h.substring(0, prefix.length) === prefix) token = h.substring(prefix.length);
  try {
    req.user = jwt.verify(token, process.env.JWT_SECRET);
    next();
  } catch {
    return res.status(401).json({ error: '未登录' });
  }
}

// 发起通话（A 呼叫 B）
r.post('/call/start', auth, async (req, res) => {
  const to = Number(req.body?.to_user_id);
  if (!to || to === req.user.id) return res.status(400).json({ error: '参数错误' });
  const [existing] = await pool.query(
    `SELECT id FROM calls WHERE ((caller_id=? AND callee_id=?) OR (caller_id=? AND callee_id=?)) AND status IN ('ringing','active')`,
    [req.user.id, to, to, req.user.id]
  );
  if (existing.length > 0) return res.status(400).json({ error: '已有进行中的通话' });
  const [r2] = await pool.query(`INSERT INTO calls (caller_id, callee_id, status) VALUES (?,?, 'ringing')`, [req.user.id, to]);
  res.json({ ok: true, call_id: r2.insertId });
});

// 检查来电（B 轮询）
r.get('/call/incoming', auth, async (req, res) => {
  const [rows] = await pool.query(
    `SELECT c.id, c.caller_id, c.status, u.username, u.nickname, u.avatar
     FROM calls c JOIN users u ON u.id=c.caller_id
     WHERE c.callee_id=? AND c.status='ringing' ORDER BY c.id DESC LIMIT 1`,
    [req.user.id]
  );
  if (rows.length === 0) return res.json({ ok: true, call: null });
  const c = rows[0];
  res.json({
    ok: true,
    call: {
      call_id: c.id,
      caller_id: c.caller_id,
      username: c.username,
      nickname: c.nickname || c.username,
      avatar: c.avatar || '',
    },
  });
});

// 接听 / 拒绝 / 取消
r.post('/call/respond', auth, async (req, res) => {
  const { call_id, action } = req.body || {};
  const status = action === 'answer' ? 'active'
    : action === 'reject' ? 'rejected'
    : action === 'cancel' ? 'cancelled' : null;
  if (!status) return res.status(400).json({ error: '参数错误' });
  await pool.query(`UPDATE calls SET status=? WHERE id=? AND (caller_id=? OR callee_id=?)`, [status, call_id, req.user.id, req.user.id]);
  res.json({ ok: true });
});

// 发送信令（offer / answer / candidate）
r.post('/call/signal', auth, async (req, res) => {
  const { call_id, type, data } = req.body || {};
  if (!call_id || !type || !data) return res.status(400).json({ error: '参数错误' });
  const [c] = await pool.query(`SELECT caller_id, callee_id FROM calls WHERE id=?`, [call_id]);
  if (!c[0]) return res.status(404).json({ error: '通话不存在' });
  const to = c[0].caller_id === req.user.id ? c[0].callee_id : c[0].caller_id;
  await pool.query(
    `INSERT INTO call_signals (call_id, from_id, to_id, type, data) VALUES (?,?,?,?,?)`,
    [call_id, req.user.id, to, String(type).slice(0, 16), String(data).slice(0, 20000)]
  );
  res.json({ ok: true });
});

// 拉取发给我的信令（双方轮询）
r.get('/call/signals', auth, async (req, res) => {
  const call_id = Number(req.query.call_id || 0);
  const after = Number(req.query.after || 0);
  const [rows] = await pool.query(
    `SELECT id, from_id, type, data FROM call_signals WHERE call_id=? AND to_id=? AND id>? ORDER BY id ASC LIMIT 100`,
    [call_id, req.user.id, after]
  );
  res.json({ ok: true, signals: rows });
});

// 通话状态（双方轮询，检测挂断）
r.get('/call/status/:id', auth, async (req, res) => {
  const [rows] = await pool.query(`SELECT status FROM calls WHERE id=? AND (caller_id=? OR callee_id=?)`, [req.params.id, req.user.id, req.user.id]);
  if (rows.length === 0) return res.status(404).json({ error: '通话不存在' });
  res.json({ ok: true, status: rows[0].status });
});

// 挂断
r.post('/call/end', auth, async (req, res) => {
  const { call_id } = req.body || {};
  await pool.query(`UPDATE calls SET status='ended' WHERE id=? AND (caller_id=? OR callee_id=?)`, [call_id, req.user.id, req.user.id]);
  res.json({ ok: true });
});

export default r;
