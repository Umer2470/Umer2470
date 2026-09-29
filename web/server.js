const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const { DatabaseSync } = require('node:sqlite');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const PORT = 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'choudhury-pos-secret-key-2026';
const DB_PATH = path.join(__dirname, 'pos_central.db');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Serve static frontend files
app.use(express.static(path.join(__dirname, 'public')));

// --------------------------------------------------------------------------
// DATABASE INITIALIZATION & SCHEMA (Native Node 22 SQLite)
// --------------------------------------------------------------------------
const db = new DatabaseSync(DB_PATH);
db.pragma = (str) => db.exec('PRAGMA ' + str);
db.transaction = (fn) => (...args) => {
  db.exec('BEGIN IMMEDIATE');
  try {
    const res = fn(...args);
    db.exec('COMMIT');
    return res;
  } catch (err) {
    try { db.exec('ROLLBACK'); } catch (_) {}
    throw err;
  }
};
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

function initDb() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS stores (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      code TEXT UNIQUE NOT NULL,
      address TEXT,
      phone TEXT,
      email TEXT,
      tax_number TEXT,
      is_active INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      full_name TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'CASHIER', -- SUPER_ADMIN, ADMIN, SUPERVISOR, CASHIER
      store_id INTEGER REFERENCES stores(id),
      pin TEXT,
      is_active INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS categories (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT UNIQUE NOT NULL,
      icon TEXT DEFAULT 'tag',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS products (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      barcode TEXT UNIQUE NOT NULL,
      sku TEXT,
      category TEXT DEFAULT 'General',
      brand TEXT DEFAULT 'General',
      purchase_price REAL DEFAULT 0.0,
      sale_price REAL NOT NULL,
      stock_quantity REAL DEFAULT 0.0,
      min_stock_alert REAL DEFAULT 5.0,
      unit TEXT DEFAULT 'Pcs',
      tax_percent REAL DEFAULT 0.0,
      store_id INTEGER DEFAULT 1,
      image_url TEXT,
      is_active INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS customers (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      phone TEXT,
      email TEXT,
      address TEXT,
      credit_limit REAL DEFAULT 10000.0,
      current_balance REAL DEFAULT 0.0,
      store_id INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS customer_ledger (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      customer_id INTEGER NOT NULL REFERENCES customers(id),
      transaction_type TEXT NOT NULL, -- SALE_CREDIT, PAYMENT_RECEIVED, RETURN_REFUND
      invoice_no TEXT,
      debit REAL DEFAULT 0.0,
      credit REAL DEFAULT 0.0,
      balance_after REAL NOT NULL,
      notes TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS suppliers (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      contact_person TEXT,
      phone TEXT,
      email TEXT,
      address TEXT,
      payable_balance REAL DEFAULT 0.0,
      store_id INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS supplier_ledger (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      supplier_id INTEGER NOT NULL REFERENCES suppliers(id),
      transaction_type TEXT NOT NULL, -- PURCHASE, PAYMENT_MADE
      reference_no TEXT,
      debit REAL DEFAULT 0.0,
      credit REAL DEFAULT 0.0,
      balance_after REAL NOT NULL,
      notes TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS purchases (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      purchase_no TEXT UNIQUE NOT NULL,
      supplier_id INTEGER REFERENCES suppliers(id),
      store_id INTEGER DEFAULT 1,
      total_amount REAL NOT NULL,
      paid_amount REAL DEFAULT 0.0,
      payment_status TEXT DEFAULT 'PAID', -- PAID, PARTIAL, UNPAID
      payment_method TEXT DEFAULT 'CASH',
      status TEXT DEFAULT 'RECEIVED',
      notes TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS purchase_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      purchase_id INTEGER NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
      product_id INTEGER NOT NULL REFERENCES products(id),
      quantity REAL NOT NULL,
      unit_cost REAL NOT NULL,
      total_cost REAL NOT NULL
    );

    CREATE TABLE IF NOT EXISTS register_shifts (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL REFERENCES users(id),
      store_id INTEGER DEFAULT 1,
      opened_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      closed_at DATETIME,
      opening_cash REAL NOT NULL DEFAULT 0.0,
      closing_cash_actual REAL,
      expected_cash REAL,
      cash_shortage_excess REAL,
      status TEXT DEFAULT 'OPEN', -- OPEN, CLOSED
      notes TEXT
    );

    CREATE TABLE IF NOT EXISTS sales (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      invoice_no TEXT UNIQUE NOT NULL,
      shift_id INTEGER REFERENCES register_shifts(id),
      store_id INTEGER DEFAULT 1,
      user_id INTEGER REFERENCES users(id),
      customer_id INTEGER REFERENCES customers(id),
      subtotal REAL NOT NULL,
      discount_amount REAL DEFAULT 0.0,
      tax_amount REAL DEFAULT 0.0,
      total_amount REAL NOT NULL,
      paid_amount REAL NOT NULL,
      change_due REAL DEFAULT 0.0,
      payment_method TEXT DEFAULT 'CASH', -- CASH, CARD, CREDIT, SPLIT
      status TEXT DEFAULT 'COMPLETED', -- COMPLETED, CANCELLED, REFUNDED
      notes TEXT,
      sync_origin TEXT DEFAULT 'WEB', -- WEB, ANDROID
      sync_status TEXT DEFAULT 'SYNCED',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS sale_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      sale_id INTEGER NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
      product_id INTEGER NOT NULL REFERENCES products(id),
      product_name TEXT NOT NULL,
      barcode TEXT,
      quantity REAL NOT NULL,
      sale_price REAL NOT NULL,
      purchase_price REAL DEFAULT 0.0,
      discount_amount REAL DEFAULT 0.0,
      total_price REAL NOT NULL
    );

    CREATE TABLE IF NOT EXISTS sale_returns (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      return_no TEXT UNIQUE NOT NULL,
      sale_id INTEGER REFERENCES sales(id),
      invoice_no TEXT NOT NULL,
      store_id INTEGER DEFAULT 1,
      refund_amount REAL NOT NULL,
      reason TEXT,
      restocked INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS sale_return_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      return_id INTEGER NOT NULL REFERENCES sale_returns(id) ON DELETE CASCADE,
      product_id INTEGER NOT NULL REFERENCES products(id),
      quantity REAL NOT NULL,
      refund_price REAL NOT NULL,
      total_refund REAL NOT NULL
    );

    CREATE TABLE IF NOT EXISTS expenses (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      category TEXT NOT NULL,
      amount REAL NOT NULL,
      payment_method TEXT DEFAULT 'CASH',
      recipient TEXT,
      description TEXT,
      store_id INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS store_settings (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      store_id INTEGER UNIQUE DEFAULT 1,
      store_name TEXT DEFAULT 'CHOUDHURY STORE POS',
      tagline TEXT DEFAULT 'Quality Products & Superior Service',
      address TEXT DEFAULT 'Main Market, Commercial Center',
      phone TEXT DEFAULT '+92-300-1234567',
      email TEXT DEFAULT 'contact@choudhurypos.com',
      tax_number TEXT DEFAULT 'NTN-9876543-2',
      currency_symbol TEXT DEFAULT 'Rs',
      default_tax_percent REAL DEFAULT 0.0,
      receipt_paper_size TEXT DEFAULT '80mm', -- 58mm, 80mm
      receipt_header TEXT DEFAULT 'Welcome to CHOUDHURY STORE POS',
      receipt_footer TEXT DEFAULT 'Thank you for your business! Please visit again.',
      barcode_format TEXT DEFAULT 'EAN_13',
      sound_enabled INTEGER DEFAULT 1,
      dark_mode INTEGER DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS sync_checkpoints (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      device_id TEXT NOT NULL,
      last_sync_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
      records_synced INTEGER DEFAULT 0,
      direction TEXT DEFAULT 'TWO_WAY'
    );
  `);

  // Seed default data if empty
  const storeCount = db.prepare('SELECT count(*) as cnt FROM stores').get().cnt;
  if (storeCount === 0) {
    db.prepare(`
      INSERT INTO stores (name, code, address, phone, email, tax_number)
      VALUES ('Main Store', 'BRANCH-01', 'Commercial Market, Lahore', '+92-300-1234567', 'info@choudhurypos.com', 'NTN-9876543-2')
    `).run();

    const salt = bcrypt.genSaltSync(10);
    const adminHash = bcrypt.hashSync('admin123', salt);
    const cashierHash = bcrypt.hashSync('1234', salt);

    db.prepare(`
      INSERT INTO users (username, password_hash, full_name, role, store_id, pin)
      VALUES 
        ('admin', ?, 'System Administrator', 'SUPER_ADMIN', 1, '1234'),
        ('cashier1', ?, 'Senior Cashier', 'CASHIER', 1, '1111')
    `).run(adminHash, cashierHash);

    db.prepare(`
      INSERT INTO store_settings (store_id, store_name, tagline, address, phone, currency_symbol)
      VALUES (1, 'CHOUDHURY STORE POS', 'Smart Retail Point of Sale', 'Commercial Market, Lahore', '+92-300-1234567', 'Rs')
    `).run();

    // Seed categories
    const categories = ['Grocery', 'Beverages', 'Dairy', 'Snacks', 'Personal Care', 'Household', 'Bakery'];
    const insertCat = db.prepare('INSERT INTO categories (name) VALUES (?)');
    for (const cat of categories) {
      insertCat.run(cat);
    }

    // Seed products matching the real retail catalog
    const insertProd = db.prepare(`
      INSERT INTO products (name, barcode, sku, category, brand, purchase_price, sale_price, stock_quantity, min_stock_alert, unit)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    insertProd.run('Super Basmati Rice 1 Kg', '8901234567890', 'SKU-RICE-001', 'Grocery', 'Guard', 210.0, 260.0, 85.0, 10.0, 'Kg');
    insertProd.run('Dalda Cooking Oil 1 Litre', '8909876543210', 'SKU-OIL-002', 'Grocery', 'Dalda', 450.0, 520.0, 42.0, 10.0, 'Bottle');
    insertProd.run('Refined Sugar 1 Kg', '2000000000017', 'SKU-SUGAR-003', 'Grocery', 'Habib', 120.0, 145.0, 150.0, 20.0, 'Kg');
    insertProd.run('Nestle MilkPak 1000ml', '8904561237891', 'SKU-MILK-004', 'Dairy', 'Nestle', 240.0, 280.0, 60.0, 15.0, 'Pack');
    insertProd.run('Tapal Danedar Tea 400g', '8906549873215', 'SKU-TEA-005', 'Beverages', 'Tapal', 480.0, 560.0, 35.0, 8.0, 'Box');
    insertProd.run('Lays Classic Chips 50g', '8907894561238', 'SKU-SNACK-006', 'Snacks', 'Lays', 60.0, 80.0, 120.0, 25.0, 'Pack');
    insertProd.run('Coca Cola Regular 1.5L', '8903216549872', 'SKU-DRINK-007', 'Beverages', 'Coca-Cola', 160.0, 200.0, 75.0, 15.0, 'Bottle');
    insertProd.run('LU Prince Biscuits Half Roll', '8901472583694', 'SKU-BISCUIT-008', 'Bakery', 'Continental', 35.0, 50.0, 95.0, 20.0, 'Pack');
    insertProd.run('Dettol Original Soap 100g', '8909638527415', 'SKU-SOAP-009', 'Personal Care', 'Reckitt', 90.0, 120.0, 50.0, 12.0, 'Bar');
    insertProd.run('Surf Excel Detergent 1 Kg', '8908529631478', 'SKU-SURF-010', 'Household', 'Unilever', 420.0, 500.0, 28.0, 5.0, 'Pouch');

    // Seed customers
    const insertCust = db.prepare(`
      INSERT INTO customers (name, phone, address, credit_limit, current_balance)
      VALUES (?, ?, ?, ?, ?)
    `);
    insertCust.run('Walking Customer', '', 'Store Retail', 0.0, 0.0);
    insertCust.run('Chaudhry Muhammad Tariq', '+92-300-5551122', 'House 42, Street 7, Model Town', 25000.0, 4200.0);
    insertCust.run('Malik Usman Liaquat', '+92-321-4443322', 'Commercial Plaza, Gulberg III', 50000.0, 11500.0);

    // Seed suppliers
    const insertSupp = db.prepare(`
      INSERT INTO suppliers (name, contact_person, phone, address, payable_balance)
      VALUES (?, ?, ?, ?, ?)
    `);
    insertSupp.run('Al-Madina Wholesale Distributors', 'Haji Rasheed', '+92-301-7778899', 'Grain Market, Badami Bagh', 45000.0);
    insertSupp.run('Nestle Pakistan Regional Depot', 'Farhan Sheikh', '+92-333-8889900', 'Ferozepur Road, Lahore', 18500.0);

    console.log('Central Database seeded with default enterprise retail data.');
  }
}

initDb();

// --------------------------------------------------------------------------
// AUTHENTICATION MIDDLEWARE
// --------------------------------------------------------------------------
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Authentication token required' });

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).json({ error: 'Invalid or expired session' });
    req.user = user;
    next();
  });
}

function requireAdmin(req, res, next) {
  if (req.user && (req.user.role === 'SUPER_ADMIN' || req.user.role === 'ADMIN')) {
    next();
  } else {
    res.status(403).json({ error: 'Administrative privileges required' });
  }
}

// --------------------------------------------------------------------------
// API ROUTES
// --------------------------------------------------------------------------

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'UP',
    system: 'CHOUDHURY POS APP -- SHARED CLOUD & ON-PREMISE ENGINE',
    version: '8.0.0',
    timestamp: new Date().toISOString()
  });
});

