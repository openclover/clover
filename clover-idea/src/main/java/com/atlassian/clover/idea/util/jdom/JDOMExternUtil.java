package com.atlassian.clover.idea.util.jdom;

import clover.org.apache.commons.lang3.StringUtils;
import com.atlassian.clover.util.collections.Pair;
import com.intellij.openapi.diagnostic.Logger;
import org.jdom.Document;
import org.jdom.Element;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class JDOMExternUtil {

    private static final String JARJAR_PREFIX = "repkg."; // this must be correlated with build.xml

    private JDOMExternUtil() {
    }

    public static void writeTo(Element root, Object obj) throws Exception {
        if (root == null) {
            throw new IllegalArgumentException("can not write to null root element.");
        }
        Class type = (obj != null) ? obj.getClass() : null;
        if (type != null) {
            setClassAttribute(root, type);
        }
        writeTo(root, type, obj);
    }

    private static void writeTo(Element root, Class type, Object obj) throws Exception {

        // if is basic type ...
        TypeConverter converter = lookupConverter(type);
        if (converter != null) {
            converter.write(root, obj);

            // else if complex object ..., get properties.
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static Object readFrom(Element root) throws Exception {
        if (root == null) {
            throw new IllegalArgumentException("can not read from null root element.");
        }

        Class type = getClassFromElement(root);

        return readFrom(root, type);
    }

    public static void readTo(Element root, Object obj) throws Exception {
        // if is basic type ...
        TypeConverter converter = lookupConverter(obj.getClass());
        if (converter != null) {
            converter.read(root, obj);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static Object readFrom(Element root, Class type) throws Exception {

        // if is basic type ...
        TypeConverter converter = lookupConverter(type);
        if (converter != null) {
            return converter.read(root);

            // else if complex object ..., get properties.
        }
        throw new UnsupportedOperationException();
    }

    private static final TypeConverter[] TYPE_CONVERTERS = {
            new IntegerConverter(),
            new LongConverter(),
            new ShortConverter(),
            new ByteConverter(),
            new DoubleConverter(),
            new FloatConverter(),
            new BooleanConverter(),
            new StringConverter(),
            new ColorConverter(),
            new ColorPairConverter(),
            new MapConverter(),
            new ListConverter(),
            new EnumConverter(),
            new DefaultConverter(),
    };

    private static TypeConverter lookupConverter(Class type) {
        for (TypeConverter converter : TYPE_CONVERTERS) {
            if (converter.canConvert(type)) {
                return converter;
            }
        }
        return null;
    }

    public static String writeToString(Object data) throws Exception {
        return writeToString("root", data);
    }

    public static String writeToString(String rootName, Object data) throws Exception {
        Document d = new Document(new Element(rootName));
        writeTo(d.getRootElement(), data);
        return JDOMUtil.writeDocument(d);
    }

    public static Object readFromString(String str) throws Exception {
        Document doc = JDOMUtil.loadDocument(str);
        Element root = doc.getRootElement();
        return readFrom(root);
    }

    private interface TypeConverter {
        boolean canConvert(Class type);

        void write(Element e, Object o) throws Exception;

        Object read(Element e) throws Exception;

        void read(Element e, Object o) throws Exception;
    }

    private abstract static class BaseConverter implements TypeConverter {

        @Override
        public void write(Element e, Object o) {
            if (o != null) {
                e.setText(o.toString());
            } else {
                e.setAttribute("null", "true");
            }
        }

        protected boolean containsNull(Element e) {
            return e.getAttributeValue("null") != null;
        }

        @Override
        public void read(Element e, Object o) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    private static class ColorConverter implements TypeConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == Color.class;
        }

        @Override
        public void write(Element e, Object o) {
            if (o != null) {
                e.setText("" + ((Color) o).getRGB());
            } else {
                e.setAttribute("null", "true");
            }
        }

        @Override
        public Object read(Element e) {
            if (e.getAttributeValue("null") != null) {
                return null;
            }
            return Color.decode(e.getText().trim());
        }

        @Override
        public void read(Element e, Object o) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Pair<Color, Color>
     */
    private static class ColorPairConverter implements TypeConverter {
        private static final String SEPARATOR = " ";

        @Override
        public boolean canConvert(Class type) {
            return type == Pair.class;
        }

        @Override
        public void write(Element e, Object o) {
            final Pair pair = (Pair)o;
            if (pair == null || pair.first == null) {
                // pair not null, but 'first' color is? mark entire object as null
                e.setAttribute("null", "true");
            } else {
                // write 'first' color at least
                final int firstRGB = ((Color)pair.first).getRGB();
                if (pair.second != null) {
                    final int secondRGB = ((Color)pair.second).getRGB();
                    e.setText(firstRGB + SEPARATOR + secondRGB);
                } else {
                    e.setText(Integer.toString(firstRGB));
                }
            }
        }

        @Override
        public Object read(final Element e) {
            if (e.getAttributeValue("null") != null) {
                return null;
            }
            final String[] colors = StringUtils.split(e.getText().trim(), " \t\n\r");
            final Color firstColor = colors.length > 0 ? Color.decode(colors[0]) : null;
            // in case when second color is missing - use the first one - may be useful for upgrade from an old 1-color format
            final Color secondColor = colors.length > 1 ? Color.decode(colors[1]) : firstColor;
            // both colours are null? return null instead of an empty pair
            if (firstColor == null && secondColor == null) {
                return null;
            }
            return Pair.of(firstColor, secondColor);
        }

        @Override
        public void read(Element e, Object o) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    private static class DoubleConverter extends BaseConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == Double.class || type == Double.TYPE;
        }

        @Override
        public Object read(Element e) {
            if (containsNull(e)) {
                return null;
            }
            return Double.valueOf(e.getText());
        }
    }

    private static class FloatConverter extends BaseConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == Float.class || type == Float.TYPE;
        }

        @Override
        public Object read(Element e) {
            if (containsNull(e)) {
                return null;
            }
            return Float.valueOf(e.getText());
        }
    }

    private static class ShortConverter extends BaseConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == Short.class || type == Short.TYPE;
        }

        @Override
        public Object read(Element e) {
            if (containsNull(e)) {
                return null;
            }
            return Short.valueOf(e.getText().trim());
        }
    }

    private static class ByteConverter extends BaseConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == Byte.class || type == Byte.TYPE;
        }

        @Override
        public Object read(Element e) {
            if (containsNull(e)) {
                return null;
            }
            return Byte.valueOf(e.getText());
        }
    }

    private static class BooleanConverter extends BaseConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == Boolean.class || type == Boolean.TYPE;
        }

        @Override
        public Object read(Element e) {
            if (containsNull(e)) {
                return null;
            }
            return Boolean.valueOf(e.getText().trim());
        }
    }

    private static class IntegerConverter extends BaseConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == Integer.class || type == Integer.TYPE;
        }

        @Override
        public Object read(Element e) {
            if (containsNull(e)) {
                return null;
            }
            return Integer.valueOf(e.getText().trim());
        }
    }

    private static class LongConverter extends BaseConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == Long.class || type == Long.TYPE;
        }

        @Override
        public Object read(Element e) {
            if (containsNull(e)) {
                return null;
            }
            return Long.valueOf(e.getText().trim());
        }
    }

    private static class StringConverter extends BaseConverter {
        @Override
        public boolean canConvert(Class type) {
            return type == String.class;
        }

        @Override
        public Object read(Element e) {
            if (containsNull(e)) {
                return null;
            }
            return e.getText();
        }
    }

    private static class MapConverter implements TypeConverter {
        @Override
        public boolean canConvert(Class type) {
            return type != null && Map.class.isAssignableFrom(type);
        }

        @Override
        public void write(Element e, Object o) throws Exception {
            if (o == null) {
                e.setAttribute("null", "true");
                return;
            }

            if (e.getAttribute("class") == null) {
                setClassAttribute(e, o.getClass());
            }

            for (Map.Entry entry : ((Map<?, ?>) o).entrySet()) {
                Element key = new Element("key");
                Object keyObj = entry.getKey();
                Class keyType = keyObj.getClass();
                setClassAttribute(key, keyType);
                writeTo(key, keyType, keyObj);

                Element value = new Element("value");
                Object valueObj = entry.getValue();
                Class valueType = valueObj.getClass();
                setClassAttribute(value, valueType);
                writeTo(value, valueType, valueObj);

                Element item = new Element("item");
                item.addContent(key);
                item.addContent(value);
                e.addContent(item);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void read(Element e, Object o) throws Exception {
            if (e.getAttributeValue("null") != null) {
                return;
            }
            Map map = (Map) o;

            for (Element element : e.getChildren("item")) {
                Element key = element.getChild("key");
                try {
                    Class keyType = getClassFromElement(key);
                    Object keyObj = readFrom(key, keyType);

                    Element value = element.getChild("value");
                    Class valueType = getClassFromElement(value);
                    Object valueObj = readFrom(value, valueType);

                    map.put(keyObj, valueObj);
                } catch (Exception e1) {
                    Logger.getInstance(getClass().getName()).info("Reading config value", e1);
                }
            }
        }

        @Override
        public Object read(Element e) throws Exception {
            if (e.getAttributeValue("null") != null) {
                return null;
            }
            Class type = getClassFromElement(e);
            Map obj = (Map) type.newInstance();
            read(e, obj);
            return obj;
        }
    }

    private static class ListConverter implements TypeConverter {
        @Override
        public boolean canConvert(Class type) {
            return type != null && List.class.isAssignableFrom(type);
        }

        @Override
        public void write(Element e, Object o) throws Exception {
            if (o == null) {
                e.setAttribute("null", "true");
                return;
            }

            if (e.getAttribute("class") == null) {
                setClassAttribute(e, o.getClass());
            }

            for (Object valueObj : (List) o) {

                Element item = new Element("item");
                final Class valueType = valueObj.getClass();
                setClassAttribute(item, valueType);
                writeTo(item, valueType, valueObj);

                e.addContent(item);
            }
        }

        @Override
        public Object read(Element e) throws Exception {
            if (e.getAttributeValue("null") != null) {
                return null;
            }
            Class type = getClassFromElement(e);
            List obj = (List) type.newInstance();
            read(e, obj);
            return obj;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void read(Element e, Object o) throws Exception {
            if (e.getAttributeValue("null") != null) {
                return;
            }
            List<? super Object> list = (List<? super Object>) o;
            for (Element element : e.getChildren("item")) {
                try {
                    Class valueType = getClassFromElement(element);
                    Object valueObj = readFrom(element, valueType);
                    list.add(valueObj);
                } catch (Exception e1) {
                    Logger.getInstance(getClass().getName()).info("Reading config value", e1);
                }
            }
        }
    }

    private static class EnumConverter implements TypeConverter {
        @Override
        public boolean canConvert(Class type) {
            return type != null && type.isEnum();
        }

        @Override
        public void write(Element e, Object o) {
            if (e.getAttribute("class") == null) {
                setClassAttribute(e, o.getClass());
            }
            if (o != null) {
                e.setText(((Enum) o).name());
            } else {
                e.setAttribute("null", "true");
            }
        }

        // throw exceptions in case of any failure and let the caller worry about malformed config
        @Override
        public Object read(Element e) throws Exception {
            if (e.getAttributeValue("null") != null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Class<Enum> clazz = (Class<Enum>) getClassFromElement(e);
            return Enum.valueOf(clazz, e.getText().trim());
        }

        @Override
        public void read(Element e, Object o) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    private static class DefaultConverter implements TypeConverter {
        @Override
        public boolean canConvert(Class type) {
            return true;
        }

        @Override
        public void write(Element e, Object o) throws Exception {
            if (o == null) {
                e.setAttribute("null", "true");
                return;
            }

            // it IS required for decoding
            if (e.getAttribute("class") == null) {
                setClassAttribute(e, o.getClass());
            }
            Property[] properties = PropertyUtil.getProperties(o);
            for (Property property : properties) {
                if (!(property.isReadable() && property.isWriteable())) {
                    continue;
                }

                Object value = property.getValue(o);
                Element propertyElement = new Element(property.getName());
                writeTo(propertyElement, property.getType(), value);
                e.addContent(propertyElement);
            }
        }

        @Override
        public Object read(Element e) throws Exception {
            if (e.getAttributeValue("null") != null) {
                return null;
            }
            Class type = getClassFromElement(e);
            Object obj = type.newInstance();
            read(e, obj);
            return obj;
        }

        @Override
        public void read(Element e, Object o) throws Exception {
            if (e.getAttributeValue("null") != null) {
                return;
            }
            Property[] properties = PropertyUtil.getProperties(o.getClass());
            for (Property property : properties) {
                if (!(property.isReadable() && property.isWriteable())) {
                    continue;
                }
                Element propertyElement = e.getChild(property.getName());
                if (propertyElement != null) {
                    try {
                        Object value = readFrom(propertyElement, property.getType());
                        property.setValue(o, value);
                    } catch (Exception e1) {
                        Logger.getInstance(getClass().getName()).info("Reading config value", e1);
                    }

                }
            }
        }
    }

    static void setClassAttribute(Element element, Class clazz) {
        final String className = clazz.getName();
        final String strippedName = className.startsWith(JARJAR_PREFIX) ? className.substring(JARJAR_PREFIX.length()) : className;
        element.setAttribute("class", strippedName);
    }

    static Class getClassFromElement(Element element) throws ClassNotFoundException {
        final String className = element.getAttributeValue("class");
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException origException) {
            try {
                return Class.forName(JARJAR_PREFIX + className);
            } catch (ClassNotFoundException e1) {
                throw origException; // throw the original ClassNotFound exception with non-transformed name
            }
        }
    }

}
