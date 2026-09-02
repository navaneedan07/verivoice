# VeriVoice Production Readiness Checklist

## Code Quality

- [x] All backend tests passing (13/13)
- [x] Frontend TypeScript build succeeds
- [x] No compiler errors or warnings
- [x] IRN verification disabled (SIGNATURE_VALID always NOT_PERFORMED)
- [x] Receipt parser handles rupee symbols and table formats
- [x] AI-Fallback extraction strategy implemented
- [x] Database schema created
- [x] Environment variables configured

## Deployment Configuration

- [x] Vercel configuration (`vercel.json`)
- [x] Render configuration (`render.yaml`)
- [x] Procfile for Java startup
- [x] Production properties file (`application-production.properties`)
- [x] GitHub Actions workflow (`.github/workflows/deploy.yml`)
- [x] Database initialization script (`db/schema.sql`)

## Documentation

- [x] DEPLOYMENT.md with step-by-step instructions
- [x] ENVIRONMENT_VARIABLES.md with all required variables
- [x] Production dependency reference (pom-render.xml)

## Before Final Deployment

### Frontend (Vercel)

- [ ] Create Vercel account and connect GitHub repo
- [ ] Set `VITE_API_URL` environment variable (update after backend deploys)
- [ ] Configure custom domain (optional)
- [ ] Enable automatic deployments on git push
- [ ] Test frontend loads without errors
- [ ] Verify TypeScript strict mode is active

### Backend (Render)

- [ ] Create Render account
- [ ] Create PostgreSQL database (Render)
- [ ] Initialize database schema (run schema.sql)
- [ ] Create Java web service
- [ ] Set all environment variables:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `GROQ_API_KEY`
  - `SPRING_PROFILES_ACTIVE=production`
- [ ] Configure health check endpoint
- [ ] Enable auto-deploy on git push

### Integration Testing

- [ ] Frontend connects to backend API
- [ ] Document upload works end-to-end
- [ ] Receipt parsing extracts data correctly
- [ ] Verification score calculated and displayed
- [ ] Error handling graceful (no blank pages)
- [ ] API errors shown to user

### Security

- [ ] Groq API key secured in environment variables
- [ ] Database credentials secured (Render managed)
- [ ] No secrets committed to git
- [ ] HTTPS enabled on both frontend and backend
- [ ] CORS configured appropriately
- [ ] Database backups enabled (Render default)

### Monitoring

- [ ] Set up error tracking (optional: Sentry, Rollbar)
- [ ] Configure log viewing in Render
- [ ] Test health check endpoint: `/api/health`
- [ ] Monitor first 24 hours for errors

## Post-Deployment

- [ ] Verify frontend URL is accessible
- [ ] Verify backend API responds to health check
- [ ] Test complete flow: Upload → Parse → Verify
- [ ] Check browser console for errors
- [ ] Review backend logs for exceptions
- [ ] Test on mobile devices

## Rollback Plan

If deployment has critical issues:

1. **Frontend**: Click "Promote to Production" on previous working deployment in Vercel
2. **Backend**: Manually redeploy previous commit in Render, or revert git commits

---

## Quick Reference: Deployment URLs

After deployment, update these locations:

1. **Frontend URL** → Vercel dashboard (e.g., verivoice.vercel.app)
2. **Backend URL** → Render dashboard (e.g., verivoice-backend.onrender.com)
3. **Database URL** → Render PostgreSQL connection string
4. **Frontend Env Var** → `VITE_API_URL` in Vercel (use Backend URL)

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| CORS errors | Ensure `VITE_API_URL` matches backend domain exactly |
| Database connection fails | Verify connection string format, test locally first |
| API 504 errors | Normal on Render free tier (cold start). Upgrade to paid or wait 30s |
| Frontend shows blank page | Check browser console, verify `VITE_API_URL` is set |
| No data visible in dashboard | Check API calls in Network tab, verify backend is running |

---

## Next Steps After Deployment

1. ✅ Set up monitoring alerts
2. ✅ Configure automated backups
3. ✅ Set up error tracking
4. ✅ Monitor logs regularly
5. ✅ Plan first maintenance window
6. ✅ Document custom domain setup (if using)

---

**Ready to Deploy?** Follow [DEPLOYMENT.md](DEPLOYMENT.md) step-by-step.

**Need Help?** Check [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) for configuration details.
