import { Router } from 'express';
import jwt from 'jsonwebtoken';
import { pool } from './db.js';

const r = Router();

function auth(req, res, next) {
  const h = req.headers.authorization || '';
  const token = h.indexOf('Bearer ') === 0 ? h.substring(7) : '';
  try {
    req.user = jwt.verify(token, process.env.JWT_SECRET);
    next();
  } catch {
    return res.status(401).json({ error: '未登录' });
  }
}

function parseImages(images) {
  if (!Array.isArray(images)) return [];
  const out = [];
  for (const im of images.slice(0, 4)) {
    const s = String(im).replace(/^data:image\/\w+;base64,/, '');
    if (s.length > 0 && s.length <= 2000000) out.push(s);
  }
  return out;
}

// 发动态（支持文字 + 图片 + 投票）
r.post('/posts', auth, async (req, res) => {
  const { content, images, poll_question, poll_options } = req.body || {};
  const c = String(content || '').trim().slice(0, 2000);
  const imgs = parseImages(images);
  if (!c && imgs.length === 0 && !poll_question) return res.status(400).json({ error: '内容不能为空' });

  let pollQ = '';
  let pollOpts = null;
  if (poll_question) {
    pollQ = String(poll_question).trim().slice(0, 255);
    if (Array.isArray(poll_options)) {
      const opts = poll_options.map(o => String(o).trim()).filter(o => o.length > 0).slice(0, 10);
      if (opts.length >= 2) pollOpts = JSON.stringify(opts);
    }
  }

  const [r2] = await pool.query(
    `INSERT INTO posts (author_id, content, images, poll_question, poll_options) VALUES (?,?,?,?,?)`,
    [req.user.id, c, imgs.length > 0 ? JSON.stringify(imgs) : null, pollQ, pollOpts]
  );
  res.json({ ok: true, id: r2.insertId });
});

// 某个用户的动态列表
r.get('/posts', auth, async (req, res) => {
  const userId = Number(req.query.user_id || req.user.id);
  const [rows] = await pool.query(
    `SELECT p.id, p.author_id, p.content, p.images, p.poll_question, p.poll_options, p.created_at,
            (SELECT COUNT(*) FROM post_likes l WHERE l.post_id=p.id) AS like_count,
            (SELECT COUNT(*) FROM post_comments c WHERE c.post_id=p.id) AS comment_count,
            (SELECT COUNT(*) FROM post_likes l2 WHERE l2.post_id=p.id AND l2.user_id=?) AS liked,
            (SELECT option_index FROM post_votes v WHERE v.post_id=p.id AND v.user_id=?) AS my_vote
     FROM posts p WHERE p.author_id=? ORDER BY p.id DESC LIMIT 50`,
    [req.user.id, req.user.id, userId]
  );
  const votes = await pool.query(
    `SELECT post_id, option_index, COUNT(*) AS n FROM post_votes GROUP BY post_id, option_index`
  );
  const voteMap = {};
  for (const v of votes[0]) {
    if (!voteMap[v.post_id]) voteMap[v.post_id] = [];
    voteMap[v.post_id][v.option_index] = Number(v.n);
  }
  res.json({ ok: true, posts: rows.map(p => ({
    id: p.id,
    author_id: p.author_id,
    content: p.content,
    images: p.images ? safeJson(p.images) : [],
    poll_question: p.poll_question || '',
    poll_options: p.poll_options ? safeJson(p.poll_options) : [],
    poll_votes: voteMap[p.id] || [],
    my_vote: p.my_vote === null || p.my_vote === undefined ? -1 : p.my_vote,
    created_at: p.created_at,
    like_count: p.like_count,
    comment_count: p.comment_count,
    liked: p.liked > 0,
  })) });
});

