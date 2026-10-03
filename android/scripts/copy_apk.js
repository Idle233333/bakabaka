const fs = require('fs');
const src = 'C:/Users/Administrator/touhou-aichat/app/build/outputs/apk/debug/app-debug.apk';
const dst = 'C:/Users/Administrator/Desktop/AI聊天.apk';
fs.copyFileSync(src, dst);
const stat = fs.statSync(dst);
console.log('Copied to: ' + dst);
console.log('Size: ' + stat.size + ' bytes');
