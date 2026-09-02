# VeriVoice Production Deployment Guide

## Overview

This guide covers deploying VeriVoice to production with:
- **Frontend**: Vercel (serverless deployment)
- **Backend**: Render (container/web service)
- **Database**: PostgreSQL on Render
- **CI/CD**: GitHub Actions

## Prerequisites

1. **GitHub Repository**
   - Push all code to GitHub (public or private)
   - Ensure main/master branch is your production branch

2. **Vercel Account**
   - Sign up at https://vercel.com
   - Install Vercel CLI: `npm i -g vercel`

3. **Render Account**
   - Sign up at https://render.com
   - No CLI needed for basic deployment

4. **Environment Variables**
   - Groq API Key: Get from https://console.groq.com
   - (Optional) Custom domain for frontend

---

## Step 1: Frontend Deployment (Vercel)

### Option A: Via GitHub (Recommended)

1. **Connect GitHub to Vercel**
   - Go to https://vercel.com/new
   - Click "Continue with GitHub"
   - Authorize and select the `verivoice` repository
   - Choose `frontend` as root directory

2. **Configure Build Settings**
   - Build Command: `npm run build`
   - Output Directory: `dist`
   - Install Command: `npm ci`

3. **Set Environment Variables**
   - Go to Project Settings → Environment Variables
   - Add: `VITE_API_URL` = (will update after backend is deployed)
   - Example: `https://verivoice-backend.onrender.com`

4. **Deploy**
   - Click "Deploy"
   - Wait for build to complete (~2-3 minutes)
   - Note the generated URL (e.g., `verivoice.vercel.app`)

5. **Set Custom Domain (Optional)**
   - Settings → Domains
   - Add custom domain
   - Update DNS records as instructed

### Option B: Via Vercel CLI

```bash
cd frontend
vercel --prod
```

---

## Step 2: Backend Deployment (Render)

### Create PostgreSQL Database

1. **Go to Render Dashboard**
   - https://dashboard.render.com/

2. **Create New PostgreSQL Database**
   - Click "New +" → "PostgreSQL"
   - Name: `verivoice-db`
   - Region: Select closest to your users
   - PostgreSQL Version: 15
   - Click "Create Database"

3. **Wait for Database**
   - Status will show "Available" when ready (~5 minutes)
   - Save connection details:
     - Host
     - Database: `verivoice`
     - Username: `verivoice`
     - Password

4. **Initialize Database Schema**
   - Click "Connect" in Render dashboard
   - Use the connection string to connect with psql or DBeaver
   - Execute: [schema.sql](server/src/main/resources/db/schema.sql)

   ```bash
   psql postgresql://user:password@host:5432/verivoice < server/src/main/resources/db/schema.sql
   ```

### Create Web Service for Backend

1. **Go to Render Dashboard**
   - Click "New +" → "Web Service"

2. **Connect Repository**
   - Select GitHub repository
   - Repository: `verivoice`
   - Branch: `main` or `master`
   - Click "Connect"

3. **Configure Deployment**
   - Name: `verivoice-backend`
   - Environment: `Java`
   - Region: Same as database
   - Build Command: `mvn clean package -q -DskipTests`
   - Start Command: `java -Dserver.port=${PORT} -Dspring.profiles.active=production -jar target/*.jar`

4. **Set Environment Variables**
   - Click "Advanced" → "Add Environment Variable"
   - Add the following:

   ```
   SPRING_DATASOURCE_URL=postgresql://host:5432/verivoice
   SPRING_DATASOURCE_USERNAME=verivoice
   SPRING_DATASOURCE_PASSWORD=<password_from_db>
   GROQ_API_KEY=<your_groq_api_key>
   GROQ_MODEL=meta-llama/llama-4-scout-17b-16e-instruct
   SPRING_PROFILES_ACTIVE=production
   ```

5. **Deploy**
   - Click "Create Web Service"
   - Wait for initial deployment (~5-10 minutes)
   - Note the service URL (e.g., `verivoice-backend.onrender.com`)

6. **Health Check Configuration**
   - Settings → Health Check
   - Endpoint: `/api/health` (if available)
   - Grace Period: 60 seconds

---

## Step 3: Connect Frontend to Backend

1. **Update Frontend Environment Variables in Vercel**
   - Go to Vercel Project Settings
   - Environment Variables
   - Update `VITE_API_URL` to: `https://verivoice-backend.onrender.com`
   - Redeploy: Click "Deployments" → Latest → "Redeploy"

2. **Test Connection**
   - Open frontend URL
   - Try uploading a document
   - Check browser network tab for successful API calls