// Auth: Login
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password required' });
  }

  const user = db.prepare('SELECT * FROM users WHERE username = ? AND is_active = 1').get(username);
  if (!user || !bcrypt.compareSync(password, user.password_hash)) {
    return res.status(401).json({ error: 'Invalid username or password' });
  }

  const store = db.prepare('SELECT * FROM stores WHERE id = ?').get(user.store_id || 1);
  const settings = db.prepare('SELECT * FROM store_settings WHERE store_id = ?').get(user.store_id || 1);

  const token = jwt.sign(
    { id: user.id, username: user.username, role: user.role, store_id: user.store_id },
    JWT_SECRET,
    { expiresIn: '24h' }
  );

  res.json({
    token,
    user: {
      id: user.id,
      username: user.username,
      fullName: user.full_name,
      role: user.role,
      storeId: user.store_id
    },
    store,
    settings
  });
});

// Auth: PIN Quick Login (POS terminals)
app.post('/api/auth/login-pin', (req, res) => {
  const { pin } = req.body;
  if (!pin) return res.status(400).json({ error: 'PIN is required' });

  const user = db.prepare('SELECT * FROM users WHERE pin = ? AND is_active = 1').get(pin);
  if (!user) return res.status(401).json({ error: 'Invalid PIN' });

  const store = db.prepare('SELECT * FROM stores WHERE id = ?').get(user.store_id || 1);
  const settings = db.prepare('SELECT * FROM store_settings WHERE store_id = ?').get(user.store_id || 1);

  const token = jwt.sign(
    { id: user.id, username: user.username, role: user.role, store_id: user.store_id },
    JWT_SECRET,
    { expiresIn: '24h' }
  );

  res.json({
    token,
    user: {
      id: user.id,
      username: user.username,
      fullName: user.full_name,
      role: user.role,
      storeId: user.store_id
    },
    store,
    settings
  });
});

