// CDP verification: trigger real ElNotification (login success / logout) and capture glass styles
const http = require('http');
const fs = require('fs');

const PORT = Number(process.env.CDP_PORT || 9222);
const APP_URL = process.env.APP_URL || 'http://localhost:8082/';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function getTargets() {
  return new Promise((resolve, reject) => {
    http.get(`http://127.0.0.1:${PORT}/json/list`, (res) => {
      let buf = '';
      res.on('data', (c) => (buf += c));
      res.on('end', () => resolve(JSON.parse(buf)));
    }).on('error', reject);
  });
}

class Cdp {
  constructor(wsUrl) {
    this.ws = new WebSocket(wsUrl);
    this.id = 0;
    this.pending = new Map();
    this.ready = new Promise((resolve, reject) => {
      this.ws.addEventListener('open', resolve);
      this.ws.addEventListener('error', reject);
    });
    this.ws.addEventListener('message', (ev) => {
      const msg = JSON.parse(ev.data.toString());
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result);
      }
    });
  }
  send(method, params = {}) {
    const id = ++this.id;
    this.ws.send(JSON.stringify({ id, method, params }));
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id);
          reject(new Error(`CDP timeout: ${method}`));
        }
      }, 20000);
    });
  }
  async eval(expression) {
    const r = await this.send('Runtime.evaluate', {
      expression,
      returnByValue: true,
      awaitPromise: true
    });
    if (r.exceptionDetails) throw new Error(JSON.stringify(r.exceptionDetails));
    return r.result.value;
  }
}

const COLLECT = `(() => {
  const n = document.querySelector('.el-notification');
  if (!n) return { found: false };
  const cs = getComputedStyle(n);
  const title = n.querySelector('.el-notification__title');
  const content = n.querySelector('.el-notification__content');
  const close = n.querySelector('.el-notification__closeBtn');
  const icon = n.querySelector('.el-notification__icon');
  const rect = n.getBoundingClientRect();
  return {
    found: true,
    bodyClass: document.body.className,
    classes: n.className,
    backdropFilter: cs.backdropFilter,
    webkitBackdropFilter: cs.webkitBackdropFilter,
    background: cs.backgroundColor,
    border: cs.border,
    boxShadow: cs.boxShadow.slice(0, 80),
    borderRadius: cs.borderRadius,
    titleColor: title ? getComputedStyle(title).color : null,
    contentColor: content ? getComputedStyle(content).color : null,
    closeColor: close ? getComputedStyle(close).color : null,
    iconColor: icon ? getComputedStyle(icon).color : null,
    titleText: title ? title.textContent : null,
    contentText: content ? content.textContent.trim().slice(0, 40) : null,
    rect: { top: Math.round(rect.top), right: Math.round(window.innerWidth - rect.right), w: Math.round(rect.width), h: Math.round(rect.height) }
  };
})()`;

async function waitFor(cdp, expr, tries, gap) {
  for (let i = 0; i < tries; i++) {
    const v = await cdp.eval(expr).catch(() => false);
    if (v) return true;
    await sleep(gap);
  }
  return false;
}