// 广场：大家分享的动态（公共 feed，含作者信息）
r.get('/posts/feed', auth, async (req, res) => {
  const [rows] = await pool.query(
    `SELECT p.id, p.author_id, p.content, p.images, p.poll_question, p.poll_options, p.created_at,
            u.username, u.nickname, u.avatar,
            (SELECT COUNT(*) FROM post_likes l WHERE l.post_id=p.id) AS like_count,
            (SELECT COUNT(*) FROM post_comments c WHERE c.post_id=p.id) AS comment_count,
            (SELECT COUNT(*) FROM post_likes l2 WHERE l2.post_id=p.id AND l2.user_id=?) AS liked,
            (SELECT option_index FROM post_votes v WHERE v.post_id=p.id AND v.user_id=?) AS my_vote
     FROM posts p JOIN users u ON u.id=p.author_id
     ORDER BY p.id DESC LIMIT 30`,
    [req.user.id, req.user.id]
  );
  const votes = await pool.query(`SELECT post_id, option_index, COUNT(*) AS n FROM post_votes GROUP BY post_id, option_index`);
  const voteMap = {};
  for (const v of votes[0]) {
    if (!voteMap[v.post_id]) voteMap[v.post_id] = [];
    voteMap[v.post_id][v.option_index] = Number(v.n);
  }
  res.json({ ok: true, posts: rows.map(p => ({
    id: p.id,
    author_id: p.author_id,
    author_username: p.username,
    author_nickname: p.nickname || p.username,
    author_avatar: p.avatar || '',
    content: p.content,
    images: p.images ? safeJson(p.images) : [],
    poll_question: p.poll_question || '',
    poll_options: p.poll_options ? safeJson(p.poll_options) : [],
    poll_votes: voteMap[p.id] || [],
    my_vote: p.my_vote === null || p.my_vote === undefined ? -1 : p.my_vote,
    created_at: p.created_at,
    like_count: p.like_count,
    comment_count: p.comment_count,
    liked: p.liked > 0,
  })) });
});

function safeJson(s) {
  try { return JSON.parse(s); } catch { return []; }
}
// 删除动态
r.delete('/posts/:id', auth, async (req, res) => {
  await pool.query(`DELETE FROM posts WHERE id=? AND author_id=?`, [req.params.id, req.user.id]);
  await pool.query(`DELETE FROM post_likes WHERE post_id=?`, [req.params.id]);
  await pool.query(`DELETE FROM post_comments WHERE post_id=?`, [req.params.id]);
  await pool.query(`DELETE FROM post_votes WHERE post_id=?`, [req.params.id]);
  res.json({ ok: true });
});

// 投票
r.post('/posts/:id/vote', auth, async (req, res) => {
  const postId = req.params.id;
  const optionIndex = Number(req.body?.option_index);
  const [p] = await pool.query(`SELECT poll_options FROM posts WHERE id=?`, [postId]);
  if (!p[0] || !p[0].poll_options) return res.status(400).json({ error: '该动态没有投票' });
  const opts = safeJson(p[0].poll_options);
  if (optionIndex < 0 || optionIndex >= opts.length) return res.status(400).json({ error: '选项无效' });
  await pool.query(`DELETE FROM post_votes WHERE post_id=? AND user_id=?`, [postId, req.user.id]);
  await pool.query(`INSERT INTO post_votes (post_id, user_id, option_index) VALUES (?,?,?)`, [postId, req.user.id, optionIndex]);
  res.json({ ok: true });
});

// 点赞 / 取消点赞
r.post('/posts/:id/like', auth, async (req, res) => {
  const postId = req.params.id;
  const [existing] = await pool.query(`SELECT id FROM post_likes WHERE post_id=? AND user_id=?`, [postId, req.user.id]);
  if (existing.length > 0) {
    await pool.query(`DELETE FROM post_likes WHERE post_id=? AND user_id=?`, [postId, req.user.id]);
    res.json({ ok: true, liked: false });
  } else {
    await pool.query(`INSERT INTO post_likes (post_id, user_id) VALUES (?,?)`, [postId, req.user.id]);
    res.json({ ok: true, liked: true });
  }
});

// 评论
r.post('/posts/:id/comment', auth, async (req, res) => {
  const { content } = req.body || {};
  if (!content || !String(content).trim()) return res.status(400).json({ error: '评论不能为空' });
  const c = String(content).trim().slice(0, 1000);
  await pool.query(`INSERT INTO post_comments (post_id, author_id, content) VALUES (?,?,?)`, [req.params.id, req.user.id, c]);
  res.json({ ok: true });
});

// 某条动态的评论列表
r.get('/posts/:id/comments', auth, async (req, res) => {
  const [rows] = await pool.query(
    `SELECT c.id, c.author_id, c.content, c.created_at, u.username, u.nickname, u.avatar
     FROM post_comments c JOIN users u ON u.id=c.author_id
     WHERE c.post_id=? ORDER BY c.id ASC LIMIT 200`,
    [req.params.id]
  );
  res.json({ ok: true, comments: rows.map(c => ({
    id: c.id,
    author_id: c.author_id,
    username: c.username,
    nickname: c.nickname || c.username,
    avatar: c.avatar || '',
    content: c.content,
    created_at: c.created_at,
  })) });
});

export default r;