// Auth: Current Session
app.get('/api/auth/me', authenticateToken, (req, res) => {
  const user = db.prepare('SELECT id, username, full_name, role, store_id FROM users WHERE id = ?').get(req.user.id);
  const store = db.prepare('SELECT * FROM stores WHERE id = ?').get(req.user.store_id || 1);
  const settings = db.prepare('SELECT * FROM store_settings WHERE store_id = ?').get(req.user.store_id || 1);
  res.json({ user, store, settings });
});

// Dashboard KPI Stats
app.get('/api/dashboard/stats', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const todayStart = new Date();
  todayStart.setHours(0, 0, 0, 0);
  const todayIso = todayStart.toISOString();

  // Sales Today
  const todaySales = db.prepare(`
    SELECT 
      COUNT(*) as invoiceCount,
      COALESCE(SUM(total_amount), 0.0) as grossSales,
      COALESCE(SUM(paid_amount), 0.0) as cashCollected
    FROM sales 
    WHERE store_id = ? AND status = 'COMPLETED' AND created_at >= ?
  `).get(storeId, todayIso);

  // Profit Today
  const profitRow = db.prepare(`
    SELECT COALESCE(SUM((si.sale_price - si.purchase_price) * si.quantity), 0.0) as netProfit
    FROM sale_items si
    JOIN sales s ON si.sale_id = s.id
    WHERE s.store_id = ? AND s.status = 'COMPLETED' AND s.created_at >= ?
  `).get(storeId, todayIso);

  // Inventory stats
  const invStats = db.prepare(`
    SELECT 
      COUNT(*) as totalProducts,
      COALESCE(SUM(stock_quantity), 0.0) as totalUnits,
      COALESCE(SUM(stock_quantity * purchase_price), 0.0) as inventoryValuation,
      SUM(CASE WHEN stock_quantity <= min_stock_alert THEN 1 ELSE 0 END) as lowStockCount,
      SUM(CASE WHEN stock_quantity <= 0 THEN 1 ELSE 0 END) as outOfStockCount
    FROM products 
    WHERE store_id = ? AND is_active = 1
  `).get(storeId);

  // Active register shift
  const activeShift = db.prepare(`
    SELECT rs.*, u.full_name as cashier_name 
    FROM register_shifts rs
    JOIN users u ON rs.user_id = u.id
    WHERE rs.store_id = ? AND rs.status = 'OPEN'
    ORDER BY rs.id DESC LIMIT 1
  `).get(storeId);

  // Recent 5 sales
  const recentSales = db.prepare(`
    SELECT s.*, c.name as customer_name, u.full_name as cashier_name
    FROM sales s
    LEFT JOIN customers c ON s.customer_id = c.id
    LEFT JOIN users u ON s.user_id = u.id
    WHERE s.store_id = ?
    ORDER BY s.id DESC LIMIT 5
  `).all(storeId);

  res.json({
    todayGrossSales: todaySales.grossSales,
    todayInvoiceCount: todaySales.invoiceCount,
    todayNetProfit: profitRow.netProfit,
    todayCashCollected: todaySales.cashCollected,
    totalProducts: invStats.totalProducts,
    totalUnits: invStats.totalUnits,
    inventoryValuation: invStats.inventoryValuation,
    lowStockCount: invStats.lowStockCount,
    outOfStockCount: invStats.outOfStockCount,
    activeShift,
    recentSales
  });
});

