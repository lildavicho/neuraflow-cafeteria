package com.ucacue.bar.erp.accounting.infrastructure.sri;

import com.ucacue.bar.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.net.URL;

@Service
public class SriXmlValidationService {

    private static final String FACTURA_V110_XSD = "sri/xsd/factura_V1.1.0.xsd";

    public void validateFacturaV110(String xml) {
        try {
            URL schemaUrl = Thread.currentThread().getContextClassLoader().getResource(FACTURA_V110_XSD);
            if (schemaUrl == null) {
                throw new BadRequestException("No esta disponible el XSD oficial factura_V1.1.0.xsd");
            }

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaUrl);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (BadRequestException ex) {
            throw ex;
        } catch (SAXException ex) {
            throw new BadRequestException("XML SRI invalido contra XSD factura v1.1.0: " + ex.getMessage());
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo validar el XML SRI contra el XSD oficial");
        }
    }
}
