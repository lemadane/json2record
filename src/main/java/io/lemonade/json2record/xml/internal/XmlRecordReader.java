package io.lemonade.json2record.xml.internal;

import io.lemonade.json2record.ExcessDataException;

import io.lemonade.json2record.MissingDataException;
import io.lemonade.json2record.TypeConversionException;
import io.lemonade.json2record.XmlMappingException;
import io.lemonade.json2record.convert.DefaultValueProvider;
import io.lemonade.json2record.convert.TypeConverter;
import io.lemonade.json2record.naming.DataNameCodec;
import io.lemonade.json2record.reflect.RecordFactory;
import io.lemonade.json2record.reflect.RecordIntrospector;
import io.lemonade.json2record.reflect.RecordMetadata;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Internal XML reader that parses XML into Java records with strict or partial mode.
 */
public final class XmlRecordReader {

    private XmlRecordReader() {
    }

    /**
     * Parses XML into a record instance.
     *
     * @param recordType target record class.
     * @param xml XML text.
     * @param strict true for strict mode, false for partial mode.
     * @param <T> record type.
     * @return parsed record instance.
     */
    public static <T extends Record> T parse(Class<T> recordType, String xml, boolean strict) {
        if (recordType == null) {
            throw new NullPointerException("recordType must not be null");
        }
        if (xml == null) {
            throw new NullPointerException("xml must not be null");
        }

        RecordMetadata metadata = RecordIntrospector.inspect(recordType);
        Document doc = parseXmlDocument(xml);
        Element rootElement = doc.getDocumentElement();

        // Validate root element name
        String rootTagName = getXmlNodeName(rootElement);
        String decodedRootTagName = DataNameCodec.decode(rootTagName);

        if (!decodedRootTagName.equals(metadata.decodedRecordName())
                && !rootTagName.equals(metadata.recordSimpleName())
                && !decodedRootTagName.equals(metadata.recordSimpleName())) {
            throw new XmlMappingException(
                    "Root XML element <" + rootTagName + "> does not match requested record type "
                            + metadata.recordType().getName() + " (expected <" + metadata.recordSimpleName() + ">)"
            );
        }

        return readRecord(metadata, rootElement, "/" + rootTagName, strict);
    }

