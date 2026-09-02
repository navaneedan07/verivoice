# VeriVoice AI

## Intelligent Invoice Verification & Fraud Detection Platform

VeriVoice AI is an AI-powered invoice verification and fraud detection platform that goes beyond traditional OCR and invoice parsing.

Instead of simply extracting invoice data, VeriVoice AI validates, verifies, and audits invoices using GST verification, tax validation, duplicate detection, vendor intelligence, QR code verification, and AI-powered fraud analysis.

---

## Quick Start

### Local Development

```bash
# Backend
cd server && mvn spring-boot:run

# Frontend (new terminal)
cd frontend && npm install && npm run dev
```

Visit: http://localhost:5173

### Production Deployment

- **Frontend**: Vercel
- **Backend**: Render (Java + PostgreSQL)
- **Deployment Docs**: See [DEPLOYMENT.md](DEPLOYMENT.md)

Quick deploy:
1. Follow [DEPLOYMENT.md](DEPLOYMENT.md) step-by-step
2. Check [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) for configuration
3. Use [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) checklist

**Production URLs** (after deployment):
- Frontend: `https://verivoice.vercel.app`
- Backend: `https://verivoice-backend.onrender.com`

---

## Architecture

```
┌─────────────────────────────────────────────┐
│          Frontend (React + Vite)            │
│         Deployed on Vercel                  │
└──────────────────┬──────────────────────────┘
                   │ API Calls (HTTP)
                   ↓
┌─────────────────────────────────────────────┐
│    Backend (Spring Boot 4.1.0 on Java 24)   │
│         Deployed on Render                  │
│  ┌───────────────────────────────────────┐  │
│  │  Groq AI  │  GST Verification Engine  │  │
│  │  Document │  Fraud Detection          │  │
│  │  Parsing  │  QR Decoder               │  │
│  └───────────────────────────────────────┘  │
└──────────────────┬──────────────────────────┘
                   │
                   ↓
        ┌──────────────────────┐
        │  PostgreSQL 15       │
        │  (Render)            │
        └──────────────────────┘
```

---

## Tech Stack

### Frontend
- **Framework**: React 18 + TypeScript
- **Build Tool**: Vite 5.4.21
- **Styling**: Tailwind CSS
- **HTTP Client**: Axios
- **State**: React Hooks

### Backend
- **Framework**: Spring Boot 4.1.0
- **Java**: Java 24
- **Build Tool**: Maven
- **Database**: PostgreSQL 15
- **API**: Spring Web MVC
- **Data**: Spring Data JPA

### AI & Integration
- **Vision AI**: Groq API (Meta Llama Scout 17B)
- **QR Decoding**: ZXing
- **PDF Parsing**: PDFBox

### Deployment
- **Frontend**: Vercel
- **Backend**: Render
- **Database**: Render PostgreSQL
- **CI/CD**: GitHub Actions

---

## Problem Statement

Organizations process thousands of invoices every month.

Common challenges include:

- Fake invoices generated using editing tools
- Invalid GST numbers
- Vendor impersonation
- Duplicate invoice submissions
- Manipulated invoice amounts
- Incorrect tax calculations
- Manual verification delays

Traditional OCR systems only extract information and cannot determine whether an invoice is trustworthy.

---

## Solution

VeriVoice AI combines:

- AI-Based Data Extraction
- GST Verification
- QR / IRN Validation
- Tax Calculation Validation
- Duplicate Invoice Detection
- Vendor Intelligence
- AI Fraud Detection

to determine whether an invoice is genuine and safe for processing.

---

# Features

## Multi-Format Document Upload

Supported formats:

- PDF
- DOCX
- PNG
- JPG
- JPEG
- Receipts
- Tax Invoices
- Business Documents

---

## AI-Powered Invoice Extraction

Automatically extracts:

- Vendor Name
- GSTIN
- Invoice Number
- Invoice Date
- Customer Details
- Tax Amount
- Total Amount
- Currency
- HSN/SAC Codes
- Line Items

Uses Groq AI (Meta Llama Scout 17B) with vision support for accurate extraction and fallback deterministic parsing for robustness.

---

## GST Verification

Verifies:

- GSTIN Format
- GST Status
- Vendor Name Matching
- State Code Validation

Example:

```text
Invoice GSTIN:
33AAACW4514C1ZW

Verified Vendor:
WONDERLA HOLIDAYS LIMITED

Status:
ACTIVE
```

---

## QR Code & IRN Verification

When available:

- Detect QR Codes
- Decode QR Payload
- Extract IRN
- Validate Invoice Data
- Cross-check with Extracted Fields

**Note**: IRN signature verification is currently disabled.

---

## Mathematical Validation

Verifies:

- Subtotal
- Tax Amount
- CGST
- SGST
- IGST
- Total Amount

Example:

```text
Subtotal = 919.00
Tax = 174.42
Convenience Fee = 50.00

Expected Total = 1143.42
Actual Total = 1143.42

PASS
```

---

## Duplicate Invoice Detection

Checks:

- Invoice Number
- GSTIN
- Vendor
- Amount
- Date

Detects duplicate invoice submissions.

---

## Vendor Intelligence

Maintains a trusted vendor database.

Stores:

- GSTIN
- Legal Name
- Verification History
- Fraud History
- Risk Scores

---

## AI Fraud Detection

Analyzes:

- Suspicious Invoice Structure
- Unusual Numbering Patterns
- Vendor Inconsistencies
- Missing Compliance Fields
- Tax Anomalies
- Potential Fraud Indicators

---

# Verification Pipeline

