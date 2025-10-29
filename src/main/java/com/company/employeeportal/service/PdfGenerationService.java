package com.company.employeeportal.service;

import com.company.employeeportal.model.Payroll;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating PDF documents, specifically payslips.
 * Uses iText7 library for PDF creation with company branding.
 */
@Service
public class PdfGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationService.class);
    
    private static final String COMPANY_NAME = "Employee Portal Inc.";
    private static final String COMPANY_ADDRESS = "123 Business Street, Corporate City, CC 12345";
    private static final String COMPANY_PHONE = "Phone: (555) 123-4567";
    private static final String COMPANY_EMAIL = "Email: hr@employeeportal.com";

    /**
     * Generate a PDF payslip for the given payroll record.
     * 
     * @param payroll The payroll record to generate PDF for
     * @return byte array containing the PDF document
     * @throws RuntimeException if PDF generation fails
     */
    public byte[] generatePayslipPdf(Payroll payroll) {
        logger.info("Generating PDF payslip for user: {} for period: {}/{}", 
                   payroll.getUser().getName(), payroll.getMonth(), payroll.getYear());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Add company header
            addCompanyHeader(document);
            
            // Add payslip title
            addPayslipTitle(document, payroll);
            
            // Add employee information
            addEmployeeInformation(document, payroll);
            
            // Add pay period information
            addPayPeriodInformation(document, payroll);
            
            // Add earnings and deductions table
            addEarningsAndDeductionsTable(document, payroll);
            
            // Add summary section
            addSummarySection(document, payroll);
            
            // Add footer
            addFooter(document);
            
            document.close();
            
            byte[] pdfBytes = baos.toByteArray();
            logger.info("Successfully generated PDF payslip ({} bytes) for user: {}", 
                       pdfBytes.length, payroll.getUser().getName());
            
            return pdfBytes;
            
        } catch (IOException e) {
            logger.error("Failed to generate PDF payslip for user: {}", payroll.getUser().getName(), e);
            throw new RuntimeException("Failed to generate PDF payslip", e);
        }
    }

    /**
     * Add company header with branding.
     */
    private void addCompanyHeader(Document document) {
        // Company name
        Paragraph companyName = new Paragraph(COMPANY_NAME)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(companyName);
        
        // Company address
        Paragraph companyAddress = new Paragraph(COMPANY_ADDRESS)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);
        document.add(companyAddress);
        
        // Company contact info
        Paragraph companyContact = new Paragraph(COMPANY_PHONE + " | " + COMPANY_EMAIL)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(companyContact);
    }

    /**
     * Add payslip title.
     */
    private void addPayslipTitle(Document document, Payroll payroll) {
        Paragraph title = new Paragraph("PAYSLIP")
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(10)
                .setMarginBottom(20);
        document.add(title);
    }

    /**
     * Add employee information section.
     */
    private void addEmployeeInformation(Document document, Payroll payroll) {
        Table employeeTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(15);
        
        // Employee details
        employeeTable.addCell(createInfoCell("Employee Name:", payroll.getUser().getName()));
        employeeTable.addCell(createInfoCell("Employee ID:", payroll.getUser().getId().toString()));
        employeeTable.addCell(createInfoCell("Email:", payroll.getUser().getEmail()));
        employeeTable.addCell(createInfoCell("Department:", payroll.getUser().getDepartment()));
        employeeTable.addCell(createInfoCell("Position:", payroll.getUser().getPosition()));
        employeeTable.addCell(createInfoCell("Join Date:", 
                payroll.getUser().getJoinDate() != null ? 
                payroll.getUser().getJoinDate().toString() : "N/A"));
        
        document.add(employeeTable);
    }

    /**
     * Add pay period information.
     */
    private void addPayPeriodInformation(Document document, Payroll payroll) {
        Table periodTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(15);
        
        periodTable.addCell(createInfoCell("Pay Period:", String.format("%02d/%d", payroll.getMonth(), payroll.getYear())));
        periodTable.addCell(createInfoCell("Generated On:", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        
        document.add(periodTable);
    }

    /**
     * Add earnings and deductions table.
     */
    private void addEarningsAndDeductionsTable(Document document, Payroll payroll) {
        // Earnings section
        Paragraph earningsTitle = new Paragraph("EARNINGS")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(earningsTitle);
        
        Table earningsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(15);
        
        // Header
        earningsTable.addHeaderCell(createHeaderCell("Description"));
        earningsTable.addHeaderCell(createHeaderCell("Amount"));
        
        // Gross pay
        earningsTable.addCell(createDataCell("Gross Pay"));
        earningsTable.addCell(createAmountCell(payroll.getGrossPay()));
        
        document.add(earningsTable);
        
        // Deductions section
        Paragraph deductionsTitle = new Paragraph("DEDUCTIONS")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(deductionsTitle);
        
        Table deductionsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(15);
        
        // Header
        deductionsTable.addHeaderCell(createHeaderCell("Description"));
        deductionsTable.addHeaderCell(createHeaderCell("Amount"));
        
        // Total deductions
        deductionsTable.addCell(createDataCell("Total Deductions"));
        deductionsTable.addCell(createAmountCell(payroll.getDeductions()));
        
        // Deduction details if available
        if (payroll.getDeductionDetails() != null && !payroll.getDeductionDetails().trim().isEmpty()) {
            deductionsTable.addCell(createDataCell("Details"));
            deductionsTable.addCell(createDataCell(payroll.getDeductionDetails()));
        }
        
        document.add(deductionsTable);
    }

    /**
     * Add summary section with net pay.
     */
    private void addSummarySection(Document document, Payroll payroll) {
        Paragraph summaryTitle = new Paragraph("SUMMARY")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(summaryTitle);
        
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
        
        // Summary rows
        summaryTable.addCell(createSummaryCell("Gross Pay:"));
        summaryTable.addCell(createAmountCell(payroll.getGrossPay()));
        
        summaryTable.addCell(createSummaryCell("Total Deductions:"));
        summaryTable.addCell(createAmountCell(payroll.getDeductions()));
        
        // Calculate tax amount (difference between gross and net minus deductions)
        BigDecimal taxAmount = payroll.getTaxAmount();
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            summaryTable.addCell(createSummaryCell("Tax Amount:"));
            summaryTable.addCell(createAmountCell(taxAmount));
        }
        
        // Net pay (highlighted)
        summaryTable.addCell(createSummaryCell("NET PAY:", true));
        summaryTable.addCell(createAmountCell(payroll.getNetPay(), true));
        
        document.add(summaryTable);
    }

    /**
     * Add footer with disclaimer and generation info.
     */
    private void addFooter(Document document) {
        Paragraph disclaimer = new Paragraph(
                "This is a computer-generated payslip and does not require a signature. " +
                "For any queries regarding this payslip, please contact the HR department.")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setItalic();
        document.add(disclaimer);
        
        Paragraph generatedInfo = new Paragraph(
                "Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(generatedInfo);
    }

    /**
     * Create an information cell for employee details.
     */
    private Table createInfoCell(String label, String value) {
        Table cellTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .setWidth(UnitValue.createPercentValue(100));
        
        cellTable.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(null));
        cellTable.addCell(new Cell().add(new Paragraph(value != null ? value : "N/A")).setBorder(null));
        
        return cellTable;
    }

    /**
     * Create a header cell for tables.
     */
    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    /**
     * Create a data cell for tables.
     */
    private Cell createDataCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setPadding(5);
    }

    /**
     * Create an amount cell with currency formatting.
     */
    private Cell createAmountCell(BigDecimal amount) {
        return createAmountCell(amount, false);
    }

    /**
     * Create an amount cell with currency formatting and optional highlighting.
     */
    private Cell createAmountCell(BigDecimal amount, boolean highlight) {
        String formattedAmount = String.format("$%,.2f", amount);
        Cell cell = new Cell()
                .add(new Paragraph(formattedAmount))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(5);
        
        if (highlight) {
            cell.setBackgroundColor(ColorConstants.YELLOW).setBold();
        }
        
        return cell;
    }

    /**
     * Create a summary cell for the summary section.
     */
    private Cell createSummaryCell(String text) {
        return createSummaryCell(text, false);
    }

    /**
     * Create a summary cell with optional highlighting.
     */
    private Cell createSummaryCell(String text, boolean highlight) {
        Cell cell = new Cell()
                .add(new Paragraph(text))
                .setPadding(5);
        
        if (highlight) {
            cell.setBold().setBackgroundColor(ColorConstants.YELLOW);
        }
        
        return cell;
    }
}
