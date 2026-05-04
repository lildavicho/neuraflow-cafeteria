package com.ucacue.bar.service;

import com.ucacue.bar.dto.dashboard.BusinessInsightDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InsightEngineService {

    private static final int MAX_INSIGHTS = 5;

    public List<BusinessInsightDTO> buildInsights(BigDecimal ventasHoy,
                                                  BigDecimal gananciasHoy,
                                                  Map<String, Object> comparacionVentas,
                                                  Map<String, Object> comparacionGanancias,
                                                  Map<String, Object> productoEstrella,
                                                  Map<String, Object> productoConPerdida,
                                                  List<Map<String, Object>> productosBajoStock,
                                                  long pagosPendientes) {
        List<BusinessInsightDTO> insights = new ArrayList<>();
        BigDecimal deltaVentas = asBigDecimal(comparacionVentas.get("deltaMonto"));
        BigDecimal deltaGanancias = asBigDecimal(comparacionGanancias.get("deltaMonto"));

        BusinessInsightDTO marginInsight = buildMarginInsight(ventasHoy, gananciasHoy, productoConPerdida);
        if (marginInsight != null) {
            insights.add(marginInsight);
        }

        BusinessInsightDTO salesInsight = buildSalesInsight(ventasHoy, deltaVentas, deltaGanancias);
        if (salesInsight != null) {
            insights.add(salesInsight);
        }

        BusinessInsightDTO rotationInsight = buildRotationInsight(productoEstrella);
        if (rotationInsight != null) {
            insights.add(rotationInsight);
        }

        BusinessInsightDTO inventoryInsight = buildInventoryInsight(productoEstrella, productosBajoStock);
        if (inventoryInsight != null) {
            insights.add(inventoryInsight);
        }

        BusinessInsightDTO paymentInsight = buildPaymentInsight(pagosPendientes);
        if (paymentInsight != null) {
            insights.add(paymentInsight);
        }

        if (insights.isEmpty()) {
            insights.add(new BusinessInsightDTO(
                    "Aun no hay una decision clara",
                    "Todavia no existe suficiente actividad pagada para emitir una recomendacion confiable.",
                    "Evitas reaccionar por intuicion cuando el negocio aun no marca una tendencia real.",
                    "Sigue registrando ventas pagadas y vuelve a revisar el panel cuando haya mas movimiento.",
                    "neutral",
                    "estado"));
        }

        return insights.stream()
                .limit(MAX_INSIGHTS)
                .toList();
    }

    private BusinessInsightDTO buildMarginInsight(BigDecimal ventasHoy,
                                                  BigDecimal gananciasHoy,
                                                  Map<String, Object> productoConPerdida) {
        if (productoConPerdida != null) {
            String nombre = asString(productoConPerdida.get("name"), "Este producto");
            return new BusinessInsightDTO(
                    "Estas perdiendo dinero aqui",
                    nombre + " se esta vendiendo sin dejar margen saludable. Cada salida debilita la utilidad del dia.",
                    "Si lo sigues empujando asi, la caja puede moverse y aun asi cerrar peor.",
                    "Revisa precio, costo o porcion antes de volver a promoverlo.",
                    "danger",
                    "margen");
        }

        if (ventasHoy.compareTo(BigDecimal.ZERO) > 0 && gananciasHoy.compareTo(BigDecimal.ZERO) <= 0) {
            return new BusinessInsightDTO(
                    "La venta no se esta convirtiendo en utilidad",
                    "Hoy entra dinero, pero la mezcla actual no esta dejando una ganancia sana.",
                    "Puedes vender y aun asi terminar el dia con una operacion fragil.",
                    "Empuja productos con mejor margen y revisa costos antes de seguir el mismo ritmo.",
                    "danger",
                    "margen");
        }

        return null;
    }

    private BusinessInsightDTO buildSalesInsight(BigDecimal ventasHoy,
                                                 BigDecimal deltaVentas,
                                                 BigDecimal deltaGanancias) {
        if (ventasHoy.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        if (deltaVentas.compareTo(BigDecimal.ZERO) < 0) {
            return new BusinessInsightDTO(
                    "Vendiste menos que ayer",
                    "El negocio arranco con menos traccion que el dia anterior y eso ya esta cambiando el ritmo de caja.",
                    "Si no corriges temprano, el cierre del dia puede quedar mas debil.",
                    "Refuerza el producto mas facil de vender y activa un incentivo corto desde caja.",
                    "warning",
                    "ventas");
        }

        if (deltaVentas.compareTo(BigDecimal.ZERO) > 0 && deltaGanancias.compareTo(BigDecimal.ZERO) >= 0) {
            return new BusinessInsightDTO(
                    "La caja se esta moviendo mejor",
                    "Hoy la venta viene mas firme y la utilidad acompana el ritmo.",
                    "Tienes margen para acelerar sin perder control del negocio.",
                    "Manten visible el producto ganador y evita quedarte sin stock en hora pico.",
                    "positive",
                    "ventas");
        }

        return null;
    }

    private BusinessInsightDTO buildRotationInsight(Map<String, Object> productoEstrella) {
        if (productoEstrella == null) {
            return null;
        }

        String nombre = asString(productoEstrella.get("name"), "Este producto");
        return new BusinessInsightDTO(
                "Este producto rota mucho",
                nombre + " esta moviendo la venta con consistencia y funciona como ancla comercial.",
                "Cuando un producto rota asi, conviene protegerlo porque puede empujar otras compras.",
                "Ponlo primero en caja, asegurale reposicion y usalo como recomendacion por defecto.",
                "positive",
                "producto");
    }

    private BusinessInsightDTO buildInventoryInsight(Map<String, Object> productoEstrella,
                                                     List<Map<String, Object>> productosBajoStock) {
        if (productosBajoStock == null || productosBajoStock.isEmpty()) {
            return null;
        }

        Long productoEstrellaId = asLong(productoEstrella != null ? productoEstrella.get("id") : null);
        for (Map<String, Object> product : productosBajoStock) {
            if (productoEstrellaId != null && productoEstrellaId.equals(asLong(product.get("id")))) {
                String nombre = asString(product.get("name"), "Tu producto ganador");
                return new BusinessInsightDTO(
                        "Tu producto ganador puede frenarse",
                        nombre + " funciona bien, pero su disponibilidad ya esta comprometiendo la continuidad de venta.",
                        "Puedes perder ventas faciles justo en el producto que mejor responde.",
                        "Repone ese producto antes de seguir empujandolo desde caja.",
                        "warning",
                        "inventario");
            }
        }

        String nombre = asString(productosBajoStock.get(0).get("name"), "Un producto clave");
        return new BusinessInsightDTO(
                "Hay una reposicion que no deberias postergar",
                nombre + " ya esta entrando en una zona donde puede cortar ventas evitables.",
                "Un quiebre pequeno termina afectando la experiencia y la continuidad de compra.",
                "Revisa inventario ahora y prioriza reposicion antes del siguiente pico.",
                "warning",
                "inventario");
    }

    private BusinessInsightDTO buildPaymentInsight(long pagosPendientes) {
        if (pagosPendientes <= 0) {
            return null;
        }

        return new BusinessInsightDTO(
                "Tienes ventas listas sin cerrar",
                "Hay cobros por transferencia que siguen abiertos y eso deja caja y reportes a medio camino.",
                "Mientras no cierres esos pagos, la operacion luce mas lenta de lo que realmente es.",
                "Confirma o descarta esas transferencias ahora para limpiar el cierre del dia.",
                "info",
                "pagos");
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String asString(Object value, String fallback) {
        return value != null ? value.toString() : fallback;
    }
}