---

## Step 4: Set Up CI/CD (Optional but Recommended)

The `/.github/workflows/deploy.yml` file enables automatic deployment on git push.

### GitHub Secrets Setup

1. Go to Repository Settings → Secrets and variables → Actions

2. Add the following secrets:

   **For Vercel:**
   ```
   VERCEL_TOKEN=<your_vercel_token>
   VERCEL_ORG_ID=<your_vercel_org_id>
   VERCEL_PROJECT_ID=<your_vercel_project_id>
   ```

   **For Render:**
   ```
   RENDER_API_KEY=<your_render_api_key>
   RENDER_SERVICE_ID=<your_render_service_id>
   RENDER_BACKEND_URL=https://verivoice-backend.onrender.com
   ```

3. **Get Tokens:**
   - **Vercel Token**: Account Settings → Tokens → Create new token
   - **Render API Key**: Account Settings → API Keys
   - **Render Service ID**: From the Render backend service URL or dashboard

### Test CI/CD

```bash
git push origin main
```

GitHub Actions will automatically:
1. Run backend tests
2. Build frontend
3. Deploy to Vercel (if tests pass)
4. Deploy to Render (if tests pass)
5. Send health check

---

## Step 5: Custom Domain (Optional)

### Frontend Custom Domain

**Option 1: Using Vercel Domain**
1. Vercel → Project Settings → Domains
2. Add your custom domain
3. Update your domain registrar's DNS to point to Vercel

**Option 2: Using Subdomain**
```
frontend.yourdomain.com → Vercel
api.yourdomain.com → Render
```

### Backend Custom Domain

1. Render → Service Settings → Custom Domains
2. Add domain: `api.yourdomain.com`
3. Update DNS CNAME record to Render

---

## Monitoring & Maintenance

### Check Deployment Status

**Vercel:**
```bash
vercel status
```

**Render:**
- Dashboard → Service → Logs tab

### View Logs

**Vercel:**
- Project → Deployments → Click deployment → Logs

**Render:**
- Service → Logs tab (real-time logs)

### Monitor Performance

- **Vercel Analytics**: Project → Analytics
- **Render Metrics**: Service → Metrics

---

## Troubleshooting

### Frontend Not Loading

**Issue**: 404 errors or blank page

**Solution**:
1. Check Vercel deployment status
2. Verify `VITE_API_URL` is set correctly
3. Check browser console for errors
4. Clear cache and hard refresh (Ctrl+Shift+R)

### API Calls Failing (CORS)

**Issue**: Frontend → Backend calls blocked

**Solution**:
1. Backend has CORS enabled in `application-production.properties`
2. Verify `VITE_API_URL` doesn't have trailing slash
3. Check Render service is running: `curl https://verivoice-backend.onrender.com/api/dashboard/stats`

### Database Connection Error

**Issue**: Backend shows database connection failures

**Solution**:
1. Verify PostgreSQL service is "Available" in Render
2. Check environment variables:
   ```bash
   echo $SPRING_DATASOURCE_URL
   ```
3. Test connection manually:
   ```bash
   psql $SPRING_DATASOURCE_URL
   ```

### Cold Start Delays

**Issue**: First request to backend is slow (~30 seconds)

**Solution**: This is normal for Render free tier. Upgrade to paid plan for faster cold starts.

---

## Cost Estimation (Monthly)

| Service | Plan | Cost |
|---------|------|------|
| Vercel Frontend | Pro | $20 |
| Render Backend | Standard | $7 |
| Render PostgreSQL | Standard | $7 |
| **Total** | | **~$34** |

**Free Tier Options:**
- Vercel: Free tier available (with limits)
- Render: Free tier with 750 hours/month (goes to sleep after 15 min inactivity)

---

## Rollback Procedure

### If Deployment Breaks

**Vercel:**
1. Go to Deployments
2. Find previous working deployment
3. Click "Promote to Production"

**Render:**
1. Service → Deploy log
2. Click previous successful deploy
3. Manual redeploy if needed

---

## Next Steps

1. Test document upload and verification in production
2. Set up monitoring alerts
3. Configure error tracking (e.g., Sentry)
4. Set up database backups (Render handles this)
5. Document your custom domain configuration

---

## Support

- **Vercel Docs**: https://vercel.com/docs
- **Render Docs**: https://render.com/docs
- **Spring Boot Production Checklist**: https://spring.io/guides/gs/spring-boot/

---

**Deployed Successfully?**

Congratulations! VeriVoice is now in production. Monitor the dashboards and logs regularly.
