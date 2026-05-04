package com.ucacue.bar.erp.accounting.infrastructure.sri;

import com.ucacue.bar.erp.accounting.infrastructure.sri.model.SriFacturaXml;
import com.ucacue.bar.exception.BadRequestException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@Service
public class SriXmlMarshaller {

    public String marshalFactura(SriFacturaXml factura) {
        try {
            JAXBContext context = JAXBContext.newInstance(SriFacturaXml.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            StringWriter writer = new StringWriter();
            marshaller.marshal(factura, writer);
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + writer;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo generar el XML de factura SRI v1.1.0");
        }
    }
}