// Products: List & Search
app.get('/api/products', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const { query, category, lowStock } = req.query;

  let sql = 'SELECT * FROM products WHERE store_id = ? AND is_active = 1';
  const params = [storeId];

  if (category && category !== 'All') {
    sql += ' AND category = ?';
    params.push(category);
  }

  if (lowStock === 'true') {
    sql += ' AND stock_quantity <= min_stock_alert';
  }

  if (query) {
    sql += ' AND (name LIKE ? OR barcode LIKE ? OR sku LIKE ?)';
    const term = `%${query.trim()}%`;
    params.push(term, term, term);
  }

  sql += ' ORDER BY name ASC';
  const products = db.prepare(sql).all(...params);
  res.json(products);
});

// Products: Lookup by exact barcode / SKU
app.get('/api/products/barcode/:barcode', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const barcode = req.params.barcode.trim();

  const product = db.prepare(`
    SELECT * FROM products 
    WHERE store_id = ? AND is_active = 1 AND (barcode = ? OR sku = ?)
  `).get(storeId, barcode, barcode);

  if (!product) {
    return res.status(404).json({ error: `Product with barcode '${barcode}' not found` });
  }

  res.json(product);
});

// Products: Generate Next Master Barcode (200-prefix sequential with Mod10 check digit)
app.get('/api/products/generate-barcode', authenticateToken, (req, res) => {
  const maxRow = db.prepare(`
    SELECT barcode FROM products 
    WHERE barcode LIKE '200%' AND length(barcode) = 13 
    ORDER BY barcode DESC LIMIT 1
  `).get();

  let nextSeq = 1;
  if (maxRow && maxRow.barcode) {
    const rawSeq = parseInt(maxRow.barcode.substring(3, 12), 10);
    if (!isNaN(rawSeq)) {
      nextSeq = rawSeq + 1;
    }
  }

  const padded = String(nextSeq).padStart(9, '0');
  const payload = '200' + padded;

  // EAN-13 Mod 10 Check Digit calculation
  let sum = 0;
  for (let i = 0; i < 12; i++) {
    const digit = parseInt(payload[i], 10);
    sum += (i % 2 === 0) ? digit * 1 : digit * 3;
  }
  const checkDigit = (10 - (sum % 10)) % 10;
  const masterBarcode = payload + checkDigit;

  res.json({ barcode: masterBarcode, sequence: nextSeq });
});

