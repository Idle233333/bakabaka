export function clientIp(req) {
  const fwd = req.headers['x-forwarded-for'];
  if (fwd) return String(fwd).split(',')[0].trim();
  return (req.socket && req.socket.remoteAddress) || '';
}

export async function lookupIpLocation(ip) {
  if (!ip || ip === '127.0.0.1' || ip === '::1' || ip === '::ffff:127.0.0.1' ||
      ip.startsWith('192.168.') || ip.startsWith('10.') || ip.startsWith('172.')) {
    return '局域网';
  }
  try {
    const url = `http://ip-api.com/json/${ip}?lang=zh-CN&fields=status,country,regionName,city`;
    const resp = await fetch(url, { signal: AbortSignal.timeout(3000) });
    const data = await resp.json();
    if (data && data.status === 'success') {
      return data.regionName || data.country || '未知';
    }
    return '未知';
  } catch {
    return '未知';
  }
}
