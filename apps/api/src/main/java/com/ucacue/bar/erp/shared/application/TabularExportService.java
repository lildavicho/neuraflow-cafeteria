package com.ucacue.bar.erp.shared.application;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ucacue.bar.exception.BadRequestException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class TabularExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ExportDocument export(String title, List<String> headers, List<List<?>> rows, String format) {
        String normalizedFormat = normalizeFormat(format);
        return switch (normalizedFormat) {
            case "pdf" -> new ExportDocument(exportPdf(title, headers, rows), "application/pdf", "pdf");
            case "csv" -> new ExportDocument(exportCsv(headers, rows), "text/csv; charset=UTF-8", "csv");
            default -> new ExportDocument(
                    exportExcel(title, headers, rows),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx");
        };
    }

    private byte[] exportExcel(String title, List<String> headers, List<List<?>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sanitizeSheetName(title));
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<?> values = rows.get(rowIndex);
                for (int cellIndex = 0; cellIndex < values.size(); cellIndex++) {
                    writeExcelCell(row.createCell(cellIndex), values.get(cellIndex));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el archivo XLSX", ex);
        }
    }

    private byte[] exportCsv(List<String> headers, List<List<?>> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append(toCsvLine(headers));
        for (List<?> row : rows) {
            builder.append('\n').append(toCsvLine(row.stream().map(this::formatValue).toList()));
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportPdf(String title, List<String> headers, List<List<?>> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(title));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(Math.max(headers.size(), 1));
            for (String header : headers) {
                table.addCell(header);
            }
            for (List<?> row : rows) {
                for (Object value : row) {
                    table.addCell(formatValue(value));
                }
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el archivo PDF", ex);
        }
    }

    private void writeExcelCell(Cell cell, Object value) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
            return;
        }
        cell.setCellValue(formatValue(value));
    }

    private String toCsvLine(List<String> values) {
        return values.stream()
                .map(this::escapeCsv)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate date) {
            return date.format(DATE_FORMATTER);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME_FORMATTER);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "xlsx";
        }
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        if ("excel".equals(normalized)) {
            return "xlsx";
        }
        if (!List.of("xlsx", "csv", "pdf").contains(normalized)) {
            throw new BadRequestException("Formato de exportacion no soportado. Usa xlsx, csv o pdf");
        }
        return normalized;
    }

    private String sanitizeSheetName(String title) {
        String safe = title.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (safe.isBlank()) {
            return "Export";
        }
        return safe.length() > 31 ? safe.substring(0, 31) : safe;
    }

    public record ExportDocument(byte[] content, String contentType, String extension) {
    }
}

