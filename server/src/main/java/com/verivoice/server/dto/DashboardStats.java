package com.verivoice.server.dto;

import java.util.List;

public class DashboardStats {
    private long totalInvoices;
    private long verified;
    private long flagged;
    private long pendingReview;
    private List<DocumentDto> recentVerifications;

    public DashboardStats() {}

    public DashboardStats(long totalInvoices, long verified, long flagged, long pendingReview, List<DocumentDto> recentVerifications) {
        this.totalInvoices = totalInvoices;
        this.verified = verified;
        this.flagged = flagged;
        this.pendingReview = pendingReview;
        this.recentVerifications = recentVerifications;
    }

    public long getTotalInvoices() { return totalInvoices; }
    public void setTotalInvoices(long totalInvoices) { this.totalInvoices = totalInvoices; }

    public long getVerified() { return verified; }
    public void setVerified(long verified) { this.verified = verified; }

    public long getFlagged() { return flagged; }
    public void setFlagged(long flagged) { this.flagged = flagged; }

    public long getPendingReview() { return pendingReview; }
    public void setPendingReview(long pendingReview) { this.pendingReview = pendingReview; }

    public List<DocumentDto> getRecentVerifications() { return recentVerifications; }
    public void setRecentVerifications(List<DocumentDto> recentVerifications) { this.recentVerifications = recentVerifications; }
}
