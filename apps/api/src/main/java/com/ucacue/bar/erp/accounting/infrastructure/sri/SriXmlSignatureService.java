package com.ucacue.bar.erp.accounting.infrastructure.sri;

import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import xades4j.algorithms.EnvelopedSignatureTransform;
import xades4j.production.BasicSignatureOptions;
import xades4j.production.DataObjectReference;
import xades4j.production.SignatureAlgorithms;
import xades4j.production.SignedDataObjects;
import xades4j.production.SigningCertificateMode;
import xades4j.production.XadesBesSigningProfile;
import xades4j.production.XadesSigner;
import xades4j.properties.DataObjectDesc;
import xades4j.properties.DataObjectFormatProperty;
import xades4j.providers.impl.DirectKeyingDataProvider;

@Service
@RequiredArgsConstructor
public class SriXmlSignatureService {

    private final SriProperties sriProperties;

    public String sign(String unsignedXml) {
        return sign(unsignedXml, credentialsFromProperties());
    }

    public String sign(String unsignedXml, SigningCredentials credentials) {
        String mode = normalizeMode(credentials != null ? credentials.mode() : null);
        return switch (mode) {
            case "NONE" -> unsignedXml;
            case "PRESIGNED" -> requirePreSignedXml(unsignedXml);
            case "PKCS12" -> signWithPkcs12(unsignedXml, credentials);
            default -> throw new BadRequestException("Modo de firma SRI no soportado: " + mode);
        };
    }

    public void validatePkcs12(byte[] pkcs12Content, String pkcs12Password, String keyAlias) {
        try {
            loadSigningMaterial(pkcs12Content, pkcs12Password, keyAlias);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo leer la firma PKCS12 del tenant");
        }
    }

    private String requirePreSignedXml(String xml) {
        if (!xml.contains(":Signature") && !xml.contains("<Signature")) {
            throw new BadRequestException("El modo PRESIGNED requiere un XML ya firmado antes del envio al SRI");
        }
        return xml;
    }

    private String signWithPkcs12(String unsignedXml, SigningCredentials credentials) {
        try {
            SigningMaterial signingMaterial = loadSigningMaterial(
                    credentials != null ? credentials.pkcs12Content() : null,
                    credentials != null ? credentials.pkcs12Password() : null,
                    credentials != null ? credentials.keyAlias() : null
            );
            DirectKeyingDataProvider keyingDataProvider = new DirectKeyingDataProvider(
                    signingMaterial.certificate(),
                    signingMaterial.privateKey()
            );

            BasicSignatureOptions signatureOptions = new BasicSignatureOptions()
                    .includeSigningCertificate(SigningCertificateMode.SIGNING_CERTIFICATE)
                    .includeIssuerSerial(true)
                    .includeSubjectName(true)
                    .includePublicKey(true);
            SignatureAlgorithms algorithms = new SignatureAlgorithms();
            Document document = parse(unsignedXml);

            DataObjectDesc signedObject = new DataObjectReference("")
                    .withTransform(new EnvelopedSignatureTransform())
                    .withDataObjectFormat(new DataObjectFormatProperty("text/xml"));
            XadesSigner signer = new XadesBesSigningProfile(keyingDataProvider)
                    .withBasicSignatureOptions(signatureOptions)
                    .withSignatureAlgorithms(algorithms)
                    .newSigner();
            signer.sign(new SignedDataObjects(signedObject), document.getDocumentElement());
            return toString(document);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo firmar el XML tributario con XAdES-BES y el certificado configurado");
        }
    }

    private SigningCredentials credentialsFromProperties() {
        SriProperties.Signature signature = sriProperties.getSignature();
        String mode = normalizeMode(signature.getMode());
        if (!"PKCS12".equals(mode)) {
            return new SigningCredentials(mode, null, null, signature.getKeyAlias());
        }
        if (signature.getPkcs12Path() == null || signature.getPkcs12Path().isBlank()
                || signature.getPkcs12Password() == null) {
            throw new BadRequestException("La firma PKCS12 requiere path y password configurados");
        }
        Path pkcs12Path = Path.of(signature.getPkcs12Path());
        if (!Files.isRegularFile(pkcs12Path)) {
            throw new BadRequestException("No existe el archivo PKCS12 configurado para firma SRI");
        }
        try {
            return new SigningCredentials(mode, Files.readAllBytes(pkcs12Path), signature.getPkcs12Password(), signature.getKeyAlias());
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo leer el archivo PKCS12 configurado para firma SRI");
        }
    }

    private SigningMaterial loadSigningMaterial(byte[] pkcs12Content, String pkcs12Password, String keyAlias) throws Exception {
        if (pkcs12Content == null || pkcs12Content.length == 0 || pkcs12Password == null) {
            throw new BadRequestException("La firma PKCS12 requiere certificado y password del tenant");
        }
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password = pkcs12Password.toCharArray();
        try {
            keyStore.load(new ByteArrayInputStream(pkcs12Content), password);

            String alias = resolveKeyAlias(keyStore, keyAlias);
            Key key = keyStore.getKey(alias, password);
            if (!(key instanceof PrivateKey privateKey)) {
                throw new BadRequestException("El alias PKCS12 no contiene una llave privada valida");
            }
            Certificate certificate = keyStore.getCertificate(alias);
            if (!(certificate instanceof X509Certificate x509Certificate)) {
                throw new BadRequestException("El alias PKCS12 no contiene un certificado X509 valido");
            }
            return new SigningMaterial(x509Certificate, privateKey);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private String resolveKeyAlias(KeyStore keyStore, String requestedAlias) throws Exception {
        if (requestedAlias != null && !requestedAlias.isBlank()) {
            String normalizedAlias = requestedAlias.trim();
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if ((normalizedAlias.equals(alias) || normalizedAlias.equalsIgnoreCase(alias))
                        && keyStore.isKeyEntry(alias)) {
                    return alias;
                }
            }
            throw new BadRequestException("No existe el alias configurado dentro del PKCS12 SRI");
        }

        List<String> keyAliases = new ArrayList<>();
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                keyAliases.add(alias);
            }
        }
        if (keyAliases.isEmpty()) {
            throw new BadRequestException("El PKCS12 no contiene llaves privadas para firmar");
        }
        if (keyAliases.size() > 1) {
            throw new BadRequestException("El PKCS12 contiene multiples llaves; configura el alias de firma SRI");
        }
        return keyAliases.get(0);
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String toString(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private String normalizeMode(String mode) {
        return (mode == null || mode.isBlank())
                ? "NONE"
                : mode.trim().toUpperCase(Locale.ROOT);
    }

    public record SigningCredentials(
            String mode,
            byte[] pkcs12Content,
            String pkcs12Password,
            String keyAlias) {
    }

    private record SigningMaterial(
            X509Certificate certificate,
            PrivateKey privateKey) {
    }
}