async function main() {
  let page = null;
  for (let i = 0; i < 30; i++) {
    try {
      const targets = await getTargets();
      page = targets.find((t) => t.type === 'page');
      if (page) break;
    } catch (e) {
      console.log('CDP not ready: ' + e.message + ', retry ' + (i + 1) + '/30');
    }
    await sleep(1000);
  }
  if (!page) throw new Error('no page target after 30 retries');

  const cdp = new Cdp(page.webSocketDebuggerUrl);
  await cdp.ready;
  await cdp.send('Page.enable');
  await cdp.send('Network.enable');
  await cdp.send('Network.setBlockedURLs', {
    urls: ['https://*', 'http://connect.qq.com/*', 'http://cdn.staticfile.org/*', 'wss://*', 'ws://*']
  });
  await cdp.send('Emulation.setDeviceMetricsOverride', {
    width: 1440, height: 900, deviceScaleFactor: 1, mobile: false
  });
  await cdp.send('Page.navigate', { url: APP_URL });

  if (!(await waitFor(cdp, `!!document.body && document.readyState !== 'loading'`, 60, 500)))
    throw new Error('body never ready, href=' + await cdp.eval('location.href').catch(() => '?'));
  await waitFor(cdp, `!!document.querySelector('.header-controls')`, 60, 500);
  await sleep(1500);

  const out = { dir: __dirname, url: APP_URL };
  out.title = await cdp.eval('document.title');
  out.theme = await cdp.eval('document.body.className');

  // ---------- LIGHT: real login -> success notification ----------
  out.loginEntry = await cdp.eval(`(() => {
    const span = [...document.querySelectorAll('.header-controls span')]
      .find(s => { const t = s.textContent.trim().toLowerCase(); return t === 'login' || t === '\u767b\u5f55'; });
    if (!span) return 'NOTFOUND';
    span.click();
    return 'CLICKED';
  })()`);
  out.loginBox = await waitFor(cdp, `!!document.querySelector('.login-box')`, 20, 300);
  if (out.loginBox) {
    out.submit = await cdp.eval(`(() => {
      const box = document.querySelector('.login-box');
      const set = (el, v) => { el.value = v; el.dispatchEvent(new Event('input', { bubbles: true })); };
      const u = box.querySelector('input[type="text"]');
      const p = box.querySelector('input[type="password"]');
      if (u) set(u, 'admin@163.com');
      if (p) set(p, '123456');
      const btn = box.querySelectorAll('button')[0];
      if (btn) btn.click();
      return !!btn;
    })()`);
  }
  out.notifLight = await waitFor(cdp, `!!document.querySelector('.el-notification')`, 30, 300);
  if (out.notifLight) {
    await sleep(400);
    out.light = await cdp.eval(COLLECT);
    let shot = await cdp.send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(__dirname + '/notification-light.png', Buffer.from(shot.data, 'base64'));
    await cdp.eval(`(() => { const c = document.querySelector('.el-notification .el-notification__closeBtn'); if (c) c.click(); return !!c; })()`);
    await sleep(500);
  }

  // ---------- switch to dark via real .toggler ----------
  out.toggler = await cdp.eval(`(() => { const t = document.querySelector('.toggler'); if (t) t.click(); return !!t; })()`);
  await sleep(900);
  if (!(await cdp.eval(`document.body.classList.contains('theme-dark')`))) {
    await cdp.eval(`document.body.classList.remove('theme-light'); document.body.classList.add('theme-dark'); true`);
    out.darkFallback = true;
  }
  out.themeDark = await cdp.eval('document.body.className');

  // ---------- DARK: real logout -> success notification ----------
  out.dropdown = await cdp.eval(`(() => {
    const dd = [...document.querySelectorAll('.header-controls .ob-dropdown')].find(d => d.querySelector('img'));
    if (dd) dd.click();
    return !!dd;
  })()`);
  await sleep(800);
  out.logoutItem = await cdp.eval(`(() => {
    const items = [...document.querySelectorAll('.ob-glass-panel *')]
      .filter(el => { const t = el.textContent.trim().toLowerCase(); return t === 'logout' || t === '\u9000\u51fa' || t === '\u767b\u51fa'; });
    items.sort((a, b) => a.children.length - b.children.length);
    const item = items[0];
    if (item) item.click();
    return !!item;
  })()`);
  out.notifDark = await waitFor(cdp, `!!document.querySelector('.el-notification')`, 30, 300);
  if (out.notifDark) {
    await sleep(400);
    out.dark = await cdp.eval(COLLECT);
    let shot = await cdp.send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(__dirname + '/notification-dark.png', Buffer.from(shot.data, 'base64'));
  }

  console.log(JSON.stringify(out, null, 2));
  process.exit(0);
}

main().catch((e) => {
  console.error('VERIFY FAILED:', e.message);
  process.exit(1);
});
