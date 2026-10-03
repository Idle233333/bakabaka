import { Router } from 'express';
import jwt from 'jsonwebtoken';
import { pool } from './db.js';
import { chatWithAI, chatWithImage } from './ai.js';

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

function messageLimit() {
  return Number(process.env.MESSAGE_LIMIT || 100);
}

// 今日 AI 对话条数检查（次日 0 点自动重置）
async function checkDailyLimit(req, res) {
  const [u] = await pool.query(`SELECT is_admin FROM users WHERE id=?`, [req.user.id]);
  const isAdmin = u[0]?.is_admin === 1;
  const [cnt] = await pool.query(`SELECT COUNT(*) AS c FROM messages WHERE user_id=? AND created_at >= CURDATE()`, [req.user.id]);
  const used = Number(cnt[0].c);
  if (!isAdmin && used >= messageLimit()) {
    res.status(403).json({ error: `今日对话已达上限（${messageLimit()} 条），明日 0 点重置` });
    return null;
  }
  return used;
}

// 判断 persona 是否为用户自建角色，返回其设定描述
async function resolvePersona(persona) {
  const [ag] = await pool.query(`SELECT persona FROM agents WHERE name=? LIMIT 1`, [persona]);
  return ag[0] ? ag[0].persona : null;
}

// 当前用户信息 + 今日用量
r.get('/me', auth, async (req, res) => {
  const [u] = await pool.query(
    `SELECT id, username, email, nickname, avatar, signature, gender, company, address, ip_location, is_admin FROM users WHERE id=?`,
    [req.user.id]
  );
  const user = u[0];
  if (!user) return res.status(404).json({ error: '用户不存在' });
  const [m] = await pool.query(`SELECT COUNT(*) AS c FROM messages WHERE user_id=? AND created_at >= CURDATE()`, [req.user.id]);
  res.json({
    ok: true,
    id: user.id,
    username: user.username,
    email: user.email,
    nickname: user.nickname || user.username,
    avatar: user.avatar || '',
    signature: user.signature || '',
    gender: user.gender || '',
    company: user.company || '',
    address: user.address || '',
    ip_location: user.ip_location || '',
    is_admin: user.is_admin === 1,
    message_count: Number(m[0].c),
    message_limit: messageLimit(),
  });
});

// 更新用户资料：昵称 + 自定义头像（base64，联机存储）
r.post('/me/profile', auth, async (req, res) => {
  const { nickname, avatar, signature, gender, company, address } = req.body || {};
  const fields = [];
  const params = [];
  if (nickname !== undefined) {
    const n = String(nickname).trim().slice(0, 64);
    if (!n) return res.status(400).json({ error: '昵称不能为空' });
    fields.push('nickname=?');
    params.push(n);
  }
  if (avatar !== undefined) {
    const a = String(avatar);
    if (a.length > 3000000) return res.status(400).json({ error: '头像图片过大' });
    fields.push('avatar=?');
    params.push(a);
  }
  if (signature !== undefined) {
    fields.push('signature=?');
    params.push(String(signature).slice(0, 255));
  }
  if (gender !== undefined) {
    fields.push('gender=?');
    params.push(String(gender).slice(0, 16));
  }
  if (company !== undefined) {
    fields.push('company=?');
    params.push(String(company).slice(0, 128));
  }
  if (address !== undefined) {
    fields.push('address=?');
    params.push(String(address).slice(0, 255));
  }
  if (fields.length === 0) return res.status(400).json({ error: '无更新内容' });
  params.push(req.user.id);
  await pool.query(`UPDATE users SET ${fields.join(', ')} WHERE id=?`, params);
  res.json({ ok: true });
});

// AI 会话列表（聊过哪些角色，按最后消息倒序）
r.get('/conversations', auth, async (req, res) => {
  const [rows] = await pool.query(
    `SELECT m.persona, m.id, m.role, m.content_type, m.content, m.created_at
     FROM messages m
     JOIN (SELECT persona, MAX(id) AS max_id FROM messages WHERE user_id=? GROUP BY persona) t
       ON t.persona = m.persona AND t.max_id = m.id
     ORDER BY m.id DESC`,
    [req.user.id]
  );
  res.json({ ok: true, conversations: rows });
});

// 某个角色的 AI 消息历史
r.get('/messages', auth, async (req, res) => {
  const { persona } = req.query;
  if (!persona) return res.status(400).json({ error: '缺少角色参数' });
  const after = Number(req.query.after || 0);
  const [rows] = await pool.query(
    `SELECT id, role, content_type, content, created_at FROM messages WHERE user_id=? AND persona=? AND id>? ORDER BY id ASC LIMIT 200`,
    [req.user.id, persona, after]
  );
  res.json({ ok: true, messages: rows });
});

// AI 文本聊天（指定角色，支持用户自建角色）
r.post('/chat', auth, async (req, res) => {
  const { persona, message } = req.body || {};
  if (!persona) return res.status(400).json({ error: '缺少角色参数' });
  if (!message || !String(message).trim()) return res.status(400).json({ error: '消息不能为空' });
  const content = String(message).trim().slice(0, 2000);

  const used = await checkDailyLimit(req, res);
  if (used === null) return;

  const customDesc = await resolvePersona(persona);

  await pool.query(
    `INSERT INTO messages (user_id, persona, role, content_type, content) VALUES (?,?, 'user', 'text', ?)`,
    [req.user.id, persona, content]
  );

  const [hist] = await pool.query(
    `SELECT role, content FROM messages WHERE user_id=? AND persona=? AND content_type='text' ORDER BY id DESC LIMIT 30`,
    [req.user.id, persona]
  );
  const history = hist.reverse().map(m => ({ role: m.role, content: m.content }));

  try {
    const reply = await chatWithAI(history, persona, customDesc);
    await pool.query(
      `INSERT INTO messages (user_id, persona, role, content_type, content) VALUES (?,?, 'assistant', 'text', ?)`,
      [req.user.id, persona, reply]
    );
    res.json({ ok: true, reply, message_count: used + 2, message_limit: messageLimit() });
  } catch (e) {
    res.status(502).json({ error: 'AI 调用失败: ' + e.message });
  }
});

// AI 图片聊天
r.post('/chat/image', auth, async (req, res) => {
  const { persona, image } = req.body || {};
  if (!persona) return res.status(400).json({ error: '缺少角色参数' });
  if (!image) return res.status(400).json({ error: '缺少图片' });
  const imageBase64 = String(image).replace(/^data:image\/\w+;base64,/, '');
  if (imageBase64.length > 3000000) return res.status(400).json({ error: '图片过大' });

  const used = await checkDailyLimit(req, res);
  if (used === null) return;

  const customDesc = await resolvePersona(persona);

  await pool.query(
    `INSERT INTO messages (user_id, persona, role, content_type, content) VALUES (?,?, 'user', 'image', ?)`,
    [req.user.id, persona, imageBase64]
  );

  try {
    const reply = await chatWithImage(persona, imageBase64, undefined, customDesc);
    await pool.query(
      `INSERT INTO messages (user_id, persona, role, content_type, content) VALUES (?,?, 'assistant', 'text', ?)`,
      [req.user.id, persona, reply]
    );
    res.json({ ok: true, reply, message_count: used + 2, message_limit: messageLimit() });
  } catch (e) {
    res.status(502).json({ error: 'AI 调用失败: ' + e.message });
  }
});

export default r;
