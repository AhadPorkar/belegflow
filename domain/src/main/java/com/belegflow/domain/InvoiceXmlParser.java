package com.belegflow.domain;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.namespace.NamespaceContext;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
/**
 * Secure extraction of a deliberately bounded UBL Invoice / UN/CEFACT CII subset.
 * This is NOT an XRechnung, EN16931 or ZUGFeRD conformance validator.
 * No external resources, DTDs, entities or XML inclusion are permitted.
 */
public final class InvoiceXmlParser {
    public record Parsed(String format, String number, String supplier, LocalDate issueDate, LocalDate dueDate, String currency, BigDecimal net, BigDecimal tax, BigDecimal gross, String description, List<String> checks) {
    }
    private static final String UBL = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    private static final String CII = "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100";
    public Parsed parse(byte[] bytes) {
        BusinessException.require(bytes != null && bytes.length <= FilePolicy.MAX_BYTES && bytes.length > 0, "INVALID_FILE");
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var b = f.newDocumentBuilder();
            b.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXParseException {
                    throw e;
                }
                public void error(SAXParseException e) throws SAXParseException {
                    throw e;
                }
                public void fatalError(SAXParseException e) throws SAXParseException {
                    throw e;
                }
            });
            Document d = b.parse(new ByteArrayInputStream(bytes));
            var xpath = XPathFactory.newInstance().newXPath();
            Map<String,String> ns = Map.of("inv",UBL, "cbc","urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2", "cac","urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2", "rsm",CII,"ram","urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100", "udt","urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100");
            xpath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    return ns.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
                }
                public String getPrefix(String uri) {
                    return ns.entrySet().stream().filter(e->e.getValue().equals(uri)).map(Map.Entry::getKey).findFirst().orElse(null);
                }
                public Iterator<String> getPrefixes(String uri) {
                    return ns.entrySet().stream().filter(e->e.getValue().equals(uri)).map(Map.Entry::getKey).iterator();
                }
            });
            java.util.function.Function<String,String> text = path -> {
                try {
                    String v = xpath.evaluate(path,d).strip();
                    BusinessException.require(v.length() <= 2000, "INVALID_XML");
                    return v;
                } catch (javax.xml.xpath.XPathExpressionException e) {
                    throw new BusinessException("INVALID_XML");
                }
            };
            String format, number, supplier, issue, due, currency, net, tax, gross, description;
            if (UBL.equals(d.getDocumentElement().getNamespaceURI()) && "Invoice".equals(d.getDocumentElement().getLocalName())) {
                format="UBL";
                number=text.apply("/inv:Invoice/cbc:ID");
                supplier=text.apply("/inv:Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName");
                if (supplier.isBlank()) supplier=text.apply("/inv:Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyName/cbc:Name");
                issue=text.apply("/inv:Invoice/cbc:IssueDate");
                due=text.apply("/inv:Invoice/cbc:DueDate");
                currency=text.apply("/inv:Invoice/cbc:DocumentCurrencyCode");
                net=text.apply("/inv:Invoice/cac:LegalMonetaryTotal/cbc:TaxExclusiveAmount");
                gross=text.apply("/inv:Invoice/cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount");
                tax=text.apply("/inv:Invoice/cac:TaxTotal/cbc:TaxAmount");
                description=text.apply("/inv:Invoice/cbc:Note");
                String type=text.apply("/inv:Invoice/cbc:InvoiceTypeCode");
                BusinessException.require(type.isBlank() || type.equals("380"), "UNSUPPORTED_INVOICE_TYPE");
                String payable=text.apply("/inv:Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount");
                BusinessException.require(payable.isBlank() || MoneyRules.amount(payable).compareTo(MoneyRules.amount(gross))==0, "UNSUPPORTED_PREPAYMENT");
            } else if (CII.equals(d.getDocumentElement().getNamespaceURI()) && "CrossIndustryInvoice".equals(d.getDocumentElement().getLocalName())) {
                format="CII";
                String base="/rsm:CrossIndustryInvoice";
                number=text.apply(base+"/rsm:ExchangedDocument/ram:ID");
                supplier=text.apply(base+"/rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:SellerTradeParty/ram:Name");
                issue=text.apply(base+"/rsm:ExchangedDocument/ram:IssueDateTime/udt:DateTimeString");
                String settlement=base+"/rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement";
                due=text.apply(settlement+"/ram:SpecifiedTradePaymentTerms/ram:DueDateDateTime/udt:DateTimeString");
                currency=text.apply(settlement+"/ram:InvoiceCurrencyCode");
                String sums=settlement+"/ram:SpecifiedTradeSettlementHeaderMonetarySummation";
                net=text.apply(sums+"/ram:TaxBasisTotalAmount");
                tax=text.apply(sums+"/ram:TaxTotalAmount");
                gross=text.apply(sums+"/ram:GrandTotalAmount");
                description=text.apply(base+"/rsm:ExchangedDocument/ram:IncludedNote/ram:Content");
                String type=text.apply(base+"/rsm:ExchangedDocument/ram:TypeCode");
                BusinessException.require(type.equals("380"), "UNSUPPORTED_INVOICE_TYPE");
                String payable=text.apply(sums+"/ram:DuePayableAmount");
                BusinessException.require(payable.isBlank() || MoneyRules.amount(payable).compareTo(MoneyRules.amount(gross))==0, "UNSUPPORTED_PREPAYMENT");
            } else throw new BusinessException("UNSUPPORTED_XML");
            BusinessException.require(!number.isBlank() && number.length() <= 120 && !supplier.isBlank() && supplier.length()<=200, "MISSING_INVOICE_FIELDS");
            LocalDate issued = date(issue);
            LocalDate deadline = due.isBlank() ? null : date(due);
            BigDecimal n=MoneyRules.amount(net), t=MoneyRules.amount(tax), g=MoneyRules.amount(gross);
            List<String> checks=new ArrayList<>();
            if (!MoneyRules.validTotals(n,t,g,currency)) checks.add("TOTALS_OR_CURRENCY_INVALID");
            if (deadline == null) checks.add("DUE_DATE_REQUIRED");
            if (deadline != null && deadline.isBefore(issued)) checks.add("DUE_BEFORE_ISSUE");
            // Conformance beyond extraction and these basic checks is deliberately not claimed.
            return new Parsed(format,number,supplier,issued,deadline,currency,n,t,g,description,List.copyOf(checks));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("INVALID_XML");
        }
    }
    private LocalDate date(String value) {
        return value.length()==8 ? LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE) : LocalDate.parse(value);
    }
}
