import 'dotenv/config';
import { sendCode } from './src/email.js';
try {
  const code = await sendCode('1113356843@qq.com');
  console.log('SENT_OK code=' + code);
} catch (e) {
  console.error('SEND_FAIL ' + e.message);
  process.exit(1);
}
