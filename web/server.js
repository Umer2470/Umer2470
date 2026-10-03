const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { DatabaseSync } = require('node:sqlite');

const PORT = process.env.APP_PORT || 3000;
const DB_PATH = process.env.DB_PATH || path.join(__dirname, 'pos_central.db');
const JWT_SECRET = process.env.JWT_SECRET || 'choudhury-pos-production-secret-2026';
const PUBLIC_DIR = path.join(__dirname, 'public');
const DOWNLOADS_DIR = path.join(PUBLIC_DIR, 'downloads');

// Ensure directories
[path.dirname(DB_PATH), PUBLIC_DIR, DOWNLOADS_DIR].forEach(dir => {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
});

// Database
const db = new DatabaseSync(DB_PATH);
console.log('Connected to SQLite via node:sqlite at', DB_PATH);

const bcrypt = require('bcryptjs');
const BCRYPT_ROUNDS = 10;

function hashPassword(password) {
  return bcrypt.hashSync(password, BCRYPT_ROUNDS);
}

function verifyPassword(password, storedHash) {
  if (!password || !storedHash) return false;
  if (storedHash.startsWith('$2')) {
    return bcrypt.compareSync(password, storedHash);
  }
  // Controlled migration fallback for legacy SHA-256 salted hashes
  const legacyHash = crypto.createHash('sha256').update(password + '_choudhury_salt_2026').digest('hex');
  return legacyHash === storedHash;
}

function hashRecoveryPin(pin) {
  return bcrypt.hashSync(pin, BCRYPT_ROUNDS);
}

function verifyRecoveryPin(pin, storedPinHash) {
  if (!pin || !storedPinHash) return false;
  if (storedPinHash.startsWith('$2')) {
    return bcrypt.compareSync(pin, storedPinHash);
  }
  const legacyPinHash = crypto.createHash('sha256').update(pin + '_choudhury_recovery_2026').digest('hex');
  return legacyPinHash === storedPinHash;
}

// Native JWT Implementation (RFC 7519 compliant)
function base64Url(str) {
  return Buffer.from(str).toString('base64url');
}

function signToken(payload, secret, expiresInSeconds = 7200) {
  const header = base64Url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const now = Math.floor(Date.now() / 1000);
  const body = base64Url(JSON.stringify({ ...payload, iat: now, exp: now + expiresInSeconds }));
  const sig = crypto.createHmac('sha256', secret).update(`${header}.${body}`).digest('base64url');
  return `${header}.${body}.${sig}`;
}

function verifyToken(token, secret) {
  try {
    if (!token) return null;
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const [header, body, sig] = parts;
    const expectedSig = crypto.createHmac('sha256', secret).update(`${header}.${body}`).digest('base64url');
    if (sig !== expectedSig) return null;
    const payload = JSON.parse(Buffer.from(body, 'base64url').toString('utf8'));
    if (payload.exp && Math.floor(Date.now() / 1000) > payload.exp) return null;
    return payload;
  } catch (e) {
    return null;
  }
}

