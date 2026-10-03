import mysql from 'mysql2/promise';

const dbName = process.env.DB_NAME || 'ai_chat';

export const pool = mysql.createPool({
  host: process.env.DB_HOST || '127.0.0.1',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASS || '',
  database: dbName,
  waitForConnections: true,
  connectionLimit: 10,
  charset: 'utf8mb4',
});

export async function initDB() {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST || '127.0.0.1',
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASS || '',
  });
  await conn.query(`CREATE DATABASE IF NOT EXISTS \`${dbName}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
  await conn.end();

  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      id INT AUTO_INCREMENT PRIMARY KEY,
      username VARCHAR(64) NOT NULL UNIQUE,
      email VARCHAR(255) NOT NULL UNIQUE,
      password_hash VARCHAR(255) NOT NULL,
      nickname VARCHAR(64) NOT NULL DEFAULT '',
      avatar MEDIUMTEXT,
      signature VARCHAR(255) NOT NULL DEFAULT '',
      gender VARCHAR(16) NOT NULL DEFAULT '',
      company VARCHAR(128) NOT NULL DEFAULT '',
      address VARCHAR(255) NOT NULL DEFAULT '',
      ip_location VARCHAR(64) NOT NULL DEFAULT '',
      is_admin TINYINT(1) NOT NULL DEFAULT 0,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS messages (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      user_id INT NOT NULL,
      persona VARCHAR(64) NOT NULL DEFAULT '',
      role VARCHAR(16) NOT NULL,
      content_type VARCHAR(16) NOT NULL DEFAULT 'text',
      content MEDIUMTEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_user_persona (user_id, persona, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS agents (
      id INT AUTO_INCREMENT PRIMARY KEY,
      owner_id INT NOT NULL,
      name VARCHAR(64) NOT NULL UNIQUE,
      persona TEXT NOT NULL,
      avatar MEDIUMTEXT,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_owner (owner_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS dm_messages (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      sender_id INT NOT NULL,
      receiver_id INT NOT NULL,
      content_type VARCHAR(16) NOT NULL DEFAULT 'text',
      content MEDIUMTEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_pair (sender_id, receiver_id, id),
      INDEX idx_receiver (receiver_id, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS posts (
      id INT AUTO_INCREMENT PRIMARY KEY,
      author_id INT NOT NULL,
      content TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_author (author_id, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS post_likes (
      id INT AUTO_INCREMENT PRIMARY KEY,
      post_id INT NOT NULL,
      user_id INT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      UNIQUE KEY uniq_post_user (post_id, user_id),
      INDEX idx_post (post_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS post_comments (
      id INT AUTO_INCREMENT PRIMARY KEY,
      post_id INT NOT NULL,
      author_id INT NOT NULL,
      content TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_post (post_id, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS post_votes (
      id INT AUTO_INCREMENT PRIMARY KEY,
      post_id INT NOT NULL,
      user_id INT NOT NULL,
      option_index INT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      UNIQUE KEY uniq_post_user (post_id, user_id),
      INDEX idx_post (post_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);

  // 迁移：posts 表加 images / poll 列
  const [pcols] = await pool.query(`SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='posts'`);
  const pexisting = pcols.map(c => c.COLUMN_NAME);
  if (!pexisting.includes('images')) await pool.query(`ALTER TABLE posts ADD COLUMN images TEXT`);
  if (!pexisting.includes('poll_question')) await pool.query(`ALTER TABLE posts ADD COLUMN poll_question VARCHAR(255) NOT NULL DEFAULT ''`);
  if (!pexisting.includes('poll_options')) await pool.query(`ALTER TABLE posts ADD COLUMN poll_options TEXT`);

  await pool.query(`
    CREATE TABLE IF NOT EXISTS calls (
      id INT AUTO_INCREMENT PRIMARY KEY,
      caller_id INT NOT NULL,
      callee_id INT NOT NULL,
      status VARCHAR(16) NOT NULL DEFAULT 'ringing',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      INDEX idx_callee (callee_id, status, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS call_signals (
      id INT AUTO_INCREMENT PRIMARY KEY,
      call_id INT NOT NULL,
      from_id INT NOT NULL,
      to_id INT NOT NULL,
      type VARCHAR(16) NOT NULL,
      data TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_call (call_id, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS remarks (
      id INT AUTO_INCREMENT PRIMARY KEY,
      owner_id INT NOT NULL,
      target_id INT NOT NULL,
      remark VARCHAR(64) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      UNIQUE KEY uniq_owner_target (owner_id, target_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);

  // 迁移：旧表缺列则补上
  const [ucols] = await pool.query(`SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='users'`);
  const uexisting = ucols.map(c => c.COLUMN_NAME);
  if (!uexisting.includes('is_admin')) await pool.query(`ALTER TABLE users ADD COLUMN is_admin TINYINT(1) NOT NULL DEFAULT 0`);
  if (!uexisting.includes('nickname')) await pool.query(`ALTER TABLE users ADD COLUMN nickname VARCHAR(64) NOT NULL DEFAULT ''`);
  if (!uexisting.includes('avatar')) await pool.query(`ALTER TABLE users ADD COLUMN avatar MEDIUMTEXT`);
  if (!uexisting.includes('signature')) await pool.query(`ALTER TABLE users ADD COLUMN signature VARCHAR(255) NOT NULL DEFAULT ''`);
  if (!uexisting.includes('gender')) await pool.query(`ALTER TABLE users ADD COLUMN gender VARCHAR(16) NOT NULL DEFAULT ''`);
  if (!uexisting.includes('company')) await pool.query(`ALTER TABLE users ADD COLUMN company VARCHAR(128) NOT NULL DEFAULT ''`);
  if (!uexisting.includes('address')) await pool.query(`ALTER TABLE users ADD COLUMN address VARCHAR(255) NOT NULL DEFAULT ''`);
  if (!uexisting.includes('ip_location')) await pool.query(`ALTER TABLE users ADD COLUMN ip_location VARCHAR(64) NOT NULL DEFAULT ''`);
  if (uexisting.includes('persona')) await pool.query(`ALTER TABLE users DROP COLUMN persona`);

  const [mcols] = await pool.query(`SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='messages'`);
  const mexisting = mcols.map(c => c.COLUMN_NAME);
  if (!mexisting.includes('persona')) await pool.query(`ALTER TABLE messages ADD COLUMN persona VARCHAR(64) NOT NULL DEFAULT ''`);
  if (!mexisting.includes('content_type')) await pool.query(`ALTER TABLE messages ADD COLUMN content_type VARCHAR(16) NOT NULL DEFAULT 'text'`);
  await pool.query(`ALTER TABLE messages MODIFY COLUMN content MEDIUMTEXT NOT NULL`);
}
