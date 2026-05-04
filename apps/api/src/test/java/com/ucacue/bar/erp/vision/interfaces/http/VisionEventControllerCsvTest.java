package com.ucacue.bar.erp.vision.interfaces.http;

import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionDetectionReportRow;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisionEventControllerCsvTest {

    @Test
    void csvExportNeutralizesFormulaCells() throws Exception {
        VisionEventController controller = new VisionEventController(null, null, null, null, null);
        Method toCsv = VisionEventController.class.getDeclaredMethod("toCsv", List.class);
        toCsv.setAccessible(true);

        String csv = (String) toCsv.invoke(controller, List.of(new VisionDetectionReportRow(
                "1",
                OffsetDateTime.parse("2026-05-02T18:00:00Z"),
                "CAM-001",
                " =cmd",
                "SUC-001",
                "\t=cmd",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ONE,
                "\r=cmd"),
                new VisionDetectionReportRow(
                        "2",
                        OffsetDateTime.parse("2026-05-02T18:01:00Z"),
                        "CAM-002",
                        "+SUM(1,1)",
                        "SUC-002",
                        "-10+20",
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        "@test")));

        assertThat(csv).contains("\"' =cmd\"");
        assertThat(csv).contains("\"'\t=cmd\"");
        assertThat(csv).contains("\"'\r=cmd\"");
        assertThat(csv).contains("\"'+SUM(1,1)\"");
        assertThat(csv).contains("\"'-10+20\"");
        assertThat(csv).contains("\"'@test\"");
    }
}
