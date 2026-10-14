const { app, BrowserWindow, ipcMain, shell, safeStorage } = require('electron');
const fs = require('fs'), path = require('path'), { execFile } = require('child_process');
const { Client, Authenticator } = require('minecraft-launcher-core');
const { Auth } = require('msmc');
const crypto = require('crypto');

const ROOT = path.join(app.getPath('appData'), '.coreclient');
const DATA = path.join(app.getPath('userData'), 'core.json');
let win, running = false;

const defaults = () => ({
  accounts: [], active: null,
  settings: {
    version: null, loader: 'fabric', ram: 4, fullscreen: false, jvm: '', lang: 'pl',
    discord: 'https://discord.gg/', hide: true, history: [], servers: [], versionText: '',
    mods: { FPS: true, CPS: true, Keystrokes: false, 'Armor Status': false, Zoom: true, 'Toggle Sprint': false, 'Full Vision': true, Koordynaty: true },
    binds: { 'Menu Core': 'RShift', Zoom: 'C', 'Full Vision': 'V', 'Toggle Sprint': 'G', Freelook: 'Alt' }
  }
});
function load() {
  const d = defaults();
  try { const s = JSON.parse(fs.readFileSync(DATA, 'utf8')); const ss = s.settings || {};
    return { ...d, ...s, settings: { ...d.settings, ...ss, mods: { ...d.settings.mods, ...ss.mods }, binds: { ...d.settings.binds, ...ss.binds } } }; }
  catch { return d; }
}
function save(d) { fs.mkdirSync(path.dirname(DATA), { recursive: true }); fs.writeFileSync(DATA, JSON.stringify(d, null, 2)); }
const enc = s => safeStorage.isEncryptionAvailable() ? 'e:' + safeStorage.encryptString(s).toString('base64') : 'p:' + s;
const dec = s => s.startsWith('e:') ? safeStorage.decryptString(Buffer.from(s.slice(2), 'base64')) : s.slice(2);
const pub = d => ({ active: d.active, settings: d.settings, accounts: d.accounts.map(({ id, type, name }) => ({ id, type, name })) });
const send = (ch, d) => win && !win.isDestroyed() && win.webContents.send(ch, d);

function createWindow() {
  win = new BrowserWindow({
    width: 1180, height: 720, minWidth: 980, minHeight: 640, title: 'Core Client',
    backgroundColor: '#0a0b10', autoHideMenuBar: true,
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true }
  });
  win.loadFile('index.html');
}

ipcMain.handle('state', () => pub(load()));
ipcMain.handle('settings', (_e, patch) => { const d = load(); d.settings = { ...d.settings, ...patch }; save(d); return pub(d); });

ipcMain.handle('versions', async () => {
  try {
    const m = await (await fetch('https://launchermeta.mojang.com/mc/game/version_manifest_v2.json')).json();
    const ids = m.versions.filter(v => v.type === 'release').map(v => v.id);
    return ids.slice(0, ids.indexOf('1.8.8') + 1);
  } catch { return ['1.21.11', '1.21.4', '1.20.4', '1.20.1', '1.19.4', '1.18.2', '1.17.1', '1.16.5', '1.12.2', '1.8.9', '1.8.8']; }
});

ipcMain.handle('acc:ms', async () => {
  const xbox = await new Auth('select_account').launch('electron');
  const mc = await xbox.getMinecraft();
  const d = load(), id = 'ms:' + mc.profile.id;
  d.accounts = d.accounts.filter(a => a.id !== id);
  d.accounts.push({ id, type: 'ms', name: mc.profile.name, token: enc(xbox.save()) });
  d.active = id; save(d); return pub(d);
});
ipcMain.handle('acc:offline', (_e, name) => {
  name = String(name || '').trim();
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) throw new Error('Nick: 3-16 znaków, litery, cyfry i _');
  const d = load(), id = 'off:' + name.toLowerCase();
  d.accounts = d.accounts.filter(a => a.id !== id);
  d.accounts.push({ id, type: 'offline', name }); d.active = id; save(d); return pub(d);
});
ipcMain.handle('acc:select', (_e, id) => { const d = load(); if (d.accounts.some(a => a.id === id)) d.active = id; save(d); return pub(d); });
ipcMain.handle('acc:remove', (_e, id) => {
  const d = load(); d.accounts = d.accounts.filter(a => a.id !== id);
  if (d.active === id) d.active = d.accounts[0] ? d.accounts[0].id : null;
  save(d); return pub(d);
});

