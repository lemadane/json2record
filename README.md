# Json2Record

Production-ready, zero-dependency standard Java 17+ library for bi-directional mapping between JSON and XML documents and strongly typed Java records.

Built purely on Java reflection and standard APIs, `Json2Record` requires no third-party JSON libraries and works seamlessly across Spring Boot, Quarkus, Micronaut, and standalone JVM applications.

---

## Key Requirements & Coordinates

* **Java Version**: Minimum required JVM version is **Java 17**.
* **Maven Coordinates**: `io.lemadane:record-data:0.1.0-SNAPSHOT`
* **Base Package**: `io.lemadane.json2record`

```java
Record record = JSON.parse(Class<Record> Record.class, String json);
// converts JSON to Java Record.

Record record = XML.parse(Class<Record> Record.class, String xml);
// converts XML to Java Record.

String json = JSON.stringify(Record record);
// converts Java Record to JSON.

String xml = XML.stringify(Record record);
// converts Java Record to XML.

Record record = JSON.partialParse(Class<Record> Record.class, String json);
// json data maps to declared Java Record components, ignoring other declared data,  

Record record = XML.partialParse(Class<Record> Record.class, String xml);
// json data maps to declared Java Record components, ignoring other declared data,  
// XML attributes map to matching mutable static fields.

```

---

## Installation

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'io.lemadane:record-data:0.1.0-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>io.lemadane</groupId>
    <artifactId>record-data</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

## Public API

The library provides two thread-safe final utility classes:

### `io.lemadane.json2record.XML`

```java
public final class XML {
    public static <T extends Record> T parse(Class<T> recordType, String xml);
    public static <T extends Record> T partialParse(Class<T> recordType, String xml);
    public static String stringify(Record xmlRecord);
}
```

### `io.lemadane.json2record.JSON`

```java
public final class JSON {
    public static <T extends Record> T parse(Class<T> recordType, String json);
    public static <T extends Record> T partialParse(Class<T> recordType, String json);
    public static String stringify(Record jsonRecord);
}
```

* All public methods reject `null` arguments with a clear `NullPointerException`.
* Target types passed to `parse()` or `partialParse()` must be Java records; ordinary classes are rejected with a `RecordConstructionException`.

---

## Strict vs. Partial Parsing

### Strict Mode (`XML.parse()` / `JSON.parse()`)
Enforces strict 1-to-1 structural completeness:
* Throws an exception if a record component has no matching XML element or JSON property (`MissingDataException`).
* Throws an exception if an undeclared XML element or JSON property is encountered (`ExcessDataException`).
* Validates root XML element name against the target simple record name.
* Rejects duplicate XML child elements for non-List components.
* Rejects duplicate JSON object keys (`JsonMappingException`).

### Partial Mode (`XML.partialParse()` / `JSON.partialParse()`)
Selective mapping into declared components:
* Maps XML elements and JSON properties matching declared record components.
* Ignores undeclared elements and properties.
* Missing components default to standard defaults:
  * Reference types -> `null`
  * `Optional<T>` -> `Optional.empty()`
  * `List<T>` -> `List.of()`
  * `boolean` -> `false`
  * `byte`, `short`, `int`, `long` -> `0` / `0L`
  * `float`, `double` -> `0.0f` / `0.0d`
  * `char` -> `'\0'`
* Present but malformed values still throw a `TypeConversionException`.

---

## XML Static-Field Attribute Mapping

XML attributes map to **matching mutable static fields** declared directly on the record class representing that element.

> [!WARNING]
> **Shared-State Warning**: Static fields in Java are shared across all instances of a class within a ClassLoader. Subsequent parses of XML containing attributes into the same record class will replace previously stored static field values.

Example record with XML attribute mapping:

```java
public record RestaurantOrderEvent(
        KitchenOrderScan KitchenOrderScan,
        PlaceHolder PlaceHolder
) {
    private static String Version;

    public static String version() {
        return Version;
    }
}
```

* Supported field visibilities: `public`, `protected`, package-private, and `private static`.
* Field must not be `final` when parsing needs to assign it.
* Unmatched XML attributes are ignored and do not trigger excess data errors during strict parsing.
* JSON object properties **never** populate static fields.

---

## Reversible Special-Character Encoding (`DataNameCodec`)

XML element/attribute names and JSON keys containing characters problematic in Java identifiers are reversibly escaped using `$` as the marker:

| External Character | Reversible Identifier Escape | Example External Name | Encoded Java Component / Type |
| :--- | :--- | :--- | :--- |
| `:` | `$` | `fxg:FacilityInboundEvent` | `fxg$FacilityInboundEvent` |
| `-` | `$hyphen$` | `Facility-ID` | `Facility$hyphen$ID` |
| `.` | `$dot$` | `Facility.ID` | `Facility$dot$ID` |
| `space` | `$space$` | `display name` | `display$space$name` |
| `$` | `$dollar$` | `price$value` | `price$dollar$value` |
| Other | `$uXXXX$` | `emoji_😀` | `emoji_$u1F600$` |

---

## Complete Runnable Examples

### 1. Standard XML Mapping

**XML Input:**

