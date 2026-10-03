# CHOUDHURY POS — PERMANENT PRODUCTION DEPLOYMENT GUIDE

This document provides complete instructions for deploying the **CHOUDHURY POS** Web Application and API Backend to a permanent public production environment with custom domain, HTTPS, persistent database storage, and shared authentication with the Android POS app.

---

## 1. What Is Ready Now vs. What Requires Your Hosting Account

### Ready & Verified in this Codebase:
1. **Zero-Dependency Production Web Engine:** Powered by Node.js v22 with built-in `node:sqlite` (`DatabaseSync`), `http`, and RFC 7519 JWT auth. No NPM compilation or external binary dependencies required.
2. **Persistent Database Architecture:** Fully structured SQLite database supporting multi-store isolation, products, customers, suppliers, invoices, payments, and ledger.
3. **Shared User Accounts:** Unified authentication (`/api/auth/register`, `/api/auth/login`, `/api/auth/change-password`, `/api/auth/recover-account`) shared between Web and Android.
4. **Real Bidirectional Sync:** `/api/sync/push` and `/api/sync/pull` APIs with idempotency and duplicate prevention.
5. **Installable PWA:** Complete with `manifest.json`, `sw.js` offline cache service worker, and icons.
6. **Web-to-Android App Linking:** Native deep link URI (`choudhurypos://app/open`) configured in both Android manifest and Web UI.
7. **Direct Binary Downloads:** `/downloads/choudhury-pos-app.apk` and `/downloads/choudhury-pos-windows-x64.zip` served directly with verified MIME headers.
8. **Store Commercial Calculator:** Fully tested in both Web interface and Android Kotlin codebase.

### What Requires Your Hosting Account & Domain:
* **Custom Domain Name (e.g. `pos.yourdomain.com`):** Requires purchasing a domain from any domain registrar (Namecheap, GoDaddy, Cloudflare, Google Domains).
* **Cloud Hosting Account & Billing Authorization:** A server instance or container runner (e.g. DigitalOcean, Hetzner, AWS EC2, Render, Railway, Google Cloud Run).
* **DNS A Record:** Pointing `pos.yourdomain.com` to your server's Public IP address.
* **SSL Certificate:** Free automatic certificate provisioned via Let's Encrypt (Certbot / Caddy).

---

## 2. Option A: Deployment with Docker Compose (Recommended for VPS / Cloud VM)

Recommended providers: DigitalOcean Droplet ($6/mo), Hetzner ($4/mo), AWS Lightsail ($5/mo), Linode.

### Step 1: Provision Server
Launch an Ubuntu 22.04 or 24.04 VM. SSH into your server:
```bash
ssh root@your-server-ip
```

### Step 2: Install Docker & Docker Compose
```bash
apt-get update && apt-get install -y docker.io docker-compose git
systemctl enable --now docker
```

### Step 3: Copy Code & Start Container
Upload the `web/` directory to `/opt/choudhury-pos` on your server:
```bash
cd /opt/choudhury-pos
docker-compose up -d --build
```

### Step 4: Configure Nginx Reverse Proxy with HTTPS (Let's Encrypt)
Install Nginx and Certbot:
```bash
apt-get install -y nginx certbot python3-certbot-nginx
```

Create `/etc/nginx/sites-available/choudhury-pos`:
```nginx
server {
    server_name pos.yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable site and issue free SSL certificate:
```bash
ln -s /etc/nginx/sites-available/choudhury-pos /etc/nginx/sites-enabled/
certbot --nginx -d pos.yourdomain.com
nginx -t && systemctl reload nginx
```

Your production website is now live at `https://pos.yourdomain.com`!

---

## 3. Option B: Deployment to Render / Railway / Fly.io

1. **GitHub Repository:** Push this repository to GitHub.
2. **New Web Service:** In Render / Railway, select "New Web Service" and link your repository.
3. **Root Directory:** Set root directory to `web`.
4. **Build Command:** None required (zero-dependency).
5. **Start Command:** `node server.js`
6. **Environment Variables:**
   * `NODE_ENV`: `production`
   * `APP_PORT`: `3000` (or `PORT` provided by platform)
   * `DB_PATH`: `/data/pos_central.db` (attach persistent disk)
   * `JWT_SECRET`: Generate a secure random string.
7. **Custom Domain:** Add `pos.yourdomain.com` in Render/Railway dashboard and add the provided CNAME record in your domain DNS manager.

---

## 4. Connecting Android App to Your Permanent Production URL

In `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "DEVELOPER_SERVER_URL", "\"https://pos.yourdomain.com/\"")
```
When you rebuild the Android app, all cloud synchronization, user login, and invoice streaming will automatically connect to your permanent server.