function initDb() {
  db.exec('PRAGMA foreign_keys = ON;');
  db.exec('PRAGMA journal_mode = WAL;');

  db.exec(`CREATE TABLE IF NOT EXISTS stores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    code TEXT UNIQUE NOT NULL,
    created_at INTEGER
  );`);

  db.exec(`CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    recovery_pin_hash TEXT,
    role TEXT DEFAULT 'CASHIER',
    created_at INTEGER,
    FOREIGN KEY (store_id) REFERENCES stores(id)
  );`);

  // Migrations for existing database tables
  try { db.exec("ALTER TABLE users ADD COLUMN recovery_pin_hash TEXT;"); } catch (e) {}
  try { db.exec("ALTER TABLE customers ADD COLUMN updated_at INTEGER;"); } catch (e) {}
  try { db.exec("ALTER TABLE suppliers ADD COLUMN updated_at INTEGER;"); } catch (e) {}

  db.exec(`CREATE TABLE IF NOT EXISTS refresh_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    token TEXT UNIQUE NOT NULL,
    expires_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
  );`);

  db.exec(`CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    barcode TEXT NOT NULL,
    category TEXT DEFAULT 'General',
    purchase_price REAL DEFAULT 0.0,
    sale_price REAL DEFAULT 0.0,
    stock_quantity REAL DEFAULT 0.0,
    min_stock_alert REAL DEFAULT 5.0,
    unit TEXT DEFAULT 'Pcs',
    updated_at INTEGER,
    FOREIGN KEY (store_id) REFERENCES stores(id)
  );`);

  db.exec(`CREATE TABLE IF NOT EXISTS customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    current_balance REAL DEFAULT 0.0,
    updated_at INTEGER,
    FOREIGN KEY (store_id) REFERENCES stores(id)
  );`);

  db.exec(`CREATE TABLE IF NOT EXISTS suppliers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    phone TEXT,
    contact_person TEXT,
    balance REAL DEFAULT 0.0,
    updated_at INTEGER,
    FOREIGN KEY (store_id) REFERENCES stores(id)
  );`);

  db.exec(`CREATE TABLE IF NOT EXISTS invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    invoice_no TEXT UNIQUE NOT NULL,
    customer_id INTEGER,
    customer_name TEXT DEFAULT 'Walking Customer',
    subtotal REAL DEFAULT 0.0,
    discount_amount REAL DEFAULT 0.0,
    tax_amount REAL DEFAULT 0.0,
    total_amount REAL DEFAULT 0.0,
    amount_received REAL DEFAULT 0.0,
    amount_applied REAL DEFAULT 0.0,
    change_due REAL DEFAULT 0.0,
    balance_due REAL DEFAULT 0.0,
    payment_method TEXT DEFAULT 'CASH',
    payment_status TEXT DEFAULT 'PAID',
    cashier_name TEXT DEFAULT 'Cashier',
    sync_status TEXT DEFAULT 'SYNCED',
    created_at INTEGER,
    FOREIGN KEY (store_id) REFERENCES stores(id)
  );`);

  db.exec(`CREATE TABLE IF NOT EXISTS invoice_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    invoice_id INTEGER NOT NULL,
    product_id INTEGER,
    product_name TEXT,
    barcode TEXT,
    quantity REAL DEFAULT 1.0,
    unit_price REAL DEFAULT 0.0,
    total_price REAL DEFAULT 0.0,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
  );`);

  db.exec(`CREATE TABLE IF NOT EXISTS payment_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    invoice_id INTEGER NOT NULL,
    invoice_no TEXT,
    customer_id INTEGER,
    amount REAL DEFAULT 0.0,
    payment_method TEXT DEFAULT 'CASH',
    cashier_name TEXT,
    notes TEXT,
    idempotency_key TEXT UNIQUE,
    created_at INTEGER,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
  );`);

  db.exec(`CREATE TABLE IF NOT EXISTS customer_ledger (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    customer_id INTEGER NOT NULL,
    invoice_id INTEGER,
    transaction_type TEXT,
    debit_amount REAL DEFAULT 0.0,
    credit_amount REAL DEFAULT 0.0,
    balance_after REAL DEFAULT 0.0,
    description TEXT,
    created_at INTEGER,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
  );`);

  const sCount = db.prepare('SELECT COUNT(*) as c FROM stores').get();
  if (sCount && sCount.c === 0) {
    const now = Date.now();
    db.prepare("INSERT INTO stores (id, name, code, created_at) VALUES (1, 'Choudhury Main Branch', 'STORE-01', ?)").run(now);
    db.prepare("INSERT INTO stores (id, name, code, created_at) VALUES (2, 'Choudhury Branch 2', 'STORE-02', ?)").run(now);

    const hash = hashPassword('admin123');
    const recPin = hashRecoveryPin('7860');
    db.prepare("INSERT INTO users (store_id, username, password_hash, recovery_pin_hash, role, created_at) VALUES (1, 'admin', ?, ?, 'OWNER', ?)").run(hash, recPin, now);

    const insProd = db.prepare('INSERT INTO products (store_id, name, barcode, category, purchase_price, sale_price, stock_quantity, unit, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)');
    insProd.run(1, 'Super Basmati Rice 1kg (سپر باسمتی چاول)', '8964001', 'Grocery', 320.0, 380.0, 150, 'kg', now);
    insProd.run(1, 'Tapal Danedar Tea 400g (ٹیپال دانے دار چائے)', '8964002', 'Beverages', 550.0, 620.0, 80, 'Pcs', now);
    insProd.run(1, 'Dalda Cooking Oil 1L (ڈالڈا کوکنگ آئل)', '8964003', 'Oils', 510.0, 580.0, 95, 'Ltr', now);
    insProd.run(1, 'National Chilli Garlic Sauce 500g', '8964004', 'Condiments', 280.0, 330.0, 60, 'Pcs', now);
    insProd.run(2, 'Branch 2 Fresh Milk 1L', '8964005', 'Dairy', 180.0, 210.0, 50, 'Ltr', now);

    const insCust = db.prepare('INSERT INTO customers (store_id, name, phone, address, current_balance, updated_at) VALUES (?, ?, ?, ?, ?, ?)');
    insCust.run(1, 'Haji Muhammad Aslam (حاجی محمد اسلم)', '0300-1234567', 'Shop #12, Commercial Market', 1500.0, now);
    insCust.run(1, 'Malik Tariq (ملک طارق)', '0321-7654321', 'House 45, Street 9', 0.0, now);
  }
}

initDb();

function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization'
  });
  res.end(JSON.stringify(data));
}

function sendFile(res, filePath, contentType, isAttachment = false, filename = '') {
  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      return sendJson(res, 404, { error: 'File not found' });
    }
    const headers = {
      'Content-Type': contentType,
      'Content-Length': stats.size,
      'Access-Control-Allow-Origin': '*',
      'Cache-Control': 'public, max-age=0'
    };
    if (isAttachment) {
      headers['Content-Disposition'] = `attachment; filename="${filename || path.basename(filePath)}"`;
    }
    res.writeHead(200, headers);
    fs.createReadStream(filePath).pipe(res);
  });
}

function parseBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (e) {
        resolve({});
      }
    });
  });
}

function getAuthUser(req) {
  const authHeader = req.headers['authorization'];
  if (!authHeader) return null;
  const token = authHeader.startsWith('Bearer ') ? authHeader.substring(7) : authHeader;
  return verifyToken(token, JWT_SECRET);
}

const server = http.createServer(async (req, res) => {
  // CORS Preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    });
    return res.end();
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;
  const method = req.method;
  const authUser = getAuthUser(req);

  try {
    // ---------------- 1. Health & Status ----------------
    if (pathname === '/.well-known/assetlinks.json' && method === 'GET') {
      const assetlinks = [{
        relation: ['delegate_permission/common.handle_all_urls'],
        target: {
          namespace: 'android_app',
          package_name: 'com.aistudio.sentrystore.pos',
          sha256_cert_fingerprints: [
            'EA:0C:37:99:F7:FE:A3:44:BF:C9:BD:B2:73:A5:93:C2:19:D6:B0:20:B5:9E:F7:4D:9D:62:DE:14:6E:2D:D9:93'
          ]
        }
      }];
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify(assetlinks, null, 2));
    }

    if (pathname === '/api/health' && (method === 'GET' || method === 'HEAD')) {
      const sCount = db.prepare('SELECT COUNT(*) as c FROM stores').get();
      const uCount = db.prepare('SELECT COUNT(*) as c FROM users').get();
      const pCount = db.prepare('SELECT COUNT(*) as c FROM products').get();
      const iCount = db.prepare('SELECT COUNT(*) as c FROM invoices').get();
      let dbBytes = 0;
      try { dbBytes = fs.statSync(DB_PATH).size; } catch (e) {}

      return sendJson(res, 200, {
        status: 'ok',
        service: 'choudhury-pos-backend',
        version: '8.1.0',
        environment: process.env.NODE_ENV || 'production',
        uptime: process.uptime(),
        timestamp: Date.now(),
        storesCount: sCount ? sCount.c : 1,
        usersCount: uCount ? uCount.c : 1,
        productsCount: pCount ? pCount.c : 0,
        invoicesCount: iCount ? iCount.c : 0,
        dbSizeBytes: dbBytes,
        port: PORT
      });
    }

    // ---------------- 2. Direct Downloads ----------------
    if (pathname === '/downloads/choudhury-pos-app.apk' && (method === 'GET' || method === 'HEAD')) {
      return sendFile(res, path.join(DOWNLOADS_DIR, 'choudhury-pos-app.apk'), 'application/vnd.android.package-archive', true, 'choudhury-pos-app.apk');
    }
    if (pathname === '/downloads/choudhury-pos-windows-x64.zip' && (method === 'GET' || method === 'HEAD')) {
      return sendFile(res, path.join(DOWNLOADS_DIR, 'choudhury-pos-windows-x64.zip'), 'application/zip', true, 'choudhury-pos-windows-x64.zip');
    }

    // ---------------- 3. Shared User Accounts & Authentication ----------------
    if (pathname === '/api/auth/register' && method === 'POST') {
      const body = await parseBody(req);
      const { username, password, storeName = 'My Retail Store', recoveryPin = '1234', role = 'OWNER' } = body;
      if (!username || !password) {
        return sendJson(res, 400, { error: 'Username and password required' });
      }

      const existing = db.prepare('SELECT id FROM users WHERE username = ?').get(username);
      if (existing) {
        return sendJson(res, 409, { error: 'Username already exists' });
      }

      const now = Date.now();
      const code = 'STORE-' + Math.floor(Math.random() * 9000 + 1000);
      const storeRes = db.prepare('INSERT INTO stores (name, code, created_at) VALUES (?, ?, ?)').run(storeName, code, now);
      const storeId = Number(storeRes.lastInsertRowid);

      const passHash = hashPassword(password);
      const pinHash = hashRecoveryPin(recoveryPin);
      const userRes = db.prepare('INSERT INTO users (store_id, username, password_hash, recovery_pin_hash, role, created_at) VALUES (?, ?, ?, ?, ?, ?)')
        .run(storeId, username, passHash, pinHash, role, now);
      const userId = Number(userRes.lastInsertRowid);

      const token = signToken({ userId, username, storeId, role }, JWT_SECRET, 86400); // 24h
      const refreshToken = crypto.randomBytes(32).toString('hex');
      db.prepare('INSERT INTO refresh_tokens (user_id, token, expires_at, created_at) VALUES (?, ?, ?, ?)')
        .run(userId, refreshToken, now + (30 * 86400 * 1000), now);

      return sendJson(res, 201, {
        success: true,
        token,
        refreshToken,
        user: { id: userId, username, storeId, storeName, role }
      });
    }

    if (pathname === '/api/auth/login' && method === 'POST') {
      const body = await parseBody(req);
      const { username, password } = body;
      if (!username || !password) {
        return sendJson(res, 400, { error: 'Username and password required' });
      }

      const user = db.prepare('SELECT u.*, s.name as storeName FROM users u LEFT JOIN stores s ON u.store_id = s.id WHERE u.username = ?')
        .get(username);

      if (!user || !verifyPassword(password, user.password_hash)) {
        return sendJson(res, 401, { error: 'Invalid username or password' });
      }

      // Transparent controlled migration to Bcrypt
      if (!user.password_hash.startsWith('$2')) {
        const upgraded = hashPassword(password);
        db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(upgraded, user.id);
      }

      const token = signToken({ userId: user.id, username: user.username, storeId: user.store_id, role: user.role }, JWT_SECRET, 86400);
      const refreshToken = crypto.randomBytes(32).toString('hex');
      const now = Date.now();
      db.prepare('INSERT INTO refresh_tokens (user_id, token, expires_at, created_at) VALUES (?, ?, ?, ?)')
        .run(user.id, refreshToken, now + (30 * 86400 * 1000), now);

      return sendJson(res, 200, {
        token,
        refreshToken,
        user: { id: user.id, username: user.username, storeId: user.store_id, storeName: user.storeName || 'Main Store', role: user.role }
      });
    }

    if (pathname === '/api/auth/logout' && method === 'POST') {
      if (authUser) {
        db.prepare('DELETE FROM refresh_tokens WHERE user_id = ?').run(authUser.userId);
      }
      return sendJson(res, 200, { success: true, message: 'Session revoked and logged out successfully' });
    }

    if (pathname === '/api/auth/refresh' && method === 'POST') {
      const body = await parseBody(req);
      const { refreshToken } = body;
      if (!refreshToken) return sendJson(res, 400, { error: 'Refresh token required' });

      const record = db.prepare('SELECT r.*, u.username, u.store_id, u.role FROM refresh_tokens r JOIN users u ON r.user_id = u.id WHERE r.token = ? AND r.expires_at > ?')
        .get(refreshToken, Date.now());

      if (!record) return sendJson(res, 401, { error: 'Invalid or expired refresh token' });

      const newToken = signToken({ userId: record.user_id, username: record.username, storeId: record.store_id, role: record.role }, JWT_SECRET, 86400);
      return sendJson(res, 200, { token: newToken, refreshToken });
    }

    if (pathname === '/api/auth/change-username' && method === 'POST') {
      if (!authUser) return sendJson(res, 401, { error: 'Unauthorized' });
      const body = await parseBody(req);
      const { newUsername } = body;
      if (!newUsername) return sendJson(res, 400, { error: 'New username required' });

      const conflict = db.prepare('SELECT id FROM users WHERE username = ? AND id != ?').get(newUsername, authUser.userId);
      if (conflict) return sendJson(res, 409, { error: 'Username already in use' });

      db.prepare('UPDATE users SET username = ? WHERE id = ?').run(newUsername, authUser.userId);
      return sendJson(res, 200, { success: true, message: 'Username updated', username: newUsername });
    }

    if (pathname === '/api/auth/change-password' && method === 'POST') {
      if (!authUser) return sendJson(res, 401, { error: 'Unauthorized' });
      const body = await parseBody(req);
      const { currentPassword, newPassword } = body;
      if (!currentPassword || !newPassword) return sendJson(res, 400, { error: 'Current and new password required' });

      const user = db.prepare('SELECT id, password_hash FROM users WHERE id = ?').get(authUser.userId);
      if (!user || !verifyPassword(currentPassword, user.password_hash)) {
        return sendJson(res, 401, { error: 'Current password incorrect' });
      }

      const newHash = hashPassword(newPassword);
      db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(newHash, authUser.userId);
      // Revoke all existing sessions on password change
      db.prepare('DELETE FROM refresh_tokens WHERE user_id = ?').run(authUser.userId);
      return sendJson(res, 200, { success: true, message: 'Password updated successfully. All other sessions revoked.' });
    }

    if (pathname === '/api/auth/recover-account' && method === 'POST') {
      const body = await parseBody(req);
      const { username, recoveryPin, newPassword } = body;
      if (!username || !recoveryPin || !newPassword) {
        return sendJson(res, 400, { error: 'Username, recovery PIN, and new password required' });
      }

      const user = db.prepare('SELECT id, recovery_pin_hash FROM users WHERE username = ?').get(username);
      if (!user || !verifyRecoveryPin(recoveryPin, user.recovery_pin_hash)) {
        return sendJson(res, 401, { error: 'Invalid recovery details' });
      }

      const newPassHash = hashPassword(newPassword);
      db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(newPassHash, user.id);
      db.prepare('DELETE FROM refresh_tokens WHERE user_id = ?').run(user.id); // revoke sessions

      return sendJson(res, 200, { success: true, message: 'Password reset successfully. Please login with your new password.' });
    }

    if (pathname === '/api/auth/me' && method === 'GET') {
      if (!authUser) return sendJson(res, 401, { error: 'Unauthorized' });
      const user = db.prepare('SELECT u.id, u.username, u.store_id, u.role, s.name as storeName FROM users u LEFT JOIN stores s ON u.store_id = s.id WHERE u.id = ?')
        .get(authUser.userId);
      if (!user) return sendJson(res, 404, { error: 'User not found' });
      return sendJson(res, 200, user);
    }

    // ---------------- 4. Real Bidirectional Synchronization ----------------
    // Android sends batch of offline changes to server
    if (pathname === '/api/sync/push' && method === 'POST') {
      const effectiveStoreId = authUser ? authUser.storeId : (Number(parsedUrl.searchParams.get('storeId')) || 1);
      const body = await parseBody(req);
      const { products = [], customers = [], invoices = [] } = body;
      const now = Date.now();

      let syncedInvoices = 0;
      let syncedProducts = 0;
      let syncedCustomers = 0;

      // Upsert Products
      const prodUpsert = db.prepare(`
        INSERT INTO products (store_id, name, barcode, category, purchase_price, sale_price, stock_quantity, unit, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `);
      const prodUpdate = db.prepare(`
        UPDATE products SET name = ?, category = ?, purchase_price = ?, sale_price = ?, stock_quantity = ?, unit = ?, updated_at = ?
        WHERE barcode = ? AND store_id = ?
      `);

      for (const p of products) {
        const exist = db.prepare('SELECT id, updated_at FROM products WHERE barcode = ? AND store_id = ?').get(p.barcode, effectiveStoreId);
        if (exist) {
          if (!exist.updated_at || (p.updatedAt && p.updatedAt > exist.updated_at)) {
            prodUpdate.run(p.name, p.category || 'General', p.purchasePrice || 0, p.salePrice || 0, p.stockQuantity || 0, p.unit || 'Pcs', p.updatedAt || now, p.barcode, effectiveStoreId);
            syncedProducts++;
          }
        } else {
          prodUpsert.run(effectiveStoreId, p.name, p.barcode, p.category || 'General', p.purchasePrice || 0, p.salePrice || 0, p.stockQuantity || 0, p.unit || 'Pcs', p.updatedAt || now);
          syncedProducts++;
        }
      }

      // Upsert Customers
      const custUpsert = db.prepare(`
        INSERT INTO customers (store_id, name, phone, address, current_balance, updated_at)
        VALUES (?, ?, ?, ?, ?, ?)
      `);
      for (const c of customers) {
        const exist = db.prepare('SELECT id FROM customers WHERE name = ? AND store_id = ?').get(c.name, effectiveStoreId);
        if (!exist) {
          custUpsert.run(effectiveStoreId, c.name, c.phone || '', c.address || '', c.currentBalance || 0, c.updatedAt || now);
          syncedCustomers++;
        }
      }

      // Insert Invoices (idempotent duplicate prevention)
      const invStmt = db.prepare(`
        INSERT OR IGNORE INTO invoices (
          store_id, invoice_no, customer_id, customer_name, subtotal, discount_amount, tax_amount,
          total_amount, amount_received, amount_applied, change_due, balance_due,
          payment_method, payment_status, cashier_name, sync_status, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCED', ?)
      `);

      for (const inv of invoices) {
        const res = invStmt.run(
          effectiveStoreId,
          inv.invoiceNo,
          inv.customerId || null,
          inv.customerName || 'Walking Customer',
          inv.subtotal || 0,
          inv.discountAmount || 0,
          inv.taxAmount || 0,
          inv.totalAmount || 0,
          inv.amountReceived || 0,
          inv.amountApplied || 0,
          inv.changeDue || 0,
          inv.balanceDue || 0,
          inv.paymentMethod || 'CASH',
          inv.paymentStatus || 'PAID',
          inv.cashierName || 'Android POS',
          inv.createdAt || now
        );
        if (res.changes > 0) syncedInvoices++;
      }

      return sendJson(res, 200, {
        success: true,
        serverTimestamp: now,
        storeId: effectiveStoreId,
        syncedProducts,
        syncedCustomers,
        syncedInvoices
      });
    }

    // Android pulls changes from server
    if (pathname === '/api/sync/pull' && method === 'GET') {
      const effectiveStoreId = authUser ? authUser.storeId : (Number(parsedUrl.searchParams.get('storeId')) || 1);
      const since = Number(parsedUrl.searchParams.get('since')) || 0;

      const products = db.prepare('SELECT * FROM products WHERE store_id = ? AND (updated_at > ? OR ? = 0)').all(effectiveStoreId, since, since);
      const customers = db.prepare('SELECT * FROM customers WHERE store_id = ? AND (updated_at > ? OR ? = 0)').all(effectiveStoreId, since, since);
      const invoices = db.prepare('SELECT * FROM invoices WHERE store_id = ? AND (created_at > ? OR ? = 0) ORDER BY id DESC LIMIT 100').all(effectiveStoreId, since, since);

      return sendJson(res, 200, {
        serverTimestamp: Date.now(),
        storeId: effectiveStoreId,
        products,
        customers,
        invoices
      });
    }

    // ---------------- 5. Business Modules (Products, Invoices, Customers, Reports) ----------------
    // Products
    if (pathname === '/api/products') {
      const storeId = authUser ? authUser.storeId : (Number(parsedUrl.searchParams.get('storeId')) || 1);
      if (method === 'GET') {
        const search = parsedUrl.searchParams.get('search');
        let sql = 'SELECT * FROM products WHERE store_id = ?';
        const params = [storeId];
        if (search) {
          sql += ' AND (name LIKE ? OR barcode LIKE ?)';
          params.push(`%${search}%`, `%${search}%`);
        }
        sql += ' ORDER BY id DESC';
        return sendJson(res, 200, db.prepare(sql).all(...params));
      }
      if (method === 'POST') {
        const body = await parseBody(req);
        const { name, barcode, category = 'General', purchase_price = 0, sale_price = 0, stock_quantity = 0, unit = 'Pcs' } = body;
        const result = db.prepare(
          'INSERT INTO products (store_id, name, barcode, category, purchase_price, sale_price, stock_quantity, unit, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        ).run(storeId, name, barcode, category, purchase_price, sale_price, stock_quantity, unit, Date.now());
        return sendJson(res, 201, { id: Number(result.lastInsertRowid), store_id: storeId, name, barcode, sale_price, stock_quantity });
      }
    }

    // Invoices / Sales
    if (pathname === '/api/invoices' || pathname === '/api/sales') {
      const storeId = authUser ? authUser.storeId : (Number(parsedUrl.searchParams.get('storeId')) || 1);
      if (method === 'GET') {
        return sendJson(res, 200, db.prepare('SELECT * FROM invoices WHERE store_id = ? ORDER BY id DESC').all(storeId));
      }
      if (method === 'POST') {
        const body = await parseBody(req);
        const {
          invoiceNo,
          customerId = null,
          customerName = 'Walking Customer',
          items = [],
          subtotal = 0,
          discountAmount = 0,
          taxAmount = 0,
          totalAmount,
          amountReceived = 0,
          paymentMethod = 'CASH',
          cashierName = 'Cashier 1'
        } = body;

        const total = Math.max(0, totalAmount !== undefined ? Number(totalAmount) : (Number(subtotal) - Number(discountAmount) + Number(taxAmount)));
        const received = Math.max(0, Number(amountReceived) || 0);

        const amountApplied = Math.min(total, received);
        const changeDue = Math.max(0, received - total);
        const balanceDue = Math.max(0, total - amountApplied);

        let paymentStatus = 'UNPAID';
        if (balanceDue <= 0.001) {
          paymentStatus = 'PAID';
        } else if (amountApplied > 0) {
          paymentStatus = 'PARTIALLY_PAID';
        }

        const generatedNo = invoiceNo || `INV-${Date.now().toString().slice(-4)}-${Math.floor(Math.random() * 9000 + 1000)}`;
        const now = body.createdAt || Date.now();

        const invResult = db.prepare(`
          INSERT INTO invoices (
            store_id, invoice_no, customer_id, customer_name, subtotal, discount_amount, tax_amount,
            total_amount, amount_received, amount_applied, change_due, balance_due,
            payment_method, payment_status, cashier_name, sync_status, created_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCED', ?)
        `).run(storeId, generatedNo, customerId, customerName, subtotal, discountAmount, taxAmount, total, received, amountApplied, changeDue, balanceDue, paymentMethod, paymentStatus, cashierName, now);

        const invoiceId = Number(invResult.lastInsertRowid);

        const itemStmt = db.prepare('INSERT INTO invoice_items (store_id, invoice_id, product_id, product_name, barcode, quantity, unit_price, total_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
        const stockStmt = db.prepare('UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND store_id = ?');

        for (const item of items) {
          const pId = item.productId || item.id || 0;
          const pName = item.productName || item.name || '';
          const bCode = item.barcode || '';
          const qty = Number(item.quantity) || 1;
          const uPrice = Number(item.unitPrice || item.salePrice) || 0;
          const tPrice = Number(item.totalPrice) || (qty * uPrice);
          itemStmt.run(storeId, invoiceId, pId, pName, bCode, qty, uPrice, tPrice);
          if (pId > 0) stockStmt.run(qty, pId, storeId);
        }

        if (amountApplied > 0) {
          const idempotencyKey = `INIT-${invoiceId}`;
          db.prepare(`
            INSERT INTO payment_records (store_id, invoice_id, invoice_no, customer_id, amount, payment_method, cashier_name, notes, idempotency_key, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Initial checkout payment', ?, ?)
          `).run(storeId, invoiceId, generatedNo, customerId, amountApplied, paymentMethod, cashierName, idempotencyKey, now);
        }

        if (balanceDue > 0.001 && customerId) {
          db.prepare('UPDATE customers SET current_balance = current_balance + ? WHERE id = ?').run(balanceDue, customerId);
          const cRow = db.prepare('SELECT current_balance FROM customers WHERE id = ?').get(customerId);
          const balAfter = cRow ? cRow.current_balance : balanceDue;
          db.prepare(`
            INSERT INTO customer_ledger (store_id, customer_id, invoice_id, transaction_type, debit_amount, credit_amount, balance_after, description, created_at)
            VALUES (?, ?, ?, 'SALE_CREDIT', ?, 0.0, ?, ?, ?)
          `).run(storeId, customerId, invoiceId, balanceDue, balAfter, `Unpaid balance on invoice ${generatedNo}`, now);
        }

        return sendJson(res, 201, {
          id: invoiceId,
          invoiceNo: generatedNo,
          storeId,
          totalAmount: total,
          amountReceived: received,
          amountApplied,
          changeDue,
          balanceDue,
          paymentStatus,
          paymentMethod
        });
      }
    }

    // Later Payment Settlement
    if (pathname === '/api/invoices/settle' && method === 'POST') {
      const storeId = authUser ? authUser.storeId : (Number(parsedUrl.searchParams.get('storeId')) || 1);
      const body = await parseBody(req);
      const { invoiceId, paymentAmount, paymentMethod = 'CASH', cashierName = 'Cashier', idempotencyKey = `PAY-${Date.now()}` } = body;
      const payAmt = Number(paymentAmount) || 0;

      if (!invoiceId || payAmt <= 0) return sendJson(res, 400, { error: 'Valid invoiceId and positive paymentAmount required' });

      const existing = db.prepare('SELECT * FROM payment_records WHERE idempotency_key = ?').get(idempotencyKey);
      if (existing) return sendJson(res, 409, { error: 'Duplicate payment transaction detected', record: existing });

      const invoice = db.prepare('SELECT * FROM invoices WHERE id = ? AND store_id = ?').get(invoiceId, storeId);
      if (!invoice) return sendJson(res, 404, { error: 'Invoice not found or access denied for this store' });

      const applied = Math.min(invoice.balance_due, payAmt);
      const newBalance = Math.max(0, invoice.balance_due - applied);
      const newStatus = newBalance <= 0.001 ? 'PAID' : 'PARTIALLY_PAID';
      const now = Date.now();

      db.prepare(`
        INSERT INTO payment_records (store_id, invoice_id, invoice_no, customer_id, amount, payment_method, cashier_name, notes, idempotency_key, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'Balance settlement', ?, ?)
      `).run(storeId, invoice.id, invoice.invoice_no, invoice.customer_id, applied, paymentMethod, cashierName, idempotencyKey, now);

      db.prepare('UPDATE invoices SET amount_applied = amount_applied + ?, balance_due = ?, payment_status = ? WHERE id = ? AND store_id = ?')
        .run(applied, newBalance, newStatus, invoice.id, storeId);

      if (invoice.customer_id) {
        db.prepare('UPDATE customers SET current_balance = MAX(0.0, current_balance - ?) WHERE id = ? AND store_id = ?').run(applied, invoice.customer_id, storeId);
        const cRow = db.prepare('SELECT current_balance FROM customers WHERE id = ? AND store_id = ?').get(invoice.customer_id, storeId);
        const balAfter = cRow ? cRow.current_balance : 0.0;
        db.prepare(`
          INSERT INTO customer_ledger (store_id, customer_id, invoice_id, transaction_type, debit_amount, credit_amount, balance_after, description, created_at)
          VALUES (?, ?, ?, 'PAYMENT_RECEIVED', 0.0, ?, ?, ?, ?)
        `).run(storeId, invoice.customer_id, invoice.id, applied, balAfter, `Payment received for invoice ${invoice.invoice_no}`, now);
      }

      return sendJson(res, 200, {
        success: true,
        invoiceId: invoice.id,
        amountApplied: applied,
        balanceDue: newBalance,
        paymentStatus: newStatus
      });
    }

    const paymentMatch = pathname.match(/^\/api\/invoices\/(\d+)\/payments$/);
    if (paymentMatch && method === 'POST') {
      const invoiceId = Number(paymentMatch[1]);
      const storeId = authUser ? authUser.storeId : (Number(parsedUrl.searchParams.get('storeId')) || 1);
      const body = await parseBody(req);
      const { amount, paymentMethod = 'CASH', cashierName = 'Cashier', idempotencyKey = `PAY-${Date.now()}` } = body;
      const payAmt = Number(amount) || 0;

      if (payAmt <= 0) return sendJson(res, 400, { error: 'Payment amount must be greater than zero' });

      const existing = db.prepare('SELECT * FROM payment_records WHERE idempotency_key = ?').get(idempotencyKey);
      if (existing) return sendJson(res, 409, { error: 'Duplicate payment transaction detected', record: existing });

      const invoice = db.prepare('SELECT * FROM invoices WHERE id = ? AND store_id = ?').get(invoiceId, storeId);
      if (!invoice) return sendJson(res, 404, { error: 'Invoice not found or access denied for this store' });

      const applied = Math.min(invoice.balance_due, payAmt);
      const newBalance = Math.max(0, invoice.balance_due - applied);
      const newStatus = newBalance <= 0.001 ? 'PAID' : 'PARTIALLY_PAID';
      const now = Date.now();

      db.prepare(`
        INSERT INTO payment_records (store_id, invoice_id, invoice_no, customer_id, amount, payment_method, cashier_name, notes, idempotency_key, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'Later balance settlement', ?, ?)
      `).run(storeId, invoice.id, invoice.invoice_no, invoice.customer_id, applied, paymentMethod, cashierName, idempotencyKey, now);

      db.prepare('UPDATE invoices SET amount_applied = amount_applied + ?, balance_due = ?, payment_status = ? WHERE id = ? AND store_id = ?')
        .run(applied, newBalance, newStatus, invoice.id, storeId);

      if (invoice.customer_id) {
        db.prepare('UPDATE customers SET current_balance = MAX(0.0, current_balance - ?) WHERE id = ?').run(applied, invoice.customer_id);
        const cRow = db.prepare('SELECT current_balance FROM customers WHERE id = ?').get(invoice.customer_id);
        const balAfter = cRow ? cRow.current_balance : 0.0;
        db.prepare(`
          INSERT INTO customer_ledger (store_id, customer_id, invoice_id, transaction_type, debit_amount, credit_amount, balance_after, description, created_at)
          VALUES (?, ?, ?, 'PAYMENT_RECEIVED', 0.0, ?, ?, ?, ?)
        `).run(storeId, invoice.customer_id, invoice.id, applied, balAfter, `Payment received for invoice ${invoice.invoice_no}`, now);
      }

      return sendJson(res, 200, {
        success: true,
        invoiceId: invoice.id,
        amountApplied: applied,
        balanceDue: newBalance,
        paymentStatus: newStatus
      });
    }

    // Customers & Ledger
    if (pathname === '/api/customers') {
      const storeId = authUser ? authUser.storeId : (Number(parsedUrl.searchParams.get('storeId')) || 1);
      if (method === 'GET') {
        return sendJson(res, 200, db.prepare('SELECT * FROM customers WHERE store_id = ? ORDER BY name ASC').all(storeId));
      }
      if (method === 'POST') {
        const body = await parseBody(req);
        const { name, phone = '', address = '', initialBalance = 0.0 } = body;
        if (!name) return sendJson(res, 400, { error: 'Customer name required' });
        const result = db.prepare(
          'INSERT INTO customers (store_id, name, phone, address, current_balance, updated_at) VALUES (?, ?, ?, ?, ?, ?)'
        ).run(storeId, name, phone, address, initialBalance, Date.now());
        return sendJson(res, 201, { id: Number(result.lastInsertRowid), storeId, name, phone, current_balance: initialBalance });
      }
    }

    const ledgerMatch = pathname.match(/^\/api\/customers\/(\d+)\/ledger$/);
    if (ledgerMatch && method === 'GET') {
      const custId = Number(ledgerMatch[1]);
      return sendJson(res, 200, db.prepare('SELECT * FROM customer_ledger WHERE customer_id = ? ORDER BY id DESC').all(custId));
    }

    // Reports
    if (pathname === '/api/reports/summary' && method === 'GET') {
      const storeId = authUser ? authUser.storeId : (Number(parsedUrl.searchParams.get('storeId')) || 1);
      const rows = db.prepare('SELECT total_amount, amount_received, change_due, balance_due FROM invoices WHERE store_id = ?').all(storeId);
      const totalSalesRevenue = rows.reduce((sum, r) => sum + (r.total_amount || 0), 0);
      const totalCashReceived = rows.reduce((sum, r) => sum + (r.amount_received || 0), 0);
      const totalChangeReturned = rows.reduce((sum, r) => sum + (r.change_due || 0), 0);
      const netCashInDrawer = totalCashReceived - totalChangeReturned;
      const totalReceivables = rows.reduce((sum, r) => sum + (r.balance_due || 0), 0);
      return sendJson(res, 200, {
        storeId: Number(storeId),
        totalSalesRevenue,
        totalCashReceived,
        totalChangeReturned,
        netCashInDrawer,
        totalReceivables,
        invoiceCount: rows.length
      });
    }

    // ---------------- 6. Static Shell & PWA Assets ----------------
    let staticFilePath = path.join(PUBLIC_DIR, pathname === '/' ? 'index.html' : pathname);
    if (!fs.existsSync(staticFilePath)) {
      staticFilePath = path.join(PUBLIC_DIR, 'index.html');
    }
    const ext = path.extname(staticFilePath).toLowerCase();
    const mimeTypes = {
      '.html': 'text/html; charset=utf-8',
      '.js': 'application/javascript',
      '.css': 'text/css',
      '.json': 'application/json',
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.svg': 'image/svg+xml'
    };
    return sendFile(res, staticFilePath, mimeTypes[ext] || 'text/html; charset=utf-8');

  } catch (err) {
    console.error('Server error on', pathname, err);
    return sendJson(res, 500, { error: err.message });
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Choudhury POS server listening on 0.0.0.0:${PORT}`);
});
