import { Router } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { pool } from './db.js';
import { sendCode, verifyCode } from './email.js';
import { clientIp, lookupIpLocation } from './ip.js';

const r = Router();
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function isAdminUsername(username) {
  const list = (process.env.ADMIN_ACCOUNTS || '').split(',').map(s => s.trim().toLowerCase()).filter(Boolean);
  return list.includes(String(username).trim().toLowerCase());
}

// 发送验证码
r.post('/send-code', async (req, res) => {
  const { email } = req.body || {};
  if (!email || !EMAIL_RE.test(email)) return res.status(400).json({ error: '邮箱格式错误' });
  try {
    await sendCode(email);
    res.json({ ok: true, msg: '验证码已发送' });
  } catch (e) {
    res.status(502).json({ error: '邮件发送失败，请检查 SMTP 配置' });
  }
});

// 注册：用户名 + 密码 + 邮箱 + 验证码
r.post('/register', async (req, res) => {
  const { username, password, email, code } = req.body || {};
  if (!username || !password || !email || !code) return res.status(400).json({ error: '参数缺失' });
  if (String(username).trim().length < 2) return res.status(400).json({ error: '用户名至少 2 个字符' });
  if (String(password).length < 6) return res.status(400).json({ error: '密码至少 6 位' });
  if (!EMAIL_RE.test(email)) return res.status(400).json({ error: '邮箱格式错误' });
  if (!verifyCode(email, code)) return res.status(400).json({ error: '验证码错误或已过期' });

  const hash = bcrypt.hashSync(password, 10);
  const isAdmin = isAdminUsername(username) ? 1 : 0;
  const loc = await lookupIpLocation(clientIp(req));
  try {
    await pool.query(
      `INSERT INTO users (username, email, password_hash, is_admin, ip_location) VALUES (?,?,?,?,?)`,
      [String(username).trim(), email, hash, isAdmin, loc]
    );
  } catch (e) {
    if (String(e.code) === 'ER_DUP_ENTRY') return res.status(409).json({ error: '用户名或邮箱已被注册' });
    throw e;
  }
  res.json({ ok: true });
});

// 登录：账号（用户名或邮箱）+ 密码
r.post('/login', async (req, res) => {
  const { account, password } = req.body || {};
  if (!account || !password) return res.status(400).json({ error: '参数缺失' });
  const [rows] = await pool.query(
    `SELECT id, username, email, password_hash, is_admin FROM users WHERE username=? OR email=?`,
    [account, account]
  );
  const row = rows[0];
  if (!row) return res.status(401).json({ error: '账号不存在' });
  if (!bcrypt.compareSync(password, row.password_hash)) return res.status(401).json({ error: '密码错误' });
  const token = jwt.sign({ id: row.id, username: row.username }, process.env.JWT_SECRET, { expiresIn: '30d' });
  // 更新 IP 属地（异步，不阻塞登录）
  lookupIpLocation(clientIp(req)).then(loc => {
    if (loc) pool.query(`UPDATE users SET ip_location=? WHERE id=?`, [loc, row.id]).catch(() => {});
  });
  res.json({ ok: true, token, username: row.username, email: row.email, is_admin: row.is_admin === 1 });
});

export default r;
