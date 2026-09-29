package com.example.demo.report.service;

import com.example.demo.report.dto.response.PeriodTransactionReportResponse;
import com.example.demo.report.dto.response.PeriodTransactionSummary;
import com.example.demo.report.entity.enums.PeriodType;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final StatsService statsService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat VND_FORMATTER = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.US));
    private static final String[] HEADERS = {
            "Kỳ", "Loại kỳ", "Từ ngày", "Đến ngày", "Tổng GD",
            "Nạp tiền", "Rút tiền", "Chuyển khoản", "Phí", "Thực nhận",
            "TB/GD", "Nhỏ nhất", "Lớn nhất",
            "SL nạp", "SL rút", "SL chuyển",
            "Tiền nạp", "Tiền rút", "Tiền chuyển"
    };

    public byte[] exportToExcel(PeriodType periodType, Integer periods,
                                LocalDateTime fromDate, LocalDateTime toDate) {
        PeriodTransactionReportResponse report = statsService.getTransactionReport(
                periodType, periods, fromDate, toDate, 0, Integer.MAX_VALUE);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Báo cáo theo kỳ");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            List<PeriodTransactionSummary> summaries = report.getSummaries();
            for (int rowIdx = 0; rowIdx < summaries.size(); rowIdx++) {
                PeriodTransactionSummary summary = summaries.get(rowIdx);
                Row row = sheet.createRow(rowIdx + 1);
                fillRow(row, summary, currencyStyle, dateStyle, numberStyle);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    public byte[] exportToPdf(PeriodType periodType, Integer periods,
                              LocalDateTime fromDate, LocalDateTime toDate) {
        PeriodTransactionReportResponse report = statsService.getTransactionReport(
                periodType, periods, fromDate, toDate, 0, Integer.MAX_VALUE);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            String periodTypeVn = getPeriodTypeVietnamese(periodType);
            String dateRange = buildDateRangeString(fromDate, toDate, periods, periodType);

            Paragraph title = new Paragraph("BÁO CÁO GIAO DỊCH THEO KỲ")
                    .setFont(fontBold)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    String.format("Loại: %s | %s | Tạo lúc: %s",
                            periodTypeVn, dateRange, LocalDateTime.now().format(DATETIME_FORMATTER))
            )
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(subtitle);

            Table table = new Table(UnitValue.createPercentArray(HEADERS.length))
                    .useAllAvailableWidth();

            for (String header : HEADERS) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(header).setFont(fontBold).setFontSize(7))
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(3);
                table.addHeaderCell(headerCell);
            }

            List<PeriodTransactionSummary> summaries = report.getSummaries();
            for (PeriodTransactionSummary summary : summaries) {
                addRowToTable(table, summary, font);
            }

            document.add(table);

            Paragraph footer = new Paragraph(
                    String.format("Hệ thống ngân hàng demo | Trang %d/%d",
                            pdfDoc.getNumberOfPages(), pdfDoc.getNumberOfPages()))
                    .setFont(font)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void fillRow(Row row, PeriodTransactionSummary summary,
                         CellStyle currencyStyle, CellStyle dateStyle, CellStyle numberStyle) {
        int col = 0;

        row.createCell(col++).setCellValue(summary.getPeriodLabel());
        row.createCell(col++).setCellValue(getPeriodTypeVietnamese(summary.getPeriodType()));
        row.createCell(col++).setCellValue(summary.getPeriodStart().format(DATE_FORMATTER));
        row.getCell(col - 1).setCellStyle(dateStyle);

        row.createCell(col++).setCellValue(summary.getPeriodEnd().format(DATE_FORMATTER));
        row.getCell(col - 1).setCellStyle(dateStyle);

        row.createCell(col++).setCellValue(summary.getTotalTransactions());
        row.getCell(col - 1).setCellStyle(numberStyle);

        setCurrencyCell(row, col++, summary.getDepositAmount(), currencyStyle);
        setCurrencyCell(row, col++, summary.getWithdrawAmount(), currencyStyle);
        setCurrencyCell(row, col++, summary.getTransferAmount(), currencyStyle);
        setCurrencyCell(row, col++, summary.getTotalFees(), currencyStyle);
        setCurrencyCell(row, col++, summary.getTotalNetAmount(), currencyStyle);
        setCurrencyCell(row, col++, summary.getAverageTransactionAmount(), currencyStyle);
        setCurrencyCell(row, col++, summary.getMinTransactionAmount(), currencyStyle);
        setCurrencyCell(row, col++, summary.getMaxTransactionAmount(), currencyStyle);

        row.createCell(col++).setCellValue(summary.getDepositCount() != null ? summary.getDepositCount() : 0);
        row.getCell(col - 1).setCellStyle(numberStyle);
        row.createCell(col++).setCellValue(summary.getWithdrawCount() != null ? summary.getWithdrawCount() : 0);
        row.getCell(col - 1).setCellStyle(numberStyle);
        row.createCell(col++).setCellValue(summary.getTransferCount() != null ? summary.getTransferCount() : 0);
        row.getCell(col - 1).setCellStyle(numberStyle);

        setCurrencyCell(row, col++, summary.getDepositAmount(), currencyStyle);
        setCurrencyCell(row, col++, summary.getWithdrawAmount(), currencyStyle);
        setCurrencyCell(row, col++, summary.getTransferAmount(), currencyStyle);
    }

    private void addRowToTable(Table table, PeriodTransactionSummary summary, PdfFont font) {
        table.addCell(createPdfCell(summary.getPeriodLabel(), font, TextAlignment.CENTER, 7));
        table.addCell(createPdfCell(getPeriodTypeVietnamese(summary.getPeriodType()), font, TextAlignment.CENTER, 7));
        table.addCell(createPdfCell(summary.getPeriodStart().format(DATE_FORMATTER), font, TextAlignment.CENTER, 7));
        table.addCell(createPdfCell(summary.getPeriodEnd().format(DATE_FORMATTER), font, TextAlignment.CENTER, 7));
        table.addCell(createPdfCell(String.valueOf(summary.getTotalTransactions()), font, TextAlignment.CENTER, 7));
        table.addCell(createPdfCell(formatVnd(summary.getDepositAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getWithdrawAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getTransferAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getTotalFees()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getTotalNetAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getAverageTransactionAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getMinTransactionAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getMaxTransactionAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(String.valueOf(summary.getDepositCount() != null ? summary.getDepositCount() : 0), font, TextAlignment.CENTER, 7));
        table.addCell(createPdfCell(String.valueOf(summary.getWithdrawCount() != null ? summary.getWithdrawCount() : 0), font, TextAlignment.CENTER, 7));
        table.addCell(createPdfCell(String.valueOf(summary.getTransferCount() != null ? summary.getTransferCount() : 0), font, TextAlignment.CENTER, 7));
        table.addCell(createPdfCell(formatVnd(summary.getDepositAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getWithdrawAmount()), font, TextAlignment.RIGHT, 7));
        table.addCell(createPdfCell(formatVnd(summary.getTransferAmount()), font, TextAlignment.RIGHT, 7));
    }

    private Cell createPdfCell(String text, PdfFont font, TextAlignment alignment, float fontSize) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(fontSize))
                .setTextAlignment(alignment)
                .setPadding(2);
    }

    private void setCurrencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0);
        }
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("dd/MM/yyyy"));
        style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String formatVnd(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return VND_FORMATTER.format(value) + " ₫";
    }

    private String getPeriodTypeVietnamese(PeriodType periodType) {
        return switch (periodType) {
            case WEEK -> "Tuần";
            case MONTH -> "Tháng";
            case QUARTER -> "Quý";
        };
    }

    private String buildDateRangeString(LocalDateTime fromDate, LocalDateTime toDate,
                                         Integer periods, PeriodType periodType) {
        if (fromDate != null && toDate != null) {
            return String.format("Từ: %s - Đến: %s",
                    fromDate.format(DATE_FORMATTER), toDate.format(DATE_FORMATTER));
        }
        int resolvedPeriods = periods != null && periods > 0 ? periods :
                (periodType == PeriodType.QUARTER ? 4 : 12);
        return String.format("%d %s gần nhất", resolvedPeriods,
                periodType == PeriodType.QUARTER ? "quý" : periodType == PeriodType.MONTH ? "tháng" : "tuần");
    }
}