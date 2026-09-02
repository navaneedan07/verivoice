-- VeriVoice Production Database Schema Initialization
-- Keep this aligned with the entity model used by the application.

CREATE TABLE IF NOT EXISTS gst_cache (
    gstin VARCHAR(15) PRIMARY KEY,
    legal_name VARCHAR(255),
    status VARCHAR(50),
    last_verified TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vendors (
    gstin VARCHAR(15) PRIMARY KEY,
    legal_name VARCHAR(255),
    trade_name VARCHAR(255),
    state VARCHAR(100),
    status VARCHAR(50),
    verified_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    content_type VARCHAR(100),
    file_hash VARCHAR(64),
    source_file BYTEA,
    upload_date TIMESTAMP,
    status VARCHAR(50),
    raw_llm_response TEXT,
    extracted_text TEXT,
    risk_score DOUBLE PRECISION,
    verification_score INTEGER,
    verification_status VARCHAR(50),
    vendor_name VARCHAR(255),
    gst_number VARCHAR(15),
    invoice_number VARCHAR(100),
    invoice_date DATE,
    subtotal DOUBLE PRECISION,
    tax_amount DOUBLE PRECISION,
    total_amount DOUBLE PRECISION,
    cgst_amount DOUBLE PRECISION,
    sgst_amount DOUBLE PRECISION,
    igst_amount DOUBLE PRECISION,
    gst_rate DOUBLE PRECISION,
    currency VARCHAR(10),
    hsn_sac VARCHAR(255),
    irn VARCHAR(255),
    qr_code TEXT,
    confidence_score DOUBLE PRECISION,
    recipient_gstin VARCHAR(15)
);

CREATE TABLE IF NOT EXISTS invoice_anomalies (
    invoice_id UUID NOT NULL,
    anomalies VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS invoice_verification_checks (
    invoice_id UUID NOT NULL,
    code VARCHAR(255),
    detail TEXT,
    layer VARCHAR(255),
    name VARCHAR(255),
    points DOUBLE PRECISION,
    status VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vendor_history (
    id UUID PRIMARY KEY,
    gst_number VARCHAR(15),
    vendor_name VARCHAR(255),
    total_documents INTEGER,
    verified_documents INTEGER,
    average_score DOUBLE PRECISION,
    last_verification TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoices_gst_number ON invoices (gst_number);
CREATE INDEX IF NOT EXISTS idx_invoices_invoice_number ON invoices (invoice_number);
CREATE INDEX IF NOT EXISTS idx_vendor_history_gst_number ON vendor_history (gst_number);
