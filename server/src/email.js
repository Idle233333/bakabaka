import nodemailer from 'nodemailer';
import crypto from 'node:crypto';

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT || 465),
  secure: process.env.SMTP_SECURE !== 'false',
  auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
});

// 内存存储验证码：email -> {code, expires}
const codes = new Map();
const TTL_MS = 10 * 60 * 1000; // 10 分钟

export async function sendCode(email) {
  const code = String(crypto.randomInt(100000, 1000000));
  codes.set(email, { code, expires: Date.now() + TTL_MS });
  await transporter.sendMail({
    from: process.env.SMTP_FROM,
    to: email,
    subject: '【AI Chat】邮箱验证码',
    text: `你的验证码是 ${code}，10 分钟内有效。如非本人操作请忽略。`,
  });
  return code;
}

export function verifyCode(email, code) {
  const rec = codes.get(email);
  if (!rec) return false;
  if (Date.now() > rec.expires) { codes.delete(email); return false; }
  if (String(rec.code) !== String(code)) return false;
  codes.delete(email);
  return true;
}
