package com.skyfl.pfm.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.skyfl.pfm.report.dto.CategoryBreakdownItem;
import com.skyfl.pfm.report.dto.SummaryResponse;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PdfExportService {

    public byte[] export(int year, int month, SummaryResponse summary, List<CategoryBreakdownItem> breakdown) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("Personal Finance Report"));
            document.add(new Paragraph("Month: " + year + "-" + String.format("%02d", month)));
            document.add(new Paragraph("Income: " + summary.totalIncome()));
            document.add(new Paragraph("Expense: " + summary.totalExpense()));
            document.add(new Paragraph("Net Savings: " + summary.netSavings()));
            document.add(new Paragraph("Savings Rate: " + summary.savingsRate()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Expense Breakdown"));
            for (CategoryBreakdownItem item : breakdown) {
                document.add(new Paragraph(item.categoryName() + ": " + item.amount()));
            }
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate PDF", ex);
        }
    }
}
