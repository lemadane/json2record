package io.lemonade.json2record.convert;

import io.lemonade.json2record.exceptions.TypeConversionException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utility for converting scalar values to declared Java record component types.
 */
public final class TypeConverter {

    private TypeConverter() {
    }

    /**
     * Converts a raw value (String, Number, Boolean, etc.) to the target type.
     *
     * @param targetType the declared target Java type.
     * @param rawValue the raw value to convert.
     * @param pathContext path context description for exception reporting.
     * @return the converted value.
     * @throws TypeConversionException if conversion fails or type is incompatible.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object convert(Class<?> targetType, Object rawValue, String pathContext) {
        if (rawValue == null) {
            if (targetType.isPrimitive()) {
                throw new TypeConversionException(
                        "Cannot convert null value to primitive type " + targetType.getName() + " at " + pathContext
                );
            }
            return null;
        }

        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }

        String strVal = rawValue.toString().trim();

        if (targetType == String.class) {
            return rawValue.toString();
        }

        if (targetType == boolean.class || targetType == Boolean.class) {
            if (rawValue instanceof Boolean b) {
                return b;
            }
            if ("true".equalsIgnoreCase(strVal)) {
                return Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(strVal)) {
                return Boolean.FALSE;
            }
            throw new TypeConversionException(
                    "Cannot convert value \"" + strVal + "\" to boolean at " + pathContext
            );
        }

        if (targetType == byte.class || targetType == Byte.class) {
            try {
                if (rawValue instanceof Number n) {
                    return n.byteValue();
                }
                return Byte.parseByte(strVal);
            } catch (NumberFormatException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to byte at " + pathContext, e
                );
            }
        }

        if (targetType == short.class || targetType == Short.class) {
            try {
                if (rawValue instanceof Number n) {
                    return n.shortValue();
                }
                return Short.parseShort(strVal);
            } catch (NumberFormatException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to short at " + pathContext, e
                );
            }
        }

        if (targetType == int.class || targetType == Integer.class) {
            try {
                if (rawValue instanceof Number n) {
                    return n.intValue();
                }
                return Integer.parseInt(strVal);
            } catch (NumberFormatException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to int at " + pathContext, e
                );
            }
        }

        if (targetType == long.class || targetType == Long.class) {
            try {
                if (rawValue instanceof Number n) {
                    return n.longValue();
                }
                return Long.parseLong(strVal);
            } catch (NumberFormatException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to long at " + pathContext, e
                );
            }
        }

        if (targetType == float.class || targetType == Float.class) {
            try {
                if (rawValue instanceof Number n) {
                    return n.floatValue();
                }
                return Float.parseFloat(strVal);
            } catch (NumberFormatException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to float at " + pathContext, e
                );
            }
        }

        if (targetType == double.class || targetType == Double.class) {
            try {
                if (rawValue instanceof Number n) {
                    return n.doubleValue();
                }
                return Double.parseDouble(strVal);
            } catch (NumberFormatException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to double at " + pathContext, e
                );
            }
        }

        if (targetType == char.class || targetType == Character.class) {
            if (strVal.length() == 1) {
                return strVal.charAt(0);
            }
            throw new TypeConversionException(
                    "Cannot convert value \"" + strVal + "\" to char at " + pathContext
            );
        }

        if (targetType == BigInteger.class) {
            try {
                return new BigInteger(strVal);
            } catch (NumberFormatException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to BigInteger at " + pathContext, e
                );
            }
        }

        if (targetType == BigDecimal.class) {
            try {
                return new BigDecimal(strVal);
            } catch (NumberFormatException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to BigDecimal at " + pathContext, e
                );
            }
        }

        if (targetType.isEnum()) {
            try {
                return Enum.valueOf((Class<Enum>) targetType, strVal);
            } catch (IllegalArgumentException e) {
                throw new TypeConversionException(
                        "Cannot convert value \"" + strVal + "\" to enum " + targetType.getName() + " at " + pathContext, e
                );
            }
        }

        throw new TypeConversionException(
                "Unsupported target type " + targetType.getName() + " for value \"" + strVal + "\" at " + pathContext
        );
    }
}