// Products: Create Product
app.post('/api/products', authenticateToken, requireAdmin, (req, res) => {
  const storeId = req.user.store_id || 1;
  const {
    name, barcode, sku, category, brand,
    purchase_price, sale_price, stock_quantity,
    min_stock_alert, unit, tax_percent
  } = req.body;

  if (!name || !barcode || !sale_price) {
    return res.status(400).json({ error: 'Product name, barcode, and sale price are required' });
  }

  const existing = db.prepare('SELECT id FROM products WHERE barcode = ?').get(barcode.trim());
  if (existing) {
    return res.status(400).json({ error: `Barcode '${barcode}' already exists in catalog` });
  }

  const stmt = db.prepare(`
    INSERT INTO products (
      name, barcode, sku, category, brand,
      purchase_price, sale_price, stock_quantity,
      min_stock_alert, unit, tax_percent, store_id
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  const info = stmt.run(
    name.trim(),
    barcode.trim(),
    sku ? sku.trim() : null,
    category || 'General',
    brand || 'General',
    parseFloat(purchase_price) || 0.0,
    parseFloat(sale_price),
    parseFloat(stock_quantity) || 0.0,
    parseFloat(min_stock_alert) || 5.0,
    unit || 'Pcs',
    parseFloat(tax_percent) || 0.0,
    storeId
  );

  const created = db.prepare('SELECT * FROM products WHERE id = ?').get(info.lastInsertRowid);
  res.status(201).json(created);
});

// Products: Update Product
app.put('/api/products/:id', authenticateToken, requireAdmin, (req, res) => {
  const id = parseInt(req.params.id, 10);
  const {
    name, barcode, sku, category, brand,
    purchase_price, sale_price, stock_quantity,
    min_stock_alert, unit, tax_percent
  } = req.body;

  const existing = db.prepare('SELECT id FROM products WHERE barcode = ? AND id != ?').get(barcode.trim(), id);
  if (existing) {
    return res.status(400).json({ error: `Barcode '${barcode}' is already in use by another product` });
  }

  db.prepare(`
    UPDATE products SET
      name = ?, barcode = ?, sku = ?, category = ?, brand = ?,
      purchase_price = ?, sale_price = ?, stock_quantity = ?,
      min_stock_alert = ?, unit = ?, tax_percent = ?, updated_at = CURRENT_TIMESTAMP
    WHERE id = ?
  `).run(
    name.trim(),
    barcode.trim(),
    sku ? sku.trim() : null,
    category || 'General',
    brand || 'General',
    parseFloat(purchase_price) || 0.0,
    parseFloat(sale_price),
    parseFloat(stock_quantity) || 0.0,
    parseFloat(min_stock_alert) || 5.0,
    unit || 'Pcs',
    parseFloat(tax_percent) || 0.0,
    id
  );

  const updated = db.prepare('SELECT * FROM products WHERE id = ?').get(id);
  res.json(updated);
});

// Products: Delete / Archive Product
app.delete('/api/products/:id', authenticateToken, requireAdmin, (req, res) => {
  const id = parseInt(req.params.id, 10);
  db.prepare('UPDATE products SET is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?').run(id);
  res.json({ success: true, message: 'Product archived successfully' });
});

// POS / Sales: Complete Checkout Transaction (Atomic Financial Transaction)
app.post('/api/sales', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const userId = req.user.id;
  const {
    items, customer_id, discount_amount, tax_amount,
    paid_amount, payment_method, notes
  } = req.body;

  if (!items || !items.length) {
    return res.status(400).json({ error: 'Cart cannot be empty' });
  }

  // Calculate totals safely
  let subtotal = 0.0;
  for (const item of items) {
    subtotal += item.sale_price * item.quantity;
  }

  const discount = parseFloat(discount_amount) || 0.0;
  const tax = parseFloat(tax_amount) || 0.0;
  const total = Math.max(0.0, subtotal - discount + tax);
  const paid = parseFloat(paid_amount) || 0.0;
  const change = Math.max(0.0, paid - total);

  // Active shift check
  const activeShift = db.prepare("SELECT id FROM register_shifts WHERE store_id = ? AND status = 'OPEN' ORDER BY id DESC LIMIT 1").get(storeId);
  const shiftId = activeShift ? activeShift.id : null;

  // Generate unique sequential invoice number (e.g. INV-2026-0001)
  const countRow = db.prepare('SELECT count(*) as cnt FROM sales').get();
  const nextNum = (countRow.cnt + 1).toString().padStart(6, '0');
  const invoiceNo = `INV-${new Date().getFullYear()}-${nextNum}`;

  // Atomic database transaction
  const checkoutTx = db.transaction(() => {
    // 1. Insert Sales Record
    const saleInfo = db.prepare(`
      INSERT INTO sales (
        invoice_no, shift_id, store_id, user_id, customer_id,
        subtotal, discount_amount, tax_amount, total_amount, paid_amount,
        change_due, payment_method, status, notes, sync_origin, sync_status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?, 'WEB', 'SYNCED')
    `).run(
      invoiceNo, shiftId, storeId, userId, customer_id || null,
      subtotal, discount, tax, total, paid,
      change, payment_method || 'CASH', notes || null
    );

    const saleId = saleInfo.lastInsertRowid;

    // 2. Insert Sale Items & Deduct Inventory Stock
    const insertItem = db.prepare(`
      INSERT INTO sale_items (
        sale_id, product_id, product_name, barcode, quantity,
        sale_price, purchase_price, discount_amount, total_price
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    const updateStock = db.prepare(`
      UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?
    `);

    for (const item of items) {
      const prod = db.prepare('SELECT purchase_price FROM products WHERE id = ?').get(item.product_id);
      const purchasePrice = prod ? prod.purchase_price : 0.0;
      const itemTotal = item.sale_price * item.quantity;

      insertItem.run(
        saleId, item.product_id, item.name, item.barcode || '',
        item.quantity, item.sale_price, purchasePrice, 0.0, itemTotal
      );

      updateStock.run(item.quantity, item.product_id);
    }

    // 3. Customer Ledger for Credit / Udhar sales
    if (customer_id && customer_id > 1) {
      const due = Math.max(0.0, total - paid);
      if (due > 0 || payment_method === 'CREDIT') {
        const cust = db.prepare('SELECT current_balance FROM customers WHERE id = ?').get(customer_id);
        const newBal = (cust ? cust.current_balance : 0.0) + due;

        db.prepare('UPDATE customers SET current_balance = ? WHERE id = ?').run(newBal, customer_id);

        db.prepare(`
          INSERT INTO customer_ledger (
            customer_id, transaction_type, invoice_no, debit, credit, balance_after, notes
          ) VALUES (?, 'SALE_CREDIT', ?, ?, 0.0, ?, ?)
        `).run(customer_id, invoiceNo, due, newBal, `Credit on Invoice ${invoiceNo}`);
      }
    }

    return saleId;
  });

  const saleId = checkoutTx();

  const sale = db.prepare(`
    SELECT s.*, c.name as customer_name, u.full_name as cashier_name
    FROM sales s
    LEFT JOIN customers c ON s.customer_id = c.id
    LEFT JOIN users u ON s.user_id = u.id
    WHERE s.id = ?
  `).get(saleId);

  const saleItems = db.prepare('SELECT * FROM sale_items WHERE sale_id = ?').all(saleId);
  const settings = db.prepare('SELECT * FROM store_settings WHERE store_id = ?').get(storeId);

  res.status(201).json({
    success: true,
    sale,
    items: saleItems,
    settings
  });
});

// Invoices: List Invoices
app.get('/api/invoices', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const { query, limit = 50 } = req.query;

  let sql = `
    SELECT s.*, c.name as customer_name, u.full_name as cashier_name
    FROM sales s
    LEFT JOIN customers c ON s.customer_id = c.id
    LEFT JOIN users u ON s.user_id = u.id
    WHERE s.store_id = ?
  `;
  const params = [storeId];

  if (query) {
    sql += ' AND (s.invoice_no LIKE ? OR c.name LIKE ?)';
    const term = `%${query.trim()}%`;
    params.push(term, term);
  }

  sql += ' ORDER BY s.id DESC LIMIT ?';
  params.push(parseInt(limit, 10));

  const invoices = db.prepare(sql).all(...params);
  res.json(invoices);
});

// Invoices: Single Invoice with Items
app.get('/api/invoices/:id', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const sale = db.prepare(`
    SELECT s.*, c.name as customer_name, c.phone as customer_phone, u.full_name as cashier_name
    FROM sales s
    LEFT JOIN customers c ON s.customer_id = c.id
    LEFT JOIN users u ON s.user_id = u.id
    WHERE (s.id = ? OR s.invoice_no = ?) AND s.store_id = ?
  `).get(req.params.id, req.params.id, storeId);

  if (!sale) return res.status(404).json({ error: 'Invoice not found' });

  const items = db.prepare('SELECT * FROM sale_items WHERE sale_id = ?').all(sale.id);
  const settings = db.prepare('SELECT * FROM store_settings WHERE store_id = ?').get(storeId);

  res.json({ sale, items, settings });
});

