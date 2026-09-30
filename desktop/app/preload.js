const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('desktopAPI', {
  isDesktop: true,
  platform: 'win32',
  version: '8.0.0',
  print: () => window.print()
});
