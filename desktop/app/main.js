const { app, BrowserWindow } = require('electron');
const path = require('path');

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 700,
    title: "Choudhury POS — Desktop Edition",
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  const serverUrl = process.env.POS_SERVER_URL || 'http://localhost:8080';
  win.loadURL(serverUrl).catch(() => {
    win.loadFile(path.join(__dirname, 'offline.html'));
  });
}

app.whenReady().then(createWindow);
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