// Sales Returns: Process Return & Restock
app.post('/api/returns', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const { invoice_no, items, reason, restock } = req.body;

  if (!invoice_no || !items || !items.length) {
    return res.status(400).json({ error: 'Invoice number and return items are required' });
  }

  const sale = db.prepare('SELECT id FROM sales WHERE invoice_no = ?').get(invoice_no);
  if (!sale) return res.status(404).json({ error: 'Original invoice not found' });

  let refundTotal = 0.0;
  for (const it of items) {
    refundTotal += it.refund_price * it.quantity;
  }

  const countRow = db.prepare('SELECT count(*) as cnt FROM sale_returns').get();
  const returnNo = `RET-${new Date().getFullYear()}-${(countRow.cnt + 1).toString().padStart(5, '0')}`;

  const returnTx = db.transaction(() => {
    const returnInfo = db.prepare(`
      INSERT INTO sale_returns (return_no, sale_id, invoice_no, store_id, refund_amount, reason, restocked)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(returnNo, sale.id, invoice_no, storeId, refundTotal, reason || 'Customer Return', restock ? 1 : 0);

    const returnId = returnInfo.lastInsertRowid;

    const insertItem = db.prepare(`
      INSERT INTO sale_return_items (return_id, product_id, quantity, refund_price, total_refund)
      VALUES (?, ?, ?, ?, ?)
    `);

    const restockStmt = db.prepare('UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?');

    for (const it of items) {
      insertItem.run(returnId, it.product_id, it.quantity, it.refund_price, it.refund_price * it.quantity);
      if (restock) {
        restockStmt.run(it.quantity, it.product_id);
      }
    }

    return returnId;
  });

  const returnId = returnTx();
  res.json({ success: true, returnNo, refundAmount: refundTotal });
});

// Customers: List & Search
app.get('/api/customers', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const { query } = req.query;

  let sql = 'SELECT * FROM customers WHERE store_id = ?';
  const params = [storeId];
  if (query) {
    sql += ' AND (name LIKE ? OR phone LIKE ?)';
    const term = `%${query.trim()}%`;
    params.push(term, term);
  }
  sql += ' ORDER BY name ASC';
  res.json(db.prepare(sql).all(...params));
});

// Customers: Ledger
app.get('/api/customers/:id/ledger', authenticateToken, (req, res) => {
  const customerId = parseInt(req.params.id, 10);
  const customer = db.prepare('SELECT * FROM customers WHERE id = ?').get(customerId);
  if (!customer) return res.status(404).json({ error: 'Customer not found' });

  const ledger = db.prepare('SELECT * FROM customer_ledger WHERE customer_id = ? ORDER BY id DESC').all(customerId);
  res.json({ customer, ledger });
});

// Customers: Record Payment
app.post('/api/customers/:id/payment', authenticateToken, (req, res) => {
  const customerId = parseInt(req.params.id, 10);
  const { amount, notes } = req.body;
  const payAmt = parseFloat(amount);

  if (!payAmt || payAmt <= 0) return res.status(400).json({ error: 'Valid payment amount required' });

  const customer = db.prepare('SELECT current_balance FROM customers WHERE id = ?').get(customerId);
  if (!customer) return res.status(404).json({ error: 'Customer not found' });

  const newBal = customer.current_balance - payAmt;

  const paymentTx = db.transaction(() => {
    db.prepare('UPDATE customers SET current_balance = ? WHERE id = ?').run(newBal, customerId);
    db.prepare(`
      INSERT INTO customer_ledger (customer_id, transaction_type, debit, credit, balance_after, notes)
      VALUES (?, 'PAYMENT_RECEIVED', 0.0, ?, ?, ?)
    `).run(customerId, payAmt, newBal, notes || 'Customer Cash/Online Payment');
  });

  paymentTx();
  res.json({ success: true, newBalance: newBal });
});

// Suppliers: List
app.get('/api/suppliers', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  res.json(db.prepare('SELECT * FROM suppliers WHERE store_id = ? ORDER BY name ASC').all(storeId));
});

// Purchases: List & Add Purchase
app.get('/api/purchases', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const purchases = db.prepare(`
    SELECT p.*, s.name as supplier_name 
    FROM purchases p
    LEFT JOIN suppliers s ON p.supplier_id = s.id
    WHERE p.store_id = ? ORDER BY p.id DESC
  `).all(storeId);
  res.json(purchases);
});

app.post('/api/purchases', authenticateToken, requireAdmin, (req, res) => {
  const storeId = req.user.store_id || 1;
  const { supplier_id, items, paid_amount, payment_method, notes } = req.body;

  if (!items || !items.length) return res.status(400).json({ error: 'Items required for purchase' });

  let totalAmount = 0.0;
  for (const it of items) {
    totalAmount += it.unit_cost * it.quantity;
  }

  const countRow = db.prepare('SELECT count(*) as cnt FROM purchases').get();
  const purchaseNo = `PO-${new Date().getFullYear()}-${(countRow.cnt + 1).toString().padStart(5, '0')}`;

  const purchaseTx = db.transaction(() => {
    const pInfo = db.prepare(`
      INSERT INTO purchases (purchase_no, supplier_id, store_id, total_amount, paid_amount, payment_method, notes)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(purchaseNo, supplier_id || null, storeId, totalAmount, paid_amount || 0.0, payment_method || 'CASH', notes || null);

    const purchaseId = pInfo.lastInsertRowid;

    const insertItem = db.prepare(`
      INSERT INTO purchase_items (purchase_id, product_id, quantity, unit_cost, total_cost)
      VALUES (?, ?, ?, ?, ?)
    `);

    const updateStock = db.prepare(`
      UPDATE products SET 
        stock_quantity = stock_quantity + ?,
        purchase_price = ?,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `);

    for (const it of items) {
      insertItem.run(purchaseId, it.product_id, it.quantity, it.unit_cost, it.unit_cost * it.quantity);
      updateStock.run(it.quantity, it.unit_cost, it.product_id);
    }

    // Update supplier ledger
    if (supplier_id) {
      const supp = db.prepare('SELECT payable_balance FROM suppliers WHERE id = ?').get(supplier_id);
      const remainingDue = totalAmount - (parseFloat(paid_amount) || 0.0);
      const newPayable = (supp ? supp.payable_balance : 0.0) + remainingDue;

      db.prepare('UPDATE suppliers SET payable_balance = ? WHERE id = ?').run(newPayable, supplier_id);
      db.prepare(`
        INSERT INTO supplier_ledger (supplier_id, transaction_type, reference_no, credit, debit, balance_after, notes)
        VALUES (?, 'PURCHASE', ?, ?, ?, ?, ?)
      `).run(supplier_id, purchaseNo, totalAmount, paid_amount || 0.0, newPayable, `Stock Inward ${purchaseNo}`);
    }

    return purchaseId;
  });

  const purchaseId = purchaseTx();
  res.status(201).json({ success: true, purchaseNo, totalAmount });
});