```
Invoice Upload
      ↓
Document Extraction (PDF/Image)
      ↓
AI Extraction Engine (Groq API)
      ↓
Fallback Deterministic Parsing
      ↓
GST Verification
      ↓
QR / IRN Detection
      ↓
Tax Validation
      ↓
Duplicate Detection
      ↓
Fraud Detection
      ↓
Verification Score & Recommendation
```

---

# Verification Score

Maximum 68 points (IRN verification disabled):

| Check | Points | Example |
|-------|--------|---------|
| GST Verification | 50 | GSTIN valid + vendor match |
| Vendor History | 8 | No previous fraud |
| Tax Validation | 0-5 | All calculations correct |
| Duplicate Check | 0-5 | No duplicates found |
| QR Detection | 0-5 | QR code detected (NON-CRITICAL) |

**Example Score**:
- Receipt from WONDERLA HOLIDAYS LIMITED
- Valid GSTIN (33AAACW4514C1ZW)
- Correct tax calculations
- Score: **68/100** (GREEN - APPROVED)

---

# Environment Setup

## Prerequisites

- Node.js 18+ (Frontend)
- Java 24 (Backend)
- Maven 3.9+ (Backend build)
- PostgreSQL 15 (Production database)
- Groq API Key (Free tier available)

## Local Development

```bash
# Clone repository
git clone <repository-url>
cd verivoice

# Backend setup
cd server
# Add your Groq API key to environment
export GROQ_API_KEY=your_key_here
mvn spring-boot:run

# Frontend setup (new terminal)
cd frontend
npm install
npm run dev
```

## Production Deployment

See complete deployment guide: [DEPLOYMENT.md](DEPLOYMENT.md)

Quick summary:
1. Frontend → Vercel (auto-deploy from git)
2. Backend → Render (auto-deploy from git)
3. Database → Render PostgreSQL
4. CI/CD → GitHub Actions

---

# API Endpoints

## Documents

```
POST   /api/documents/upload          - Upload invoice for verification
GET    /api/documents/{id}             - Get verification result
GET    /api/documents                  - List all documents
DELETE /api/documents/{id}             - Delete document
```

## Dashboard

```
GET    /api/dashboard/stats            - Overall statistics
GET    /api/dashboard/recent           - Recent verifications
GET    /api/dashboard/fraud-alerts     - Fraud indicators
```

## Vendors

```
GET    /api/vendors                    - List all vendors
GET    /api/vendors/{gstin}            - Get vendor details
GET    /api/vendors/history/{gstin}    - Vendor verification history
```

## Health

```
GET    /api/health                     - Application health check
```

---

# Configuration

### Environment Variables

Required for production:

```bash
# Database
SPRING_DATASOURCE_URL=postgresql://...
SPRING_DATASOURCE_USERNAME=verivoice
SPRING_DATASOURCE_PASSWORD=...

# Groq AI
GROQ_API_KEY=gsk_...
GROQ_MODEL=meta-llama/llama-4-scout-17b-16e-instruct

# Frontend API
VITE_API_URL=https://backend-url.com
```

See [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) for complete reference.

---

# Testing

## Backend Unit Tests

```bash
cd server
mvn test
```

**Test Results**: 13/13 tests passing ✓

## Frontend Build

```bash
cd frontend
npm run build
```

**Build Status**: Success (1548 modules) ✓

---

# Database Schema

PostgreSQL 15 tables:

- `gst_cache` - GST database cache
- `vendor` - Vendor master data
- `document` - Invoice documents
- `verification_check` - Verification results
- `document_anomaly` - Fraud indicators
- `vendor_history` - Vendor verification history

See [schema.sql](server/src/main/resources/db/schema.sql) for full schema.

---

# Development

### File Structure

```
verivoice/
├── frontend/                 - React + Vite
│   ├── src/
│   │   ├── components/      - React components
│   │   ├── pages/           - Page components
│   │   ├── services/        - API client
│   │   └── types/           - TypeScript interfaces
│   └── vite.config.ts
│
├── server/                   - Spring Boot backend
│   ├── src/main/java/
│   │   └── com/verivoice/
│   │       ├── controller/  - REST endpoints
│   │       ├── service/     - Business logic
│   │       ├── entity/      - JPA entities
│   │       ├── repository/  - Data access
│   │       └── verification/ - Verification logic
│   ├── src/test/java/       - Unit tests
│   └── pom.xml
│
├── DEPLOYMENT.md            - Deployment guide
├── ENVIRONMENT_VARIABLES.md - Config reference
├── PRODUCTION_READINESS.md  - Pre-deployment checklist
└── README.md               - This file
```

### Code Quality

- TypeScript strict mode enabled (Frontend)
- All tests passing (Backend)
- No compiler errors

---

# Troubleshooting

| Issue | Solution |
|-------|----------|
| API connection fails | Check `VITE_API_URL` environment variable |
| Database errors | Verify PostgreSQL is running and schema initialized |
| Groq API errors | Check API key validity and rate limits |
| QR code not detected | Some invoices may not have QR codes (optional) |
| Receipt score below expected | Review extraction results, check for special characters |

See [DEPLOYMENT.md](DEPLOYMENT.md#troubleshooting) for more.

---

# Support & Documentation

- [Deployment Guide](DEPLOYMENT.md)
- [Environment Variables](ENVIRONMENT_VARIABLES.md)
- [Production Checklist](PRODUCTION_READINESS.md)
- [Groq API Docs](https://console.groq.com)
- [Spring Boot Docs](https://spring.io/guides)

---

# License

[Add your license here]

---

**Ready to Deploy?** → Start with [DEPLOYMENT.md](DEPLOYMENT.md)

**Need Configuration Help?** → Check [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md)

**Pre-Deployment Checklist?** → Use [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md)
