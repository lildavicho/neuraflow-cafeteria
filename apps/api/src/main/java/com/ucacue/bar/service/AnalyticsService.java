package com.ucacue.bar.service;

import com.ucacue.bar.entity.SaleEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.repository.PeopleCountHourlyRepository;
import com.ucacue.bar.repository.PeopleCountRepository;
import com.ucacue.bar.repository.SaleRepository;
import com.ucacue.bar.repository.VisionHourlyMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final String KEY_RANGE = "range";
    private static final String KEY_SERIES = "series";

    private final SaleRepository saleRepository;
    private final PeopleCountRepository peopleCountRepository;
    private final PeopleCountHourlyRepository peopleCountHourlyRepository;
    private final VisionHourlyMetricRepository visionHourlyMetricRepository;
    private final TenantContextResolver tenantContextResolver;

    @Transactional(readOnly = true)
    public Map<String, Object> sales(LocalDateTime from, LocalDateTime to) {
        Range range = resolveRange(from, to);
        Map<LocalDate, Stats> daily = aggregateSales(range);
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_RANGE, range);
        response.put(KEY_SERIES, daily.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", entry.getKey());
                    point.put("total", entry.getValue().total);
                    point.put("orders", entry.getValue().count);
                    return point;
                })
                .toList());
        response.put("total", daily.values().stream()
                .map(stat -> stat.total)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        response.put("orders", daily.values().stream().mapToLong(stat -> stat.count).sum());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> people(LocalDateTime from, LocalDateTime to) {
        Range range = resolveRange(from, to);
        List<Map<String, Object>> visionSeries = aggregateVisionPeopleByHour(range);
        if (!visionSeries.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put(KEY_RANGE, range);
            response.put(KEY_SERIES, visionSeries);
            return response;
        }

        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        List<Map<String, Object>> series;
        if (range.days() > 3) {
            series = peopleCountHourlyRepository
                    .findByTenantIdAndHourStartBetweenOrderByHourStartAsc(tenantId, range.start(), range.end()).stream()
                    .map(entry -> {
                        Map<String, Object> point = new HashMap<>();
                        point.put("timestamp", entry.getHourStart());
                        point.put("count", entry.getAverageCount());
                        return point;
                    })
                    .toList();
        } else {
            series = peopleCountRepository.findBetweenByTenantId(tenantId, range.start(), range.end()).stream()
                    .map(entry -> {
                        Map<String, Object> point = new HashMap<>();
                        point.put("timestamp", entry.getTimestamp());
                        point.put("count", entry.getCount());
                        return point;
                    })
                    .toList();
        }
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_RANGE, range);
        response.put(KEY_SERIES, series);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> salesVsPeople(LocalDateTime from, LocalDateTime to) {
        Range range = resolveRange(from, to);
        Map<LocalDate, Stats> sales = aggregateSales(range);
        Map<LocalDate, Double> people = aggregateVisionPeople(range);
        if (people.isEmpty()) {
            people = aggregatePeople(range);
        }
        Map<LocalDate, Double> resolvedPeople = people;

        List<Map<String, Object>> series = sales.keySet().stream()
                .sorted()
                .map(date -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", date);
                    point.put("sales", sales.get(date).total);
                    point.put("people", resolvedPeople.getOrDefault(date, 0.0));
                    return point;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put(KEY_RANGE, range);
        response.put(KEY_SERIES, series);
        return response;
    }

    @Transactional(readOnly = true)
    public byte[] export(String format, LocalDateTime from, LocalDateTime to) {
        Range range = resolveRange(from, to);
        if ("pdf".equalsIgnoreCase(format)) {
            return exportPdf(range);
        }
        return exportExcel(range);
    }

    private Map<LocalDate, Stats> aggregateSales(Range range) {
        Map<LocalDate, Stats> aggregated = new TreeMap<>();
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        List<SaleEntity> sales = saleRepository.findByPaidAtBetweenByTenantId(tenantId, range.start(), range.end());
        sales.stream()
                .filter(sale -> sale.getPaidAt() != null)
                .forEach(sale -> {
                    LocalDate date = sale.getPaidAt().toLocalDate();
                    aggregated.computeIfAbsent(date, key -> new Stats())
                            .add(sale.getTotal());
                });
        return aggregated;
    }

    private Map<LocalDate, Double> aggregatePeople(Range range) {
        Map<LocalDate, Double> aggregated = new TreeMap<>();
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        peopleCountRepository.findBetweenByTenantId(tenantId, range.start(), range.end()).forEach(entry -> {
            LocalDate date = entry.getTimestamp().toLocalDate();
            Double count = entry.getCount() != null ? entry.getCount().doubleValue() : 0.0;
            aggregated.merge(date, count, (a, b) -> Double.valueOf((a != null ? a : 0.0) + (b != null ? b : 0.0)));
        });
        return aggregated;
    }

    private List<Map<String, Object>> aggregateVisionPeopleByHour(Range range) {
        Map<LocalDateTime, Long> aggregated = new TreeMap<>();
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        visionHourlyMetricRepository
                .findByTenantIdAndHourStartBetweenOrderByHourStartAsc(tenantId, range.start().truncatedTo(ChronoUnit.HOURS), range.end())
                .forEach(entry -> aggregated.merge(
                        entry.getHourStart(),
                        entry.getUniquePeople() != null ? entry.getUniquePeople().longValue() : 0L,
                        Long::sum));

        return aggregated.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("timestamp", entry.getKey());
                    point.put("count", entry.getValue());
                    return point;
                })
                .toList();
    }

    private Map<LocalDate, Double> aggregateVisionPeople(Range range) {
        Map<LocalDate, Double> aggregated = new TreeMap<>();
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        visionHourlyMetricRepository
                .findByTenantIdAndHourStartBetweenOrderByHourStartAsc(tenantId, range.start().truncatedTo(ChronoUnit.HOURS), range.end())
                .forEach(entry -> {
                    LocalDate date = entry.getHourStart().toLocalDate();
                    double count = entry.getUniquePeople() != null ? entry.getUniquePeople().doubleValue() : 0.0;
                    aggregated.merge(date, count, Double::sum);
                });
        return aggregated;
    }

    private byte[] exportExcel(Range range) {
        Map<LocalDate, Stats> sales = aggregateSales(range);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Ventas");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Fecha");
            header.createCell(1).setCellValue("Total");
            header.createCell(2).setCellValue("Ordenes");

            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Map.Entry<LocalDate, Stats> entry : sales.entrySet()) {
                Row row = sheet.createRow(rowIdx++);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(entry.getKey().format(formatter));
                row.createCell(1).setCellValue(entry.getValue().total.doubleValue());
                row.createCell(2).setCellValue(entry.getValue().count);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el Excel", ex);
        }
    }

    private byte[] exportPdf(Range range) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();
            document.add(new com.lowagie.text.Paragraph("Reporte de Ventas"));
            document.add(new com.lowagie.text.Paragraph("Rango: " + range));
            document.add(new com.lowagie.text.Paragraph("Generado: " + LocalDateTime.now(ZoneId.systemDefault())));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(3);
            table.addCell("Fecha");
            table.addCell("Total");
            table.addCell("Ordenes");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            aggregateSales(range).forEach((date, stats) -> {
                table.addCell(date.format(formatter));
                table.addCell(stats.total.toPlainString());
                table.addCell(String.valueOf(stats.count));
            });

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF", ex);
        }
    }

    private Range resolveRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusDays(30);
        return new Range(start, end);
    }

    private record Range(LocalDateTime start, LocalDateTime end) {
        @Override
        public String toString() {
            return start + " - " + end;
        }

        public long days() {
            return java.time.Duration.between(start, end).toDays();
        }
    }

    private static class Stats {
        private java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        private long count = 0;

        void add(java.math.BigDecimal value) {
            total = total.add(value);
            count++;
        }
    }
}
