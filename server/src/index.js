import 'dotenv/config';
import express from 'express';
import { initDB } from './db.js';
import authRouter from './auth.js';
import chatRouter from './chat.js';
import updateRouter from './update.js';
import announcementRouter from './announcements.js';
import agentsRouter from './agents.js';
import dmRouter from './dm.js';
import postsRouter from './posts.js';
import callRouter from './call.js';
import { runProactive } from './proactive.js';

const app = express();
app.use(express.json({ limit: '5mb' }));

app.get('/health', (req, res) => res.json({ ok: true, time: new Date().toISOString() }));
app.use('/api/auth', authRouter);
app.use('/api', chatRouter);
app.use('/api', updateRouter);
app.use('/api', announcementRouter);
app.use('/api', agentsRouter);
app.use('/api', dmRouter);
app.use('/api', postsRouter);
app.use('/api', callRouter);

const port = Number(process.env.PORT || 3000);

await initDB();
app.listen(port, () => console.log(`[server] listening on :${port}`));

// 角色主动发消息的定时器
const interval = Number(process.env.PROACTIVE_INTERVAL_MS || 600000);
setInterval(runProactive, interval);
console.log(`[proactive] scheduler on, every ${interval}ms`);
