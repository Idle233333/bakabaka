import { Router } from 'express';
import jwt from 'jsonwebtoken';
import { pool } from './db.js';

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

// 搜索用户（用户名精准 / 昵称模糊，@ 开头表示精准搜用户名）
r.get('/users', auth, async (req, res) => {
  let q = String(req.query.q || '').trim();
  if (!q) return res.json({ ok: true, users: [] });
  let rows;
  if (q.charAt(0) === '@') {
    const uname = q.slice(1).trim();
    [rows] = await pool.query(
      `SELECT id, username, nickname, avatar FROM users WHERE username=? AND id != ? LIMIT 20`,
      [uname, req.user.id]
    );
  } else {
    [rows] = await pool.query(
      `SELECT id, username, nickname, avatar FROM users WHERE (username=? OR nickname LIKE ?) AND id != ? LIMIT 20`,
      [q, `%${q}%`, req.user.id]
    );
  }
  res.json({ ok: true, users: rows.map(u => ({ id: u.id, username: u.username, nickname: u.nickname || u.username, avatar: u.avatar || '' })) });
});

// 用户主页（查看他人公开资料，含 IP 属地）
r.get('/user/:id', auth, async (req, res) => {
  const id = Number(req.params.id);
  const [rows] = await pool.query(
    `SELECT id, username, nickname, avatar, signature, gender, company, address, ip_location FROM users WHERE id=?`,
    [id]
  );
  const u = rows[0];
  if (!u) return res.status(404).json({ error: '用户不存在' });
  res.json({
    ok: true,
    user: {
      id: u.id,
      username: u.username,
      nickname: u.nickname || u.username,
      avatar: u.avatar || '',
      signature: u.signature || '',
      gender: u.gender || '',
      company: u.company || '',
      address: u.address || '',
      ip_location: u.ip_location || '',
    },
  });
});

// 发私信
r.post('/dm', auth, async (req, res) => {
  const { to_user_id, message } = req.body || {};
  const to = Number(to_user_id);
  if (!to || !message || !String(message).trim()) return res.status(400).json({ error: '参数缺失' });
  if (to === req.user.id) return res.status(400).json({ error: '不能给自己发消息' });
  const content = String(message).trim().slice(0, 2000);
  await pool.query(`INSERT INTO dm_messages (sender_id, receiver_id, content_type, content) VALUES (?,?, 'text', ?)`, [req.user.id, to, content]);
  res.json({ ok: true });
});

// 发私信图片
r.post('/dm/image', auth, async (req, res) => {
  const { to_user_id, image } = req.body || {};
  const to = Number(to_user_id);
  if (!to || !image) return res.status(400).json({ error: '参数缺失' });
  if (to === req.user.id) return res.status(400).json({ error: '不能给自己发消息' });
  const imageBase64 = String(image).replace(/^data:image\/\w+;base64,/, '');
  if (imageBase64.length > 3000000) return res.status(400).json({ error: '图片过大' });
  await pool.query(`INSERT INTO dm_messages (sender_id, receiver_id, content_type, content) VALUES (?,?, 'image', ?)`, [req.user.id, to, imageBase64]);
  res.json({ ok: true });
});

// 私信会话列表
r.get('/dm/conversations', auth, async (req, res) => {
  const [rows] = await pool.query(
    `SELECT t.other_id, t.last_id, m.content AS last_content, m.content_type, m.created_at AS last_time,
            u.username, u.nickname, u.avatar
     FROM (
       SELECT IF(sender_id=?, receiver_id, sender_id) AS other_id, MAX(id) AS last_id
       FROM dm_messages WHERE sender_id=? OR receiver_id=?
       GROUP BY other_id
     ) t
     JOIN dm_messages m ON m.id = t.last_id
     JOIN users u ON u.id = t.other_id
     ORDER BY t.last_id DESC`,
    [req.user.id, req.user.id, req.user.id]
  );
  res.json({ ok: true, conversations: rows.map(r => ({
    user_id: r.other_id,
    username: r.username,
    nickname: r.nickname || r.username,
    avatar: r.avatar || '',
    last_content: r.last_content,
    last_content_type: r.content_type,
    last_time: r.last_time,
  })) });
});

// 与某人的私信消息
r.get('/dm/messages', auth, async (req, res) => {
  const withId = Number(req.query.with || 0);
  if (!withId) return res.status(400).json({ error: '缺少对方参数' });
  const after = Number(req.query.after || 0);
  const [rows] = await pool.query(
    `SELECT id, sender_id, receiver_id, content_type, content, created_at
     FROM dm_messages
     WHERE ((sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)) AND id>?
     ORDER BY id ASC LIMIT 200`,
    [req.user.id, withId, withId, req.user.id, after]
  );
  res.json({ ok: true, messages: rows });
});

// 发给我的新私信（通知轮询用）
r.get('/dm/unread', auth, async (req, res) => {
  const after = Number(req.query.after || 0);
  const [rows] = await pool.query(
    `SELECT m.id, m.sender_id, m.content_type, m.content, m.created_at, u.username, u.nickname, u.avatar
     FROM dm_messages m JOIN users u ON u.id=m.sender_id
     WHERE m.receiver_id=? AND m.id>? ORDER BY m.id ASC LIMIT 50`,
    [req.user.id, after]
  );
  res.json({ ok: true, messages: rows.map(m => ({
    id: m.id,
    sender_id: m.sender_id,
    username: m.username,
    nickname: m.nickname || m.username,
    avatar: m.avatar || '',
    content_type: m.content_type,
    content: m.content_type === 'image' ? '[图片]' : m.content,
    created_at: m.created_at,
  })) });
});

// 设置/删除备注（空字符串=删除）
r.post('/remark', auth, async (req, res) => {
  const target = Number(req.body?.target_id);
  const remark = String(req.body?.remark || '').trim().slice(0, 64);
  if (!target || target === req.user.id) return res.status(400).json({ error: '参数错误' });
  if (remark) {
    await pool.query(
      `INSERT INTO remarks (owner_id, target_id, remark) VALUES (?,?,?) ON DUPLICATE KEY UPDATE remark=?`,
      [req.user.id, target, remark, remark]
    );
  } else {
    await pool.query(`DELETE FROM remarks WHERE owner_id=? AND target_id=?`, [req.user.id, target]);
  }
  res.json({ ok: true });
});

// 我的备注列表
r.get('/remarks', auth, async (req, res) => {
  const [rows] = await pool.query(`SELECT target_id, remark FROM remarks WHERE owner_id=?`, [req.user.id]);
  res.json({ ok: true, remarks: rows.map(r => ({ target_id: r.target_id, remark: r.remark })) });
});

export default r;
