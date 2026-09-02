-- VeriVoice Production Database Schema Initialization
-- Execute this script on Render PostgreSQL after database creation

-- Drop existing tables (if any) - use with caution
-- DROP TABLE IF EXISTS document CASCADE;
-- DROP TABLE IF EXISTS vendor CASCADE;
-- DROP TABLE IF EXISTS gst_cache CASCADE;
-- DROP TABLE IF EXISTS vendor_history CASCADE;

-- Create tables
CREATE TABLE IF NOT EXISTS gst_cache (
    id VARCHAR(15) PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vendor (
    id VARCHAR(15) PRIMARY KEY,
    vendor_name VARCHAR(255) NOT NULL,
    vendor_short_name VARCHAR(100),
    state VARCHAR(100),
    vendor_status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255),
    content_type VARCHAR(50),
    source_file BYTEA,
    extracted_text TEXT,
    raw_llm_response TEXT,
    file_hash VARCHAR(255),
    status VARCHAR(50),
    verification_score DOUBLE PRECISION,
    verification_status VARCHAR(50),
    risk_score DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- ExtractedData fields
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
    
    FOREIGN KEY (gst_number) REFERENCES gst_cache(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS verification_check (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    layer VARCHAR(100),
    code VARCHAR(100),
    name VARCHAR(255),
    status VARCHAR(50),
    detail TEXT,
    points DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS document_anomaly (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    anomaly_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS vendor_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gst_number VARCHAR(15),
    vendor_name VARCHAR(255),
    total_documents INT,
    verified_documents INT,
    average_score DOUBLE PRECISION,
    last_verification TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (gst_number) REFERENCES gst_cache(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_document_gst_number ON document(gst_number);
CREATE INDEX IF NOT EXISTS idx_document_invoice_number ON document(invoice_number);
CREATE INDEX IF NOT EXISTS idx_document_created_at ON document(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_verification_check_document_id ON verification_check(document_id);
CREATE INDEX IF NOT EXISTS idx_vendor_history_gst_number ON vendor_history(gst_number);

-- Grant permissions (adjust username as needed)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO verivoice;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO verivoice;
