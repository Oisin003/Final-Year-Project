package com.example;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

/**
 * A simple Java proof-of-concept pipeline: 1) Load a financial PDF. 2) Render
 * pages to images with PDFBox. 3) OCR images with Tess4J (Tesseract). 4)
 * Extract: - Narrative sentences. - Financial lines (lines that contain
 * amounts). 5) Derive key metrics (turnover, profit, cash, net assets). 6)
 * Export: - Excel file (Narrative, FinancialLines, Summary). - PDF summary
 * report.
 */
public class FinancialOcrApp {

    // Adjust this to your input PDF file
    private static final String INPUT_PDF = "input.pdf";

    // Output filenames
    private static final String OUTPUT_EXCEL = "financial_output.xlsx";
    private static final String OUTPUT_SUMMARY_PDF = "financial_summary_report.pdf";

    // Regex pattern for money-like tokens
    private static final Pattern MONEY_PATTERN
            = Pattern.compile("(€|£)?\\s*\\(?\\d[\\d,\\.]*\\)?");

    public static void main(String[] args) {
        try {
            String pdfPath = (args.length > 0) ? args[0] : INPUT_PDF;
            // Optional second argument: tessdata path. If not provided, use a
            // commonly-used default on Windows. Adjust as needed for your system.
            String tessDataPath = (args.length > 1) ? args[1]
                    : "C:/Program Files/Tesseract-OCR/tessdata";
            if (!Files.exists(Paths.get(pdfPath))) {
                System.err.println("Input PDF not found: " + pdfPath);
                System.err.println("Place your PDF in the project root or pass a path as argument.");
                return;
            }

            // 1) OCR entire PDF (all pages)
            System.out.println("Running OCR on PDF: " + pdfPath);
            String fullText = ocrPdfToText(pdfPath, tessDataPath);
            System.out.println("OCR complete. Characters: " + fullText.length());

            // 2) Split into sentences
            List<String> sentences = splitIntoSentences(fullText);
            System.out.println("Sentences extracted: " + sentences.size());

            // 3) Extract financial lines (lines with numbers)
            List<FinancialLine> financialLines = extractFinancialLines(fullText);
            System.out.println("Financial lines detected: " + financialLines.size());

            // 4) Compute key metrics
            Map<String, Double> metrics = computeMetrics(fullText, financialLines);

            // 5) Export to Excel
            writeExcel(sentences, financialLines, metrics, OUTPUT_EXCEL);
            System.out.println("Excel written: " + OUTPUT_EXCEL);

            // 6) Export summary PDF
            writeSummaryPdf(metrics, sentences, OUTPUT_SUMMARY_PDF);
            System.out.println("Summary PDF written: " + OUTPUT_SUMMARY_PDF);

            System.out.println("\nDONE. Check " + OUTPUT_EXCEL + " and " + OUTPUT_SUMMARY_PDF);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Render each page of the PDF as an image and run Tesseract OCR on it.
     */
    private static String ocrPdfToText(String pdfPath, String tessDataPath) throws IOException, TesseractException {
        StringBuilder sb = new StringBuilder();

        // Load PDF
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);

            // Prepare Tesseract. Use provided tessdata path so users don't need
            // to edit this source file.
            ITesseract tesseract = new Tesseract();
            // Use the tessdata path supplied by the caller (or default passed
            // from main). Update as needed for your environment.
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");

            int pageCount = document.getNumberOfPages();
            for (int page = 0; page < pageCount; page++) {
                System.out.println(" OCR page " + (page + 1) + " / " + pageCount);
                BufferedImage image = renderer.renderImageWithDPI(page, 300);
                String text = tesseract.doOCR(image);
                sb.append(text).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Very simple sentence splitter using punctuation marks.
     */
    private static List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        // Split on ., !, ? followed by whitespace
        String[] parts = text.split("(?<=[.!?])\\s+");
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    /**
     * A simple representation of a "financial line" – a line of text from the
     * OCR that contains one or more numeric tokens (e.g. a table row).
     */
    private static class FinancialLine {

        String rawLine;
        String label;
        List<Double> values = new ArrayList<>();
    }

    /**
     * Extract lines that contain money-like tokens, and attempt to split them
     * into label + numeric amounts.
     */
    private static List<FinancialLine> extractFinancialLines(String text) {
        List<FinancialLine> lines = new ArrayList<>();

        String[] rawLines = text.split("\\r?\\n");
        for (String raw : rawLines) {
            Matcher m = MONEY_PATTERN.matcher(raw);
            List<String> tokens = new ArrayList<>();

            while (m.find()) {
                tokens.add(m.group());
            }
            if (tokens.isEmpty()) {
                continue;
            }

            FinancialLine fl = new FinancialLine();
            fl.rawLine = raw;

            // Label: everything before first numeric match
            int firstIdx = raw.length();
            if (!tokens.isEmpty()) {
                m = MONEY_PATTERN.matcher(raw);
                if (m.find()) {
                    firstIdx = m.start();
                }
            }
            fl.label = raw.substring(0, firstIdx).trim();

            // Parse numeric values
            for (String tok : tokens) {
                String cleaned = tok.replace("(", "-").replace(")", "");
                cleaned = cleaned.replaceAll("[^0-9\\.-]", "");
                if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
                    continue;
                }
                try {
                    fl.values.add(Double.parseDouble(cleaned.replace(",", "")));
                } catch (NumberFormatException nfe) {
                    // ignore
                }
            }

            if (!fl.values.isEmpty()) {
                lines.add(fl);
            }
        }

        return lines;
    }

    /**
     * Compute some key metrics by searching for lines containing specific
     * keywords and taking the first numeric values found.
     */
    private static Map<String, Double> computeMetrics(String fullText, List<FinancialLine> financialLines) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        metrics.put("Turnover", findMetric(financialLines, "turnover", "sales"));
        metrics.put("Profit for the year", findMetric(financialLines, "profit for the financial year", "profit"));
        metrics.put("Tangible assets", findMetric(financialLines, "tangible assets", "tangible"));
        metrics.put("Stocks", findMetric(financialLines, "stocks", "inventory", "inventories"));
        metrics.put("Debtors", findMetric(financialLines, "debtors", "trade debtors"));
        metrics.put("Cash", findMetric(financialLines, "cash at bank", "cash at bank and in hand", "cash"));
        metrics.put("Net assets", findMetric(financialLines, "net assets"));

        return metrics;
    }

    /**
     * Look through financial lines for one whose label contains a given
     * keyword; return the first numeric value from that line.
     */
    private static Double findMetric(List<FinancialLine> lines, String... keywords) {
        for (String kw : keywords) {
            String kwLower = kw.toLowerCase();
            for (FinancialLine fl : lines) {
                if (fl.label != null && fl.label.toLowerCase().contains(kwLower)) {
                    if (!fl.values.isEmpty()) {
                        return fl.values.get(0); // take first value as "current"
                    }
                }
            }
        }
        return null;
    }

    /**
     * Write an Excel workbook: - Sheet 1: Narrative (sentences) - Sheet 2:
     * FinancialLines - Sheet 3: Summary metrics
     */
    private static void writeExcel(List<String> sentences,
            List<FinancialLine> financialLines,
            Map<String, Double> metrics,
            String outputPath) throws IOException {
        Workbook wb = new XSSFWorkbook();

        // Sheet 1: Narrative
        Sheet sNarr = wb.createSheet("Narrative");
        int rowIdx = 0;
        Row headerNarr = sNarr.createRow(rowIdx++);
        headerNarr.createCell(0).setCellValue("Sentence");
        for (String sentence : sentences) {
            Row r = sNarr.createRow(rowIdx++);
            r.createCell(0).setCellValue(sentence);
        }

        // Sheet 2: Financial Lines
        Sheet sFin = wb.createSheet("FinancialLines");
        rowIdx = 0;
        Row headerFin = sFin.createRow(rowIdx++);
        headerFin.createCell(0).setCellValue("Raw Line");
        headerFin.createCell(1).setCellValue("Label");
        headerFin.createCell(2).setCellValue("Values (comma separated)");

        for (FinancialLine fl : financialLines) {
            Row r = sFin.createRow(rowIdx++);
            r.createCell(0).setCellValue(fl.rawLine);
            r.createCell(1).setCellValue(fl.label);
            r.createCell(2).setCellValue(fl.values.toString());
        }

        // Sheet 3: Summary
        Sheet sSum = wb.createSheet("Summary");
        rowIdx = 0;
        Row headerSum = sSum.createRow(rowIdx++);
        headerSum.createCell(0).setCellValue("Metric");
        headerSum.createCell(1).setCellValue("Value");

        for (Map.Entry<String, Double> e : metrics.entrySet()) {
            Row r = sSum.createRow(rowIdx++);
            r.createCell(0).setCellValue(e.getKey());
            if (e.getValue() != null) {
                r.createCell(1).setCellValue(e.getValue());
            } else {
                r.createCell(1).setCellValue("N/A");
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            wb.write(fos);
        }
        wb.close();
    }

    /**
     * Write a simple PDF summary using OpenPDF: - Title - Key metrics paragraph
     * - Some sample sentences from narrative
     */
    private static void writeSummaryPdf(Map<String, Double> metrics,
            List<String> sentences,
            String outputPath)
            throws FileNotFoundException, DocumentException {

        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(outputPath));
        doc.open();

        doc.add(new Paragraph("Financial Summary Report"));
        doc.add(new Paragraph(" ")); // blank line

        // Metrics Section
        doc.add(new Paragraph("Key Metrics:"));
        for (Map.Entry<String, Double> e : metrics.entrySet()) {
            String line = "  - " + e.getKey() + ": " + formatCurrency(e.getValue());
            doc.add(new Paragraph(line));
        }

        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Narrative Extract:"));

        int count = 0;
        for (String s : sentences) {
            doc.add(new Paragraph("  * " + s));
            if (++count >= 10) {
                break; // only first 10 sentences

            }
        }

        doc.close();
    }

    private static String formatCurrency(Double v) {
        if (v == null) {
            return "N/A";
        }
        return String.format("€%,.0f", v);
    }
}
