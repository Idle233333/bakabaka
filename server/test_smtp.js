import 'dotenv/config';
import nodemailer from 'nodemailer';

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT || 465),
  secure: process.env.SMTP_SECURE !== 'false',
  auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
});

try {
  const ok = await transporter.verify();
  console.log('SMTP_VERIFY_OK', ok);
} catch (e) {
  console.error('SMTP_VERIFY_FAIL', e.message);
  process.exit(1);
}