    private static Document parseXmlDocument(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new XmlMappingException("Failed to parse XML document: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Record> T readRecord(RecordMetadata metadata, Element element, String path, boolean strict) {
        // 1. Process XML attributes into static fields on the record
        processAttributes(metadata, element, path);

        // 2. Collect child elements by decoded tag name
        Map<String, List<Element>> childElementsMap = new HashMap<>();
        List<Element> allChildElements = new ArrayList<>();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                String tagName = getXmlNodeName(child);
                String decodedTagName = DataNameCodec.decode(tagName);

                childElementsMap.computeIfAbsent(decodedTagName, k -> new ArrayList<>()).add(child);
                allChildElements.add(child);
            } else if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                String text = node.getNodeValue();
                if (text != null && !text.trim().isEmpty() && metadata.components().isEmpty()) {
                    // Mixed content error for empty record
                    if (strict) {
                        throw new XmlMappingException("Mixed content cannot be mapped safely at path " + path);
                    }
                }
            }
        }

        // 3. Strict mode check: Excess elements
        if (strict) {
            Set<String> declaredComponentDecodedNames = new HashSet<>();
            for (RecordMetadata.ComponentMetadata comp : metadata.components()) {
                declaredComponentDecodedNames.add(comp.decodedName());
                declaredComponentDecodedNames.add(comp.name());
            }

            for (Element child : allChildElements) {
                String tagName = getXmlNodeName(child);
                String decodedTagName = DataNameCodec.decode(tagName);

                if (!declaredComponentDecodedNames.contains(decodedTagName) && !declaredComponentDecodedNames.contains(tagName)) {
                    throw new ExcessDataException(
                            "Excess XML element <" + tagName + "> at " + path + ". No matching record component exists in " + metadata.recordType().getName()
                    );
                }
            }
        }

        // 4. Map values for each component
        Object[] args = new Object[metadata.components().size()];

        for (int i = 0; i < metadata.components().size(); i++) {
            RecordMetadata.ComponentMetadata comp = metadata.components().get(i);
            String compPath = path + "/" + comp.name();

            List<Element> matchingElements = childElementsMap.get(comp.decodedName());
            if (matchingElements == null || matchingElements.isEmpty()) {
                matchingElements = childElementsMap.get(comp.name());
            }

            if (matchingElements == null || matchingElements.isEmpty()) {
                if (strict) {
                    throw new MissingDataException(
                            "Missing XML element <" + comp.decodedName() + "> required by "
                                    + metadata.recordType().getName() + "." + comp.name() + " at " + path
                    );
                } else {
                    args[i] = DefaultValueProvider.getDefaultValue(comp);
                }
            } else if (comp.isList()) {
                // List component
                Class<?> elemType = comp.listElementType();
                List<Object> listValues = new ArrayList<>();
                for (Element matchingElem : matchingElements) {
                    Object elemValue = readValue(elemType, comp.genericType(), matchingElem, compPath, strict);
                    listValues.add(elemValue);
                }
                args[i] = List.copyOf(listValues);
            } else {
                // Non-list component
                if (matchingElements.size() > 1) {
                    throw new XmlMappingException(
                            "Multiple XML elements <" + comp.decodedName() + "> mapped to non-List component "
                                    + metadata.recordType().getName() + "." + comp.name() + " at " + path
                    );
                }
                Element matchingElem = matchingElements.get(0);
                args[i] = readValue(comp.type(), comp.genericType(), matchingElem, compPath, strict);
            }
        }

        return RecordFactory.createRecord(metadata, args);
    }

    private static Object readValue(Class<?> targetType, java.lang.reflect.Type genericType, Element elem, String path, boolean strict) {
        String tagName = getXmlNodeName(elem);

        if (targetType.isRecord()) {
            RecordMetadata nestedMeta = RecordIntrospector.inspect((Class<? extends Record>) targetType);
            return readRecord(nestedMeta, elem, path, strict);
        }

        if (Optional.class.isAssignableFrom(targetType)) {
            Class<?> valueType = RecordMetadata.ComponentMetadata.class.cast(
                    new RecordMetadata.ComponentMetadata("opt", "opt", targetType, genericType, null)
            ).optionalValueType();

            if (isEmptyElement(elem)) {
                return Optional.empty();
            }
            String text = elem.getTextContent();
            if (text == null || text.isEmpty()) {
                return Optional.empty();
            }
            Object val = TypeConverter.convert(valueType, text, path);
            return Optional.of(val);
        }

        if (isEmptyElement(elem)) {
            if (targetType == String.class) {
                return "";
            }
            if (strict) {
                throw new TypeConversionException("Empty XML element <" + tagName + "> cannot be mapped to type " + targetType.getName() + " at " + path);
            } else {
                if (targetType.isPrimitive()) {
                    throw new TypeConversionException("Empty XML element <" + tagName + "> cannot be mapped to primitive type " + targetType.getName() + " at " + path);
                }
                return null;
            }
        }

        String textContent = elem.getTextContent();
        return TypeConverter.convert(targetType, textContent, path);
    }

    private static boolean isEmptyElement(Element elem) {
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return false;
            }
            if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                if (!n.getNodeValue().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void processAttributes(RecordMetadata metadata, Element element, String path) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes == null) {
            return;
        }

        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            String rawAttrName = attr.getName();
            String decodedAttrName = DataNameCodec.decode(rawAttrName);

            Field staticField = metadata.staticFieldsByDecodedName().get(decodedAttrName);
            if (staticField == null) {
                staticField = metadata.staticFieldsByDecodedName().get(rawAttrName);
            }

            if (staticField != null) {
                if (Modifier.isFinal(staticField.getModifiers())) {
                    throw new XmlMappingException(
                            "Cannot assign XML attribute " + rawAttrName + " to final static field "
                                    + staticField.getName() + " in " + metadata.recordType().getName() + " at " + path
                    );
                }
                try {
                    Object convertedValue = TypeConverter.convert(staticField.getType(), attr.getValue(), path + "@" + rawAttrName);
                    staticField.set(null, convertedValue);
                } catch (IllegalAccessException e) {
                    throw new XmlMappingException(
                            "Access to static field " + staticField.getName() + " in " + metadata.recordType().getName()
                                    + " is blocked. Package may need to be opened to record-data library.", e
                    );
                }
            }
        }
    }

    private static String getXmlNodeName(Element element) {
        String tagName = element.getTagName();
        return tagName;
    }
}