const needJava = v => { const [, mi = 0, pa = 0] = v.split('.').map(Number); return mi >= 21 || (mi === 20 && pa >= 5) ? 21 : mi >= 17 ? 17 : 8; };
ipcMain.handle('java', (_e, version) => new Promise(res => {
  const need = needJava(version);
  execFile('java', ['-version'], (err, so, se) => {
    if (err) return res({ ok: false, found: null, need });
    const m = /version "(\d+)(?:\.(\d+))?/.exec(se || so || '');
    const found = m ? (m[1] === '1' ? Number(m[2]) : Number(m[1])) : 0;
    res({ ok: found >= need, found, need });
  });
}));


// Automatyczne pobieranie właściwej Javy (8 dla 1.8-1.16, 17 dla 1.17-1.20.4, 21 dla nowszych)
async function ensureJava(major) {
  const base = path.join(ROOT, 'runtime', `java-${major}`);
  const exe = process.platform === 'win32' ? 'java.exe' : 'java';
  const find = d => {
    for (const e of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, e.name);
      if (e.isDirectory()) { const r = find(p); if (r) return r; }
      else if (e.name === exe && path.basename(d) === 'bin') return p;
    }
    return null;
  };
  if (fs.existsSync(base)) { const f = find(base); if (f) return f; }
  const os = { win32: 'windows', darwin: 'mac', linux: 'linux' }[process.platform];
  const arch = process.arch === 'arm64' ? 'aarch64' : 'x64';
  send('status', `Pobieranie Javy ${major}...`);
  const res = await fetch(`https://api.adoptium.net/v3/binary/latest/${major}/ga/${os}/${arch}/jre/hotspot/normal/eclipse`, { redirect: 'follow' });
  if (!res.ok) throw new Error(`Nie udało się pobrać Javy ${major} (kod ${res.status})`);
  const buf = Buffer.from(await res.arrayBuffer());
  fs.mkdirSync(base, { recursive: true });
  const tmp = path.join(ROOT, 'runtime', `java-${major}.${process.platform === 'win32' ? 'zip' : 'tar.gz'}`);
  fs.writeFileSync(tmp, buf);
  send('status', `Rozpakowywanie Javy ${major}...`);
  await new Promise((ok, no) => execFile('tar', ['-xf', tmp, '-C', base], e => e ? no(new Error('Rozpakowanie Javy nie powiodło się')) : ok()));
  try { fs.unlinkSync(tmp); } catch {}
  const f = find(base);
  if (!f) throw new Error('Nie znaleziono Javy po rozpakowaniu');
  if (process.platform !== 'win32') fs.chmodSync(f, 0o755);
  return f;
}

const legacy = v => Number(v.split('.')[1]) < 14;
const META = v => legacy(v) ? 'https://meta.legacyfabric.net/v2/versions/loader' : 'https://meta.fabricmc.net/v2/versions/loader';

async function ensureFabric(mc) {
  const lr = await (await fetch(`${META(mc)}/${mc}`)).json();
  if (!Array.isArray(lr) || !lr.length) throw new Error(legacy(mc) ? `Legacy Fabric nie ma wersji ${mc}. Spróbuj 1.8.9 lub 1.12.2.` : `Fabric nie wspiera wersji ${mc}`);
  const lv = (lr.find(x => x.loader.stable) || lr[0]).loader.version;
  const id = `fabric-loader-${lv}-${mc}`, dir = path.join(ROOT, 'versions', id), file = path.join(dir, id + '.json');
  if (!fs.existsSync(file)) {
    const p = await (await fetch(`${META(mc)}/${mc}/${lv}/profile/json`)).text();
    fs.mkdirSync(dir, { recursive: true }); fs.writeFileSync(file, p);
  }
  return id;
}

