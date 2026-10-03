import 'dotenv/config';
import { chatWithAI } from './src/ai.js';
try {
  const reply = await chatWithAI([{ role: 'user', content: '你好，你是谁？' }], '博丽灵梦');
  console.log('REPLY:', reply);
} catch (e) {
  console.error('FAIL', e.message);
  process.exit(1);
}
