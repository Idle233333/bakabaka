import fs from 'node:fs';
import crypto from 'node:crypto';

const src = 'C:/Users/Administrator/touhou-aichat/app/build/outputs/apk/debug/app-debug.apk';
const desktop = 'C:/Users/Administrator/Desktop/AI聊天.apk';
const updatesDir = 'C:/Users/Administrator/Desktop/ai-chat/server/updates';
const file = 'app-v19.apk';

const data = fs.readFileSync(src);
fs.mkdirSync(updatesDir, { recursive: true });
fs.writeFileSync(desktop, data);
fs.writeFileSync(updatesDir + '/' + file, data);

const sha256 = crypto.createHash('sha256').update(data).digest('hex');
const manifest = {
  version_code: 19,
  version_name: '1.18.0',
  file,
  size: data.length,
  sha256,
  note: '1.18.0：修图OOM+动态图片投票+极简图标+移除假语音通话'
};
fs.writeFileSync(updatesDir + '/manifest.json', JSON.stringify(manifest, null, 2), 'utf8');

console.log('APK size=' + data.length);
console.log('sha256=' + sha256);
console.log('copied to desktop: ' + desktop);
console.log('copied to updates: ' + updatesDir + '/' + file);
console.log('manifest.json written');