async function ensureFabricApi(mc, gd) {
  const dir = path.join(gd, 'mods');
  if (fs.readdirSync(dir).some(f => /fabric-api/i.test(f))) return;
  const project = legacy(mc) ? 'legacy-fabric-api' : 'fabric-api';
  const q = `game_versions=${encodeURIComponent(JSON.stringify([mc]))}&loaders=${encodeURIComponent('["fabric"]')}`;
  const vs = await (await fetch(`https://api.modrinth.com/v2/project/${project}/version?${q}`, { headers: { 'User-Agent': 'CoreClient/0.3' } })).json();
  if (!Array.isArray(vs) || !vs.length) throw new Error('Brak ' + project + ' dla ' + mc);
  const f = vs[0].files.find(x => x.primary) || vs[0].files[0];
  fs.writeFileSync(path.join(dir, f.filename), Buffer.from(await (await fetch(f.url)).arrayBuffer()));
}
function installBundledMods(mc, gd) {
  const src = path.join(app.getAppPath(), 'mods');
  if (!fs.existsSync(src)) return;
  for (const f of fs.readdirSync(src)) if (f.endsWith('.jar') && f.includes('+' + mc)) fs.copyFileSync(path.join(src, f), path.join(gd, 'mods', f));
}

ipcMain.handle('launch', async (_e, serverAddr) => {
  if (running) throw new Error('Gra już działa');
  const d = load(), s = d.settings, acc = d.accounts.find(a => a.id === d.active);
  if (!acc) throw new Error('Wybierz konto');
  if (!s.version) throw new Error('Wybierz wersję');
  running = true;
  const GAME = path.join(ROOT, 'instances', `${s.version}-${s.loader}`);
  try {
    send('status', 'Logowanie...');
    let auth;
    if (acc.type === 'ms') {
      const xbox = await new Auth('select_account').refresh(dec(acc.token));
      const mc = await xbox.getMinecraft(); auth = mc.mclc();
      try { acc.token = enc(xbox.save()); save(d); } catch {}
    } else auth = Authenticator.getAuth(acc.name);

    let custom, useFabric = s.loader === 'fabric';
    if (useFabric) {
      try { send('status', 'Instalacja Fabric...'); custom = await ensureFabric(s.version); }
      catch (e) { useFabric = false; custom = undefined; send('status', 'Fabric niedostępny, start bez modów'); send('log', 'Fabric: ' + e.message); }
    }

    fs.mkdirSync(path.join(GAME, 'mods'), { recursive: true });
    fs.mkdirSync(path.join(GAME, 'config'), { recursive: true });
    fs.writeFileSync(path.join(GAME, 'config', 'coreclient.json'), JSON.stringify({ mods: s.mods, binds: s.binds, user: acc.name, versionText: s.versionText || ('Core Client ' + s.version) }, null, 2));

    if (useFabric) { try { send('status', 'Fabric API...'); await ensureFabricApi(s.version, GAME); installBundledMods(s.version, GAME); } catch (e) { send('log', 'Fabric API: ' + e.message); } }
    const javaPath = await ensureJava(needJava(s.version));

    const launcher = new Client();
    launcher.on('progress', p => { send('status', 'Pobieranie: ' + p.type); send('progress', p); });
    launcher.on('debug', x => send('log', x));
    launcher.on('data', x => send('log', String(x)));
    launcher.on('arguments', () => { send('status', 'Gra startuje...'); if (s.hide && win) win.hide(); });
    launcher.on('close', c => { running = false; if (win && !win.isDestroyed()) win.show(); send('closed', c); });

    const opts = {
      authorization: auth, root: ROOT, javaPath, overrides: { gameDirectory: GAME },
      version: { number: s.version, type: 'release', ...(custom ? { custom } : {}) },
      memory: { max: `${s.ram}G`, min: '1G' },
      customArgs: s.jvm ? s.jvm.split(/\s+/).filter(Boolean) : []
    };
    if (s.fullscreen) opts.window = { fullscreen: true };
    if (serverAddr) { const [host, port] = String(serverAddr).split(':'); opts.server = { host, port: port || '25565' }; }
    const proc = await launcher.launch(opts);
    if (!proc) throw new Error('Nie udało się uruchomić gry. Sprawdź logi (ikona notatnika).');
    const d2 = load();
    d2.settings.history = [{ version: s.version, loader: useFabric ? 'fabric' : 'vanilla', ts: Date.now() }, ...(d2.settings.history || []).filter(h => !(h.version === s.version && h.loader === (useFabric ? 'fabric' : 'vanilla')))].slice(0, 5);
    save(d2);
  } catch (e) { running = false; throw e; }
});


