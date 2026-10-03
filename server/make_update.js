import fs from 'node:fs';
import crypto from 'node:crypto';
const src = 'C:/Users/Administrator/Desktop/AI聊天.apk';
const dir = 'C:/Users/Administrator/Desktop/ai-chat/server/updates';
fs.mkdirSync(dir, { recursive: true });
const data = fs.readFileSync(src);
const file = 'app-v1.apk';
fs.writeFileSync(dir + '/' + file, data);
const sha256 = crypto.createHash('sha256').update(data).digest('hex');
const manifest = {
  version_code: 1,
  version_name: '1.0.0',
  file,
  size: data.length,
  sha256,
  note: '首版：底部导航+搜索+Windows10风+角色头像选择'
};
fs.writeFileSync(dir + '/manifest.json', JSON.stringify(manifest, null, 2), 'utf8');
console.log('size=' + data.length);
console.log('sha256=' + sha256);
