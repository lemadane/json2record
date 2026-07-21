package io.lemonade.json2record.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Metadata holding reflection details for a Java record class.
 */
public final class RecordMetadata {

    private final Class<?> recordType;
    private final String recordSimpleName;
    private final String decodedRecordName;
    private final Constructor<?> canonicalConstructor;
    private final List<ComponentMetadata> components;
    private final Map<String, ComponentMetadata> componentsByDecodedName;
    private final Map<String, Field> staticFieldsByDecodedName;

    public RecordMetadata(
            Class<?> recordType,
            String recordSimpleName,
            String decodedRecordName,
            Constructor<?> canonicalConstructor,
            List<ComponentMetadata> components,
            Map<String, ComponentMetadata> componentsByDecodedName,
            Map<String, Field> staticFieldsByDecodedName
    ) {
        this.recordType = recordType;
        this.recordSimpleName = recordSimpleName;
        this.decodedRecordName = decodedRecordName;
        this.canonicalConstructor = canonicalConstructor;
        this.components = Collections.unmodifiableList(components);
        this.componentsByDecodedName = Collections.unmodifiableMap(componentsByDecodedName);
        this.staticFieldsByDecodedName = Collections.unmodifiableMap(staticFieldsByDecodedName);
    }

    public Class<?> recordType() {
        return recordType;
    }

    public String recordSimpleName() {
        return recordSimpleName;
    }

    public String decodedRecordName() {
        return decodedRecordName;
    }

    public Constructor<?> canonicalConstructor() {
        return canonicalConstructor;
    }

    public List<ComponentMetadata> components() {
        return components;
    }

    public ComponentMetadata findComponentByDecodedName(String name) {
        return componentsByDecodedName.get(name);
    }

    public ComponentMetadata findComponentByRawName(String name) {
        for (ComponentMetadata comp : components) {
            if (comp.name().equals(name)) {
                return comp;
            }
        }
        return null;
    }

    public Map<String, Field> staticFieldsByDecodedName() {
        return staticFieldsByDecodedName;
    }

    /**
     * Metadata holding details for a single record component.
     */
    public static final class ComponentMetadata {
        private final String name;
        private final String decodedName;
        private final Class<?> type;
        private final Type genericType;
        private final Method accessor;
        private final boolean isList;
        private final Class<?> listElementType;
        private final boolean isOptional;
        private final Class<?> optionalValueType;
        private final boolean isNestedRecord;

        public ComponentMetadata(
                String name,
                String decodedName,
                Class<?> type,
                Type genericType,
                Method accessor
        ) {
            this.name = name;
            this.decodedName = decodedName;
            this.type = type;
            this.genericType = genericType;
            this.accessor = accessor;
            this.isList = List.class.isAssignableFrom(type);
            this.listElementType = isList ? resolveGenericTypeArgument(genericType) : null;
            this.isOptional = Optional.class.isAssignableFrom(type);
            this.optionalValueType = isOptional ? resolveGenericTypeArgument(genericType) : null;
            this.isNestedRecord = type.isRecord();
        }

        private static Class<?> resolveGenericTypeArgument(Type type) {
            if (type instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0) {
                    if (args[0] instanceof Class<?> clazz) {
                        return clazz;
                    } else if (args[0] instanceof ParameterizedType nestedPt && nestedPt.getRawType() instanceof Class<?> rawClazz) {
                        return rawClazz;
                    }
                }
            }
            return Object.class;
        }

        public String name() {
            return name;
        }

        public String decodedName() {
            return decodedName;
        }

        public Class<?> type() {
            return type;
        }

        public Type genericType() {
            return genericType;
        }

        public Method accessor() {
            return accessor;
        }

        public boolean isList() {
            return isList;
        }

        public Class<?> listElementType() {
            return listElementType;
        }

        public boolean isOptional() {
            return isOptional;
        }

        public Class<?> optionalValueType() {
            return optionalValueType;
        }

        public boolean isNestedRecord() {
            return isNestedRecord;
        }
    }
}
