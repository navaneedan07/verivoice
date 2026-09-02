# Environment Variables Guide for VeriVoice

## Backend Environment Variables (Spring Boot)

All environment variables are prefixed with `SPRING_` and use the property naming convention.

### Database Configuration (Required for Production)

```bash
SPRING_DATASOURCE_URL=postgresql://user:password@host:5432/verivoice
SPRING_DATASOURCE_USERNAME=verivoice
SPRING_DATASOURCE_PASSWORD=<secure_password>
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

### Groq AI Configuration (Required)

```bash
GROQ_API_KEY=<your_groq_api_key>
GROQ_MODEL=meta-llama/llama-4-scout-17b-16e-instruct
```

**Get your API key:**
1. Go to https://console.groq.com
2. Sign up or log in
3. Create API key
4. Copy and store securely

### Application Configuration

```bash
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/
```

### JPA/Hibernate Configuration

```bash
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQL15Dialect
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_JPA_SHOW_SQL=false
```

### Logging Configuration

```bash
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_VERIVOICE=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=WARN
LOGGING_LEVEL_ORG_HIBERNATE=WARN
```

### Connection Pool Configuration

```bash
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=2
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000
SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=600000
```

---

## Frontend Environment Variables (Vite)

Frontend variables must be prefixed with `VITE_` to be exposed to the client.

### API Configuration (Required)

```bash
VITE_API_URL=https://verivoice-backend.onrender.com
```

**Examples:**
- Local development: `http://localhost:8080`
- Production: `https://verivoice-backend.onrender.com`

---

## Vercel Environment Variable Setup

### Step-by-Step

1. **Go to Vercel Dashboard**
   - Select your VeriVoice project
   - Settings → Environment Variables

2. **Add Variables**
   ```
   Name: VITE_API_URL
   Value: https://verivoice-backend.onrender.com
   Environments: Production (can also select Preview/Development)
   ```

3. **Redeploy After Changes**
   - Changes take effect on next deployment
   - Deployments → Latest → "Redeploy"

### Vercel Production vs Preview

- **Production**: Used for main branch deployments
- **Preview**: Used for pull requests and preview branches
- **Development**: Used locally

---

## Render Environment Variable Setup

### Step-by-Step

1. **Go to Render Dashboard**
   - Select VeriVoice Backend service
   - Settings → Environment

2. **Add Variables in Render**
   ```
   Key: SPRING_DATASOURCE_URL
   Value: postgresql://verivoice:PASSWORD@dpg-xxxxx.postgres.render.com:5432/verivoice
   
   Key: SPRING_DATASOURCE_USERNAME
   Value: verivoice
   
   Key: SPRING_DATASOURCE_PASSWORD
   Value: <secure_password_from_postgres_database>
   
   Key: GROQ_API_KEY
   Value: gsk_xxxxxxxxxxxxx
   
   Key: SPRING_PROFILES_ACTIVE
   Value: production
   ```

3. **Redeploy After Changes**
   - Manual Redeploy button in Service dashboard

---

## Local Development Setup

### Create `.env.local` in server/

```bash
# Database (local H2 or PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=

# Groq API
GROQ_API_KEY=<your_groq_api_key>
GROQ_MODEL=meta-llama/llama-4-scout-17b-16e-instruct

# Profile
SPRING_PROFILES_ACTIVE=development
```

### Create `.env.local` in frontend/

```bash
VITE_API_URL=http://localhost:8080
```

### Run Locally

**Backend:**
```bash
cd server
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm run dev
```

---

## Security Checklist

### ✅ Before Production

1. **Groq API Key**
   - [ ] Stored securely in Render environment variables
   - [ ] Not committed to git (add to .gitignore if needed)
   - [ ] Rotated periodically

2. **Database Credentials**
   - [ ] Strong password (min 16 characters, mixed case, numbers, symbols)
   - [ ] Stored in Render environment variables
   - [ ] Never in code or .env files
   - [ ] Use Render's managed database for security

3. **HTTPS Only**
   - [ ] Vercel enforces HTTPS by default ✓
   - [ ] Render enforces HTTPS by default ✓
   - [ ] All API calls use https://

4. **Sensitive Data**
   - [ ] No API keys in logs
   - [ ] No passwords in error messages
   - [ ] CORS restricted to known origins (if needed)

---

## Troubleshooting Environment Variables

### Variable Not Being Read

**Solution:**
1. Check spelling and case (exact match required)
2. For Vercel: Redeploy after adding variable
3. For Render: Redeploy after saving environment
4. Check application logs for actual values being used

### API Connection Fails

**Solution:**
1. Verify `VITE_API_URL` is set in Vercel
2. Verify backend URL is correct and accessible
3. Check CORS headers in backend logs
4. Test endpoint directly: `curl -i https://verivoice-backend.onrender.com/api/health`

### Database Connection Errors

**Solution:**
1. Verify `SPRING_DATASOURCE_URL` format:
   ```
   postgresql://user:password@host:5432/database
   ```
2. Test connection locally before deploying
3. Check Render PostgreSQL status is "Available"
4. Verify username and password are correct

---

## Environment Variable Precedence (Spring Boot)

Variables are read in this order (first match wins):
1. System environment variables
2. Properties from `.env.production`
3. Properties from `application-production.properties`
4. Properties from `application.properties`

**For Render/Vercel**: Set all variables in the platform's environment variable UI.

---

## Reference: All Required Variables

### Minimal Production Setup

```bash
# Backend (Render)
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
GROQ_API_KEY=...
SPRING_PROFILES_ACTIVE=production

# Frontend (Vercel)
VITE_API_URL=...
```

### Full Production Setup

```bash
# Database
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver

# Groq AI
GROQ_API_KEY=...
GROQ_MODEL=meta-llama/llama-4-scout-17b-16e-instruct

# Application
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/

# JPA
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQL15Dialect
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_JPA_SHOW_SQL=false

# Frontend API
VITE_API_URL=...
```

---

Need help? See [DEPLOYMENT.md](DEPLOYMENT.md) for step-by-step deployment guide.
