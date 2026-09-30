const { app, BrowserWindow, Menu, ipcMain, shell, dialog } = require('electron');
const path = require('path');
const fs = require('fs');

let mainWindow;

const CLOUD_URL = 'https://ais-pre-repstbphrkqk34xvfwxoji-454250663559.asia-east1.run.app';
const LOCAL_FALLBACK_FILE = path.join(__dirname, 'public', 'index.html');

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1366,
    height: 850,
    minWidth: 1024,
    minHeight: 700,
    title: 'CHOUDHURY POS — Enterprise Business Management System',
    icon: path.join(__dirname, 'icon.png'),
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
      sandbox: false
    },
    autoHideMenuBar: false,
    backgroundColor: '#0f172a'
  });

  // Create Application Menu
  const template = [
    {
      label: 'Shop',
      submenu: [
        {
          label: 'Reload Terminal',
          accelerator: 'CmdOrCtrl+R',
          click: () => mainWindow.reload()
        },
        {
          label: 'Open Central Cloud Portal',
          click: () => mainWindow.loadURL(CLOUD_URL)
        },
        {
          label: 'Switch to Local Terminal Mode',
          click: () => mainWindow.loadFile(LOCAL_FALLBACK_FILE)
        },
        { type: 'separator' },
        {
          label: 'Exit CHOUDHURY POS',
          accelerator: 'Alt+F4',
          click: () => app.quit()
        }
      ]
    },
    {
      label: 'Print & Receipts',
      submenu: [
        {
          label: 'Print Active Invoice (A4 / 80mm)',
          accelerator: 'CmdOrCtrl+P',
          click: () => {
            mainWindow.webContents.print({ silent: false, printBackground: true });
          }
        },
        {
          label: 'Silent Thermal Print (Default 80mm)',
          accelerator: 'CmdOrCtrl+Shift+P',
          click: () => {
            mainWindow.webContents.print({ silent: true, printBackground: true });
          }
        }
      ]
    },
    {
      label: 'View',
      submenu: [
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' }
      ]
    },
    {
      label: 'Help',
      submenu: [
        {
          label: 'About CHOUDHURY POS',
          click: () => {
            dialog.showMessageBox(mainWindow, {
              type: 'info',
              title: 'CHOUDHURY POS — Enterprise Windows Edition',
              message: 'CHOUDHURY POS v8.0.0 (Windows Desktop Edition)\n\nComplete Multi-Shop Business Management System\nAuthorized License & Universal Cloud Synchronization\n\n© 2027 CHOUDHURY POS. All Rights Reserved.'
            });
          }
        },
        {
          label: 'Download Android APK',
          click: () => shell.openExternal(CLOUD_URL + '/downloads/choudhury-pos-app.apk')
        }
      ]
    }
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);

  // Try loading local file first for instant zero-latency desktop startup
  mainWindow.loadFile(LOCAL_FALLBACK_FILE).catch(() => {
    mainWindow.loadURL(CLOUD_URL).catch(err => {
      console.error('Failed to load portal URL:', err);
    });
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
