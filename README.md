# VeriVoice AI

## Intelligent Invoice Verification & Fraud Detection Platform

VeriVoice AI is an AI-powered invoice verification and fraud detection platform that goes beyond traditional OCR and invoice parsing.

Instead of simply extracting invoice data, VeriVoice AI validates, verifies, and audits invoices using GST verification, tax validation, duplicate detection, vendor intelligence, QR code verification, and AI-powered fraud analysis.

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

```text
Invoice Upload
      ↓
Document Extraction
      ↓
AI Extraction Engine
      ↓
GST Verification
      ↓
QR / IRN Verification
      ↓
Tax Validation
      ↓
Duplicate Detection
      ↓
Fraud Detection Engine
      ↓
Risk Scoring
      ↓
Verification Report
```

---

# Risk Scoring

| Score | Status |
|---------|---------|
| 90-100 | VERIFIED |
| 70-89 | LOW RISK |
| 40-69 | REVIEW REQUIRED |
| 0-39 | HIGH RISK |

---

# Tech Stack

## Backend

- Java 24
- Spring Boot 4
- Spring Data JPA
- Hibernate

## Database

- PostgreSQL

## AI Layer

- Groq API
- Llama Models

## Document Processing

- Apache PDFBox
- Apache POI

## Validation Layer

- GST Validation
- Tax Validation
- Duplicate Detection
- Fraud Analysis

---

# Database Design

## Documents

Stores:

- Uploaded Files
- Extracted Data
- Risk Scores
- Verification Results

## Vendors

Stores:

- GSTIN
- Legal Name
- Verification History

## Verification Cache

Stores:

- Previously Verified GST Records
- Vendor Metadata

---

# Future Enhancements

## Real-Time GST Verification

- GST Registry Validation
- Vendor Status Verification
- Legal Name Verification

## ERP Integration

- SAP
- Oracle ERP
- Tally
- Zoho Books
- QuickBooks

## Advanced Fraud Analytics

- Vendor Reputation Scoring
- Invoice Network Analysis
- Cross-Organization Fraud Detection

---

# Project Goal

VeriVoice AI aims to transform invoice processing from simple data extraction into a comprehensive verification and fraud detection workflow.

The platform helps organizations reduce financial fraud, automate invoice validation, improve compliance, and accelerate approval processes.

---

## Author

**Navaneedan S**