```xml
<RestaurantOrderEvent Version="1.0">
    <KitchenOrderScan Version="1.0">
        <RestaurantID>0603</RestaurantID>
        <OrderType>DineIn</OrderType>
        <OrderTicketNumber>9611019061319607196193</OrderTicketNumber>
        <OrderTimestamp>
            <Date>20260721</Date>
            <Time24hr>123045</Time24hr>
            <Microseconds>527412</Microseconds>
        </OrderTimestamp>
        <PlaceHolder/>
    </KitchenOrderScan>
    <PlaceHolder/>
</RestaurantOrderEvent>
```

**Java Records:**

```java
public record RestaurantOrderEvent(
        KitchenOrderScan KitchenOrderScan,
        PlaceHolder PlaceHolder
) {
    private static String Version;

    public static String version() {
        return Version;
    }
}

public record KitchenOrderScan(
        String RestaurantID,
        String OrderType,
        String OrderTicketNumber,
        OrderTimestamp OrderTimestamp,
        PlaceHolder PlaceHolder
) {
    private static String Version;

    public static String version() {
        return Version;
    }
}

public record OrderTimestamp(
        String Date,
        String Time24hr,
        String Microseconds
) {}

public record PlaceHolder() {}
```

**Usage:**

```java
final var event = XML.parse(RestaurantOrderEvent.class, xml);
final String restaurantID = event.KitchenOrderScan().RestaurantID();
final String version = RestaurantOrderEvent.version();
final String outputXml = XML.stringify(event);
```

### 2. Namespaced XML Mapping

**XML Input:**

```xml
<rest:RestaurantOrderEvent xmlns:rest="urn:restaurant:kitchen">
    <rest:KitchenOrderScan>
        <rest:RestaurantID>0603</rest:RestaurantID>
    </rest:KitchenOrderScan>
</rest:RestaurantOrderEvent>
```

**Java Records:**

```java
public record rest$RestaurantOrderEvent(
        rest$KitchenOrderScan rest$KitchenOrderScan
) {
    private static String xmlns$rest;

    public static String namespace() {
        return xmlns$rest;
    }
}

public record rest$KitchenOrderScan(
        String rest$RestaurantID
) {}
```

**Usage:**

```java
final var event = XML.parse(rest$RestaurantOrderEvent.class, xml);
final String restaurantID = event.rest$KitchenOrderScan().rest$RestaurantID();
final String ns = rest$RestaurantOrderEvent.namespace(); // "urn:restaurant:kitchen"
```

### 3. JSON Mapping

**JSON Input:**

```json
{
  "eventMessageCount": 2,
  "kitchenOrderScans": [
    {
      "restaurantID": "0603",
      "orderType": "DineIn"
    },
    {
      "restaurantID": "0417",
      "orderType": "Takeout"
    }
  ]
}
```

**Java Records:**

```java
public record RestaurantOrderEvent(
        int eventMessageCount,
        List<KitchenOrderScan> kitchenOrderScans
) {}

public record KitchenOrderScan(
        String restaurantID,
        String orderType
) {}
```

**Usage:**

```java
final var event = JSON.parse(RestaurantOrderEvent.class, json);
final String outputJson = JSON.stringify(event);
```

---

## Supported Java Component Types

* `String` (preserves leading zeros and identifier strings)
* Primitives & Wrappers (`boolean`/`Boolean`, `byte`/`Byte`, `short`/`Short`, `int`/`Integer`, `long`/`Long`, `float`/`Float`, `double`/`Double`, `char`/`Character`)
* `BigInteger` & `BigDecimal` (exact numeric precision preserved)
* `Optional<T>`
* `List<T>` (collections of records or primitives)
* Nested records
* Enums (`Enum.name()`)

---

## XML Parsing Security Policy

Configured defensively by default:
* External general entities (`external-general-entities`) disabled.
* External parameter entities (`external-parameter-entities`) disabled.
* `DOCTYPE` declarations disallowed (`disallow-doctype-decl`).
* Secure processing feature enabled (`XMLConstants.FEATURE_SECURE_PROCESSING`).
* XInclude & entity expansion disabled.

---

## Error Model

Base unchecked exception:
* `DataMappingException`

Focused sub-exceptions with rich path context:
* `XmlMappingException`
* `JsonMappingException`
* `MissingDataException`
* `ExcessDataException`
* `TypeConversionException`
* `NameEncodingException`
* `RecordConstructionException`

Example rich path messages:
* `Missing XML element <OrderTicketNumber> required by com.example.KitchenOrderScan.OrderTicketNumber at /RestaurantOrderEvent/KitchenOrderScan.`
* `Excess JSON property "table" at $.table. No matching record component exists in com.example.KitchenOrderScan.`
* `Cannot convert JSON value "abc" to int for com.example.OrderData.itemCount at $.itemCount.`

---

## Build & Test Commands

To build the library, run tests, checkstyle static analysis, and verify JaCoCo coverage (>= 90%):

```bash
./gradlew clean test
./gradlew jacocoTestReport
./gradlew build
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](file:///home/lem/Projects/java/json-xml-utility/LICENSE) file for details.
