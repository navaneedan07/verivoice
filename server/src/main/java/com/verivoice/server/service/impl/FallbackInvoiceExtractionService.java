package com.verivoice.server.service.impl;

import com.verivoice.server.embeddable.ExtractedData;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FallbackInvoiceExtractionService {
    private static final Pattern GSTIN = Pattern.compile("\\b[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]\\b");

    public ExtractedData extract(String content) {
        ExtractedData data = new ExtractedData();
        String source = content == null ? "" : content;
        data.setVendorName(value(source, "(?im)^(?:vendor|seller|merchant|store|business(?: name)?|supplier)\\s*[:#-]?\\s*(.+)$"));
        if (data.getVendorName() == null) data.setVendorName(vendorBeforeGstin(source));
        data.setInvoiceNumber(value(source, "(?im)^(?:tax\\s*invoice\\s*(?:number|no)?|invoice\\s*(?:number|no)?|invoice\\s*#|receipt\\s*(?:number|no)?|bill\\s*(?:number|no)?|sales\\s*order\\s*id)\\s*[:#-]?\\s*([A-Z0-9][A-Z0-9./_-]*)$"));
        data.setGstNumber(firstMatch(source.toUpperCase(Locale.ROOT), GSTIN));
        data.setInvoiceDate(parseDate(value(source, "(?im)^(?:invoice\\s*date|receipt\\s*date|sales\\s*order\\s*time|visit\\s*date|date)\\s*[:#-]?\\s*([0-9]{1,2}[-/]\\s*[0-9]{1,2}[-/]\\s*[0-9]{2,4})")));
        data.setSubtotal(number(source, "(?im)\\b(?:subtotal|taxable value|taxable amount|assessable value)\\b\\s*[:#-]?\\s*(?:[^0-9\\n]{0,6})\\s*([0-9,]+(?:\\.[0-9]+)?)"));
        data.setTaxAmount(number(source, "(?im)\\b(?:tax amount|gst amount|total gst|total tax|tax total)\\b\\s*[:#-]?\\s*(?:[^0-9\\n]{0,6})\\s*([0-9,]+(?:\\.[0-9]+)?)"));
        data.setTotalAmount(number(source, "(?im)\\b(?:grand total|invoice value|total amount|amount due|net total|final amount|gross amount)\\b\\s*[:#-]?\\s*(?:[^0-9\\n]{0,6})\\s*([0-9,]+(?:\\.[0-9]+)?)"));
        if (data.getTotalAmount() == null) data.setTotalAmount(lastNumber(source, "(?im)^total\\b\\s*(?:[^0-9\\n]{0,6})\\s*([0-9,]+(?:\\.[0-9]+)?)"));
        data.setCgstAmount(sumNumbers(source, "(?im)\\bcgst(?:\\s+[0-9]+(?:\\.[0-9]+)?\\s*%)?\\s*[:#-]?\\s*(?:[^0-9\\n]{0,6})\\s*([0-9,]+(?:\\.[0-9]+)?)"));
        data.setSgstAmount(sumNumbers(source, "(?im)\\bsgst(?:\\s+[0-9]+(?:\\.[0-9]+)?\\s*%)?\\s*[:#-]?\\s*(?:[^0-9\\n]{0,6})\\s*([0-9,]+(?:\\.[0-9]+)?)"));
        data.setIgstAmount(sumNumbers(source, "(?im)\\bigst(?:\\s+[0-9]+(?:\\.[0-9]+)?\\s*%)?\\s*[:#-]?\\s*(?:[^0-9\\n]{0,6})\\s*([0-9,]+(?:\\.[0-9]+)?)"));
        data.setGstRate(number(source, "(?im)\\b(?:gst|tax)\\s*rate\\s*[:#-]?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*%?"));
        if (data.getTaxAmount() == null && (data.getCgstAmount() != null || data.getSgstAmount() != null || data.getIgstAmount() != null)) {
            data.setTaxAmount((data.getCgstAmount() == null ? 0d : data.getCgstAmount())
                    + (data.getSgstAmount() == null ? 0d : data.getSgstAmount())
                    + (data.getIgstAmount() == null ? 0d : data.getIgstAmount()));
        }
        if (data.getSubtotal() == null && data.getTotalAmount() != null && data.getTaxAmount() != null) {
            double subtotal = data.getTotalAmount() - data.getTaxAmount();
            if (subtotal >= 0d) data.setSubtotal(subtotal);
        }
        if (data.getGstRate() == null && data.getSubtotal() != null && data.getSubtotal() > 0d && data.getTaxAmount() != null) {
            data.setGstRate((data.getTaxAmount() / data.getSubtotal()) * 100d);
        }
        data.setCurrency("INR");
        data.setConfidenceScore(0.45d);
        return data;
    }

    private String value(String source, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(source);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String lastValue(String source, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(source);
        String found = null;
        while (matcher.find()) {
            found = matcher.group(1).trim();
        }
        return found;
    }

    private String firstMatch(String source, Pattern pattern) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group() : null;
    }

    private String vendorBeforeGstin(String source) {
        Matcher gst = GSTIN.matcher(source.toUpperCase(Locale.ROOT));
        if (!gst.find()) return null;
        String before = source.substring(0, gst.start());
        String[] lines = before.split("\\R");
        for (String line : lines) {
            String candidate = line.replaceAll("[^A-Za-z .&'-]", " ").trim();
            if (candidate.length() >= 3 && candidate.length() <= 80
                    && candidate.equals(candidate.toUpperCase(Locale.ROOT))
                    && !candidate.matches("(?i).*(invoice|receipt|sales order|tax|gst|date|customer|website|email|phone|address).*")) {
                return candidate;
            }
        }
        for (int index = lines.length - 1; index >= 0; index--) {
            String line = lines[index].replaceAll("[^A-Za-z .&'-]", " ").trim();
            if (line.length() >= 3 && !line.matches("(?i).*(invoice|receipt|sales order|tax|gst|date|customer).*")) {
                return line;
            }
        }
        return null;
    }

    private Double number(String source, String expression) {
        String value = lastValue(source, expression);
        return value == null ? null : Double.valueOf(value.replace(",", ""));
    }

    private Double lastNumber(String source, String expression) {
        String value = lastValue(source, expression);
        return value == null ? null : Double.valueOf(value.replace(",", ""));
    }

    private LocalDate parseDate(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("\\s", "");
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("d/M/uu"),
                DateTimeFormatter.ofPattern("d-M-uu")
        }) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported invoice date format.
            }
        }
        return null;
    }

    private Double sumNumbers(String source, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(source);
        double total = 0d;
        boolean found = false;
        while (matcher.find()) {
            total += Double.parseDouble(matcher.group(1).replace(",", ""));
            found = true;
        }
        return found ? total : null;
    }
}