// Expenses: List & Create
app.get('/api/expenses', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  res.json(db.prepare('SELECT * FROM expenses WHERE store_id = ? ORDER BY id DESC').all(storeId));
});

app.post('/api/expenses', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const { category, amount, payment_method, recipient, description } = req.body;
  const expAmt = parseFloat(amount);

  if (!category || !expAmt || expAmt <= 0) {
    return res.status(400).json({ error: 'Valid category and amount required' });
  }

  const info = db.prepare(`
    INSERT INTO expenses (category, amount, payment_method, recipient, description, store_id)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(category, expAmt, payment_method || 'CASH', recipient || null, description || null, storeId);

  res.status(201).json({ success: true, id: info.lastInsertRowid });
});

// Register Shifts & Daily Closing
app.get('/api/shifts/active', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const shift = db.prepare(`
    SELECT rs.*, u.full_name as cashier_name
    FROM register_shifts rs
    JOIN users u ON rs.user_id = u.id
    WHERE rs.store_id = ? AND rs.status = 'OPEN'
    ORDER BY rs.id DESC LIMIT 1
  `).get(storeId);
  res.json(shift || null);
});

app.post('/api/shifts/open', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const userId = req.user.id;
  const { opening_cash, notes } = req.body;

  const existing = db.prepare("SELECT id FROM register_shifts WHERE store_id = ? AND status = 'OPEN'").get(storeId);
  if (existing) return res.status(400).json({ error: 'A shift is already open for this store' });

  const info = db.prepare(`
    INSERT INTO register_shifts (user_id, store_id, opening_cash, status, notes)
    VALUES (?, ?, ?, 'OPEN', ?)
  `).run(userId, storeId, parseFloat(opening_cash) || 0.0, notes || null);

  res.status(201).json({ success: true, shiftId: info.lastInsertRowid });
});

app.post('/api/shifts/close', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const { actual_cash, notes } = req.body;

  const activeShift = db.prepare("SELECT * FROM register_shifts WHERE store_id = ? AND status = 'OPEN' ORDER BY id DESC LIMIT 1").get(storeId);
  if (!activeShift) return res.status(400).json({ error: 'No active shift found to close' });

  // Calculate sales cash collected during this shift
  const cashSales = db.prepare(`
    SELECT COALESCE(SUM(paid_amount - change_due), 0.0) as totalCash
    FROM sales 
    WHERE shift_id = ? AND payment_method = 'CASH' AND status = 'COMPLETED'
  `).get(activeShift.id).totalCash;

  const expectedCash = activeShift.opening_cash + cashSales;
  const actualCash = parseFloat(actual_cash) || 0.0;
  const diff = actualCash - expectedCash; // Positive = excess, Negative = shortage

  db.prepare(`
    UPDATE register_shifts SET
      closed_at = CURRENT_TIMESTAMP,
      closing_cash_actual = ?,
      expected_cash = ?,
      cash_shortage_excess = ?,
      status = 'CLOSED',
      notes = ?
    WHERE id = ?
  `).run(actualCash, expectedCash, diff, notes || null, activeShift.id);

  res.json({
    success: true,
    openingCash: activeShift.opening_cash,
    cashSales,
    expectedCash,
    actualCash,
    cashShortageExcess: diff
  });
});

// Reports & Financial Analytics
app.get('/api/reports/summary', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;

  const totalGross = db.prepare('SELECT COALESCE(SUM(total_amount), 0.0) as gross FROM sales WHERE store_id = ? AND status = "COMPLETED"').get(storeId).gross;
  const totalInvoices = db.prepare('SELECT count(*) as count FROM sales WHERE store_id = ? AND status = "COMPLETED"').get(storeId).count;
  const totalExpenses = db.prepare('SELECT COALESCE(SUM(amount), 0.0) as exp FROM expenses WHERE store_id = ?').get(storeId).exp;

  const totalProfit = db.prepare(`
    SELECT COALESCE(SUM((si.sale_price - si.purchase_price) * si.quantity), 0.0) as profit
    FROM sale_items si
    JOIN sales s ON si.sale_id = s.id
    WHERE s.store_id = ? AND s.status = 'COMPLETED'
  `).get(storeId).profit;

  const netPnl = totalProfit - totalExpenses;

  // Payment Breakdown
  const payments = db.prepare(`
    SELECT payment_method, COUNT(*) as tx_count, COALESCE(SUM(total_amount), 0.0) as total
    FROM sales WHERE store_id = ? AND status = 'COMPLETED'
    GROUP BY payment_method
  `).all(storeId);

  // Top 5 Selling Products
  const topProducts = db.prepare(`
    SELECT product_name, SUM(quantity) as units_sold, SUM(total_price) as revenue
    FROM sale_items si
    JOIN sales s ON si.sale_id = s.id
    WHERE s.store_id = ? AND s.status = 'COMPLETED'
    GROUP BY product_id ORDER BY units_sold DESC LIMIT 5
  `).all(storeId);

  res.json({
    totalGross,
    totalInvoices,
    totalProfit,
    totalExpenses,
    netPnl,
    payments,
    topProducts
  });
});

// Store Settings
app.get('/api/settings', authenticateToken, (req, res) => {
  const storeId = req.user.store_id || 1;
  const settings = db.prepare('SELECT * FROM store_settings WHERE store_id = ?').get(storeId);
  res.json(settings || {});
});

app.put('/api/settings', authenticateToken, requireAdmin, (req, res) => {
  const storeId = req.user.store_id || 1;
  const {
    store_name, tagline, address, phone, email, tax_number,
    currency_symbol, default_tax_percent, receipt_paper_size,
    receipt_header, receipt_footer, sound_enabled, dark_mode
  } = req.body;

  db.prepare(`
    UPDATE store_settings SET
      store_name = ?, tagline = ?, address = ?, phone = ?, email = ?, tax_number = ?,
      currency_symbol = ?, default_tax_percent = ?, receipt_paper_size = ?,
      receipt_header = ?, receipt_footer = ?, sound_enabled = ?, dark_mode = ?
    WHERE store_id = ?
  `).run(
    store_name, tagline, address, phone, email, tax_number,
    currency_symbol || 'Rs', parseFloat(default_tax_percent) || 0.0, receipt_paper_size || '80mm',
    receipt_header, receipt_footer, sound_enabled ? 1 : 0, dark_mode ? 1 : 0,
    storeId
  );

  res.json({ success: true, settings: db.prepare('SELECT * FROM store_settings WHERE store_id = ?').get(storeId) });
});

// Users / Staff Management
app.get('/api/users', authenticateToken, requireAdmin, (req, res) => {
  res.json(db.prepare('SELECT id, username, full_name, role, store_id, pin, is_active FROM users').all());
});

app.post('/api/users', authenticateToken, requireAdmin, (req, res) => {
  const { username, password, full_name, role, pin, store_id } = req.body;
  if (!username || !password || !full_name) {
    return res.status(400).json({ error: 'Username, password, and full name required' });
  }

  const existing = db.prepare('SELECT id FROM users WHERE username = ?').get(username.trim());
  if (existing) return res.status(400).json({ error: 'Username already in use' });

  const hash = bcrypt.hashSync(password, 10);
  const info = db.prepare(`
    INSERT INTO users (username, password_hash, full_name, role, pin, store_id)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(username.trim(), hash, full_name.trim(), role || 'CASHIER', pin || null, store_id || 1);

  res.status(201).json({ success: true, id: info.lastInsertRowid });
});