// ---------- Modrinth ----------
const MR = 'https://api.modrinth.com/v2', UA = { headers: { 'User-Agent': 'CoreClient/0.4' } };
const jget = async u => { const r = await fetch(u, UA); if (!r.ok) throw new Error('Modrinth: błąd ' + r.status); return r.json(); };
const gameDir = s => path.join(ROOT, 'instances', `${s.version}-${s.loader}`);
const modsDir = s => path.join(gameDir(s), 'mods');

ipcMain.handle('mr:search', async (_e, query) => {
  const s = load().settings;
  if (s.loader !== 'fabric') throw new Error('Wybierz silnik Fabric (w Wersjach), żeby dodawać mody.');
  const facets = JSON.stringify([['project_type:mod'], ['categories:fabric'], [`versions:${s.version}`]]);
  const d = await jget(`${MR}/search?query=${encodeURIComponent(query || '')}&facets=${encodeURIComponent(facets)}&limit=20&index=downloads`);
  return d.hits.map(h => ({ id: h.project_id, title: h.title, desc: h.description, icon: h.icon_url, downloads: h.downloads, author: h.author }));
});

async function installProject(id, s, seen = new Set()) {
  if (seen.has(id)) return []; seen.add(id);
  const q = `game_versions=${encodeURIComponent(JSON.stringify([s.version]))}&loaders=${encodeURIComponent('["fabric"]')}`;
  const vs = await jget(`${MR}/project/${id}/version?${q}`);
  if (!vs.length) throw new Error('Ten mod nie ma wersji dla ' + s.version);
  const v = vs[0], f = v.files.find(x => x.primary) || v.files[0];
  const dir = modsDir(s); fs.mkdirSync(dir, { recursive: true });
  const isApi = /fabric-api/i.test(f.filename), have = fs.readdirSync(dir);
  const out = [];
  if (!(isApi && have.some(x => /fabric-api/i.test(x))) && !have.includes(f.filename)) {
    const buf = Buffer.from(await (await fetch(f.url, UA)).arrayBuffer());
    if (f.hashes && f.hashes.sha512 && crypto.createHash('sha512').update(buf).digest('hex') !== f.hashes.sha512) throw new Error('Plik uszkodzony (zła suma SHA-512)');
    fs.writeFileSync(path.join(dir, f.filename), buf);
    out.push(f.filename);
  }
  for (const dep of v.dependencies || []) {
    if (dep.dependency_type === 'required' && dep.project_id) { try { out.push(...await installProject(dep.project_id, s, seen)); } catch {} }
  }
  return out;
}
ipcMain.handle('mr:install', (_e, id) => installProject(String(id), load().settings));
ipcMain.handle('mods:list', () => {
  const dir = modsDir(load().settings);
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir).filter(f => f.endsWith('.jar')).map(f => ({ name: f, size: fs.statSync(path.join(dir, f)).size }));
});
ipcMain.handle('mods:remove', (_e, name) => {
  const f = path.join(modsDir(load().settings), path.basename(String(name)));
  if (fs.existsSync(f)) fs.unlinkSync(f);
});

ipcMain.handle('open', (_e, what) => {
  if (what === 'mods') { const s = load().settings, g = path.join(ROOT, 'instances', `${s.version}-${s.loader}`, 'mods'); fs.mkdirSync(g, { recursive: true }); return shell.openPath(g); }
  fs.mkdirSync(ROOT, { recursive: true }); return shell.openPath(ROOT);
});
ipcMain.handle('url', (_e, u) => { if (/^https:\/\//.test(u)) shell.openExternal(u); });

app.whenReady().then(createWindow);
app.on('window-all-closed', () => app.quit());
