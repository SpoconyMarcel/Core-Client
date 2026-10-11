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
  launch: () => r.invoke('launch'),
  open: w => r.invoke('open', w),
  url: u => r.invoke('url', u),
  on: (ch, fn) => r.on(ch, (_e, d) => fn(d))
});