// --------------------------------------------------------------------------
// TWO-WAY SYNCHRONIZATION API (For Android App & Offline Web Clients)
// --------------------------------------------------------------------------

// Pull updates created or updated since checkpoint timestamp
app.get('/api/sync/pull', authenticateToken, (req, res) => {
  const { since } = req.query;
  const sinceTime = since || '1970-01-01T00:00:00.000Z';

  const products = db.prepare('SELECT * FROM products WHERE updated_at >= ?').all(sinceTime);
  const sales = db.prepare('SELECT * FROM sales WHERE created_at >= ?').all(sinceTime);
  const customers = db.prepare('SELECT * FROM customers WHERE created_at >= ?').all(sinceTime);
  const suppliers = db.prepare('SELECT * FROM suppliers WHERE created_at >= ?').all(sinceTime);
  const serverTime = new Date().toISOString();

  res.json({
    serverTime,
    products,
    sales,
    customers,
    suppliers
  });
});

// Push client transactions and entities to central server
app.post('/api/sync/push', authenticateToken, (req, res) => {
  const { device_id, sales, products, customers } = req.body;
  const storeId = req.user.store_id || 1;

  let insertedSales = 0;
  let updatedProducts = 0;

  const syncTx = db.transaction(() => {
    // 1. Process offline-created sales with Idempotency check on invoice_no
    if (sales && sales.length) {
      for (const s of sales) {
        const existing = db.prepare('SELECT id FROM sales WHERE invoice_no = ?').get(s.invoice_no);
        if (!existing) {
          const sInfo = db.prepare(`
            INSERT INTO sales (
              invoice_no, store_id, user_id, customer_id,
              subtotal, discount_amount, tax_amount, total_amount, paid_amount,
              change_due, payment_method, status, notes, sync_origin, sync_status, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ANDROID', 'SYNCED', ?)
          `).run(
            s.invoice_no, storeId, req.user.id, s.customer_id || null,
            s.subtotal, s.discount_amount || 0.0, s.tax_amount || 0.0, s.total_amount, s.paid_amount,
            s.change_due || 0.0, s.payment_method || 'CASH', s.status || 'COMPLETED',
            s.notes || null, s.created_at || new Date().toISOString()
          );

          insertedSales++;

          if (s.items && s.items.length) {
            const insertItem = db.prepare(`
              INSERT INTO sale_items (sale_id, product_id, product_name, barcode, quantity, sale_price, purchase_price, total_price)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            `);
            const deduct = db.prepare('UPDATE products SET stock_quantity = stock_quantity - ? WHERE barcode = ?');

            for (const it of s.items) {
              insertItem.run(sInfo.lastInsertRowid, it.product_id || 1, it.product_name, it.barcode, it.quantity, it.sale_price, it.purchase_price || 0.0, it.sale_price * it.quantity);
              deduct.run(it.quantity, it.barcode);
            }
          }
        }
      }
    }

    // 2. Process products created on client
    if (products && products.length) {
      for (const p of products) {
        const existing = db.prepare('SELECT id FROM products WHERE barcode = ?').get(p.barcode);
        if (!existing) {
          db.prepare(`
            INSERT INTO products (name, barcode, sku, category, brand, purchase_price, sale_price, stock_quantity, unit, store_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `).run(p.name, p.barcode, p.sku || null, p.category || 'General', p.brand || 'General', p.purchase_price || 0.0, p.sale_price, p.stock_quantity || 0.0, p.unit || 'Pcs', storeId);
          updatedProducts++;
        }
      }
    }

    // Record checkpoint
    db.prepare(`
      INSERT INTO sync_checkpoints (device_id, records_synced, direction)
      VALUES (?, ?, 'PUSH')
    `).run(device_id || 'UNKNOWN-CLIENT', insertedSales + updatedProducts);
  });

  syncTx();

  res.json({
    success: true,
    serverTime: new Date().toISOString(),
    insertedSales,
    updatedProducts
  });
});

// Fallback to SPA index.html for frontend routing
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`================================================================`);
  console.log(`CHOUDHURY POS APP -- REAL WEB APPLICATION & CENTRAL SYNC SERVER`);
  console.log(`Server actively running on http://0.0.0.0:${PORT}`);
  console.log(`Database initialized: ${DB_PATH}`);
  console.log(`================================================================`);
});
