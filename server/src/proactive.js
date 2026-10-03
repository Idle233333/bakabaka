import { pool } from './db.js';
import { proactiveMessage } from './ai.js';

// 角色「偶尔主动发消息」：每隔一段时间，按概率挑一个活跃用户，让角色主动说一句
export async function runProactive() {
  const chance = Number(process.env.PROACTIVE_CHANCE || 0.3);
  if (Math.random() > chance) return;
  try {
    const [users] = await pool.query(
      `SELECT DISTINCT u.id, u.username, u.persona, u.is_admin FROM users u JOIN messages m ON m.user_id=u.id ORDER BY RAND() LIMIT 1`
    );
    if (users.length === 0) return;
    const u = users[0];
    const limit = Number(process.env.MESSAGE_LIMIT || 100);
    const [cnt] = await pool.query(`SELECT COUNT(*) AS c FROM messages WHERE user_id=?`, [u.id]);
    // 达到上限的用户（非 admin）不再主动打扰
    if (u.is_admin !== 1 && Number(cnt[0].c) >= limit) return;

    const [hist] = await pool.query(
      `SELECT role, content FROM messages WHERE user_id=? ORDER BY id DESC LIMIT 10`,
      [u.id]
    );
    const history = hist.reverse().map(m => ({ role: m.role, content: m.content }));
    const text = await proactiveMessage(u.persona, history);
    await pool.query(`INSERT INTO messages (user_id, role, content) VALUES (?, 'assistant', ?)`, [u.id, text]);
    console.log(`[proactive] -> ${u.username}: ${String(text).slice(0, 60)}`);
  } catch (e) {
    console.error('[proactive] error:', e.message);
  }
}
