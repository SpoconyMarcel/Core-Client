const { contextBridge, ipcRenderer: r } = require('electron');
contextBridge.exposeInMainWorld('core', {
  state: () => r.invoke('state'),
  settings: p => r.invoke('settings', p),
  versions: () => r.invoke('versions'),
  java: v => r.invoke('java', v),
  loginMs: () => r.invoke('acc:ms'),
  loginOffline: n => r.invoke('acc:offline', n),
  select: id => r.invoke('acc:select', id),
  remove: id => r.invoke('acc:remove', id),
  launch: s => r.invoke('launch', s),
  mrSearch: q => r.invoke('mr:search', q),
  mrInstall: id => r.invoke('mr:install', id),
  modsList: () => r.invoke('mods:list'),
  modsRemove: n => r.invoke('mods:remove', n),
  open: w => r.invoke('open', w),
  url: u => r.invoke('url', u),
  on: (ch, fn) => r.on(ch, (_e, d) => fn(d))
});
