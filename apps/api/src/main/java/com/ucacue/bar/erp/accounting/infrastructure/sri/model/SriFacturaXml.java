package com.ucacue.bar.erp.accounting.infrastructure.sri.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "factura")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"infoTributaria", "infoFactura", "detalles", "infoAdicional"})
@Getter
@Setter
@NoArgsConstructor
public class SriFacturaXml {

    @XmlAttribute
    private String id = "comprobante";

    @XmlAttribute
    private String version = "1.1.0";

    @XmlElement(required = true)
    private InfoTributaria infoTributaria;

    @XmlElement(required = true)
    private InfoFactura infoFactura;

    @XmlElement(required = true)
    private Detalles detalles;

    @XmlElement
    private InfoAdicional infoAdicional;

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {
            "ambiente",
            "tipoEmision",
            "razonSocial",
            "nombreComercial",
            "ruc",
            "claveAcceso",
            "codDoc",
            "estab",
            "ptoEmi",
            "secuencial",
            "dirMatriz",
            "agenteRetencion",
            "contribuyenteRimpe"
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InfoTributaria {
        private String ambiente;
        private String tipoEmision;
        private String razonSocial;
        private String nombreComercial;
        private String ruc;
        private String claveAcceso;
        private String codDoc;
        private String estab;
        private String ptoEmi;
        private String secuencial;
        private String dirMatriz;
        private String agenteRetencion;
        private String contribuyenteRimpe;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {
            "fechaEmision",
            "dirEstablecimiento",
            "contribuyenteEspecial",
            "obligadoContabilidad",
            "tipoIdentificacionComprador",
            "razonSocialComprador",
            "identificacionComprador",
            "totalSinImpuestos",
            "totalDescuento",
            "totalConImpuestos",
            "propina",
            "importeTotal",
            "moneda",
            "pagos"
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InfoFactura {
        private String fechaEmision;
        private String dirEstablecimiento;
        private String contribuyenteEspecial;
        private String obligadoContabilidad;
        private String tipoIdentificacionComprador;
        private String razonSocialComprador;
        private String identificacionComprador;
        private String totalSinImpuestos;
        private String totalDescuento;
        private TotalConImpuestos totalConImpuestos;
        private String propina;
        private String importeTotal;
        private String moneda;
        private Pagos pagos;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TotalConImpuestos {
        @XmlElement(name = "totalImpuesto", required = true)
        private List<TotalImpuesto> totalImpuesto = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"codigo", "codigoPorcentaje", "descuentoAdicional", "baseImponible", "tarifa", "valor"})
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalImpuesto {
        private String codigo;
        private String codigoPorcentaje;
        private String descuentoAdicional;
        private String baseImponible;
        private String tarifa;
        private String valor;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Pagos {
        @XmlElement(name = "pago", required = true)
        private List<Pago> pago = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"formaPago", "total", "plazo", "unidadTiempo"})
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pago {
        private String formaPago;
        private String total;
        private String plazo;
        private String unidadTiempo;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Detalles {
        @XmlElement(name = "detalle", required = true)
        private List<Detalle> detalle = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {
            "codigoPrincipal",
            "codigoAuxiliar",
            "descripcion",
            "unidadMedida",
            "cantidad",
            "precioUnitario",
            "precioSinSubsidio",
            "descuento",
            "precioTotalSinImpuesto",
            "detallesAdicionales",
            "impuestos"
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detalle {
        private String codigoPrincipal;
        private String codigoAuxiliar;
        private String descripcion;
        private String unidadMedida;
        private String cantidad;
        private String precioUnitario;
        private String precioSinSubsidio;
        private String descuento;
        private String precioTotalSinImpuesto;
        private DetallesAdicionales detallesAdicionales;
        private Impuestos impuestos;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class DetallesAdicionales {
        @XmlElement(name = "detAdicional")
        private List<DetalleAdicional> detAdicional = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleAdicional {
        @XmlAttribute
        private String nombre;

        @XmlAttribute
        private String valor;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Impuestos {
        @XmlElement(name = "impuesto", required = true)
        private List<Impuesto> impuesto = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"codigo", "codigoPorcentaje", "tarifa", "baseImponible", "valor"})
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Impuesto {
        private String codigo;
        private String codigoPorcentaje;
        private String tarifa;
        private String baseImponible;
        private String valor;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class InfoAdicional {
        @XmlElement(name = "campoAdicional", required = true)
        private List<CampoAdicional> campoAdicional = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampoAdicional {
        @XmlAttribute
        private String nombre;

        @XmlValue
        private String value;
    }
}
