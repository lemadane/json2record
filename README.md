# Record Data (`io.lemonade:record-data`)

Production-ready, zero-dependency standard Java 17+ library for bi-directional mapping between XML/JSON documents and strongly typed Java records.

Built purely on Java reflection and standard APIs, `record-data` requires no third-party XML/JSON libraries and works seamlessly across Spring Boot, Quarkus, Micronaut, and standalone JVM applications.

---

## Key Requirements & Coordinates

* **Java Version**: Minimum required JVM version is **Java 17**.
* **Maven Coordinates**: `io.lemonade:record-data:0.1.0-SNAPSHOT`
* **Base Package**: `io.lemonade.json2record`

```text
XML.parse() and JSON.parse()
require complete structural matching.

XML.partialParse() and JSON.partialParse()
map only declared record components and ignore undeclared input data.

There is no parseList(). Lists are represented by List<T>
record components.

XML attributes may map to matching mutable static fields.
JSON properties do not map to static fields.
```

---

## Installation

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'io.lemonade:record-data:0.1.0-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>io.lemonade</groupId>
    <artifactId>record-data</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

## Public API

The library provides two thread-safe final utility classes:

### `io.lemonade.json2record.XML`

```java
public final class XML {
    public static <T extends Record> T parse(Class<T> recordType, String xml);
    public static <T extends Record> T partialParse(Class<T> recordType, String xml);
    public static String stringify(Record xmlRecord);
}
```

### `io.lemonade.json2record.JSON`

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
public record FacilityInboundEvent(
        VanLoadScan VanLoadScan,
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
<FacilityInboundEvent Version="1.0">
    <VanLoadScan Version="1.0">
        <FacilityID>0603</FacilityID>
        <ScanType>V3</ScanType>
        <ScannedBarcode>9611019061319607196193</ScannedBarcode>
        <ScanDateTime>
            <Date>20210804</Date>
            <Time24hr>072512</Time24hr>
            <Microseconds>527412</Microseconds>
        </ScanDateTime>
        <PlaceHolder/>
    </VanLoadScan>
    <PlaceHolder/>
</FacilityInboundEvent>
```

**Java Records:**

```java
public record FacilityInboundEvent(
        VanLoadScan VanLoadScan,
        PlaceHolder PlaceHolder
) {
    private static String Version;

    public static String version() {
        return Version;
    }
}

public record VanLoadScan(
        String FacilityID,
        String ScanType,
        String ScannedBarcode,
        ScanDateTime ScanDateTime,
        PlaceHolder PlaceHolder
) {
    private static String Version;

    public static String version() {
        return Version;
    }
}

public record ScanDateTime(
        String Date,
        String Time24hr,
        String Microseconds
) {}

public record PlaceHolder() {}
```

**Usage:**

```java
final var event = XML.parse(FacilityInboundEvent.class, xml);
final String facilityID = event.VanLoadScan().FacilityID();
final String version = FacilityInboundEvent.version();
final String outputXml = XML.stringify(event);
```

### 2. Namespaced XML Mapping

**XML Input:**

```xml
<fxg:FacilityInboundEvent xmlns:fxg="urn:fedex:facility">
    <fxg:VanLoadScan>
        <fxg:FacilityID>0603</fxg:FacilityID>
    </fxg:VanLoadScan>
</fxg:FacilityInboundEvent>
```

**Java Records:**

```java
public record fxg$FacilityInboundEvent(
        fxg$VanLoadScan fxg$VanLoadScan
) {
    private static String xmlns$fxg;

    public static String namespace() {
        return xmlns$fxg;
    }
}

public record fxg$VanLoadScan(
        String fxg$FacilityID
) {}
```

**Usage:**

```java
final var event = XML.parse(fxg$FacilityInboundEvent.class, xml);
final String facilityID = event.fxg$VanLoadScan().fxg$FacilityID();
final String ns = fxg$FacilityInboundEvent.namespace(); // "urn:fedex:facility"
```

### 3. JSON Mapping

**JSON Input:**

```json
{
  "eventMessageCount": 2,
  "vanLoadScans": [
    {
      "facilityID": "0603",
      "scanType": "V3"
    },
    {
      "facilityID": "0417",
      "scanType": "I3"
    }
  ]
}
```

**Java Records:**

```java
public record FacilityInboundEvent(
        int eventMessageCount,
        List<VanLoadScan> vanLoadScans
) {}

public record VanLoadScan(
        String facilityID,
        String scanType
) {}
```

**Usage:**

```java
final var event = JSON.parse(FacilityInboundEvent.class, json);
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
* `Missing XML element <ScannedBarcode> required by com.example.VanLoadScan.ScannedBarcode at /FacilityInboundEvent/VanLoadScan.`
* `Excess JSON property "vehicle" at $.vehicle. No matching record component exists in com.example.ScanSummary.`
* `Cannot convert JSON value "abc" to int for com.example.PackageData.packageCount at $.packageCount.`

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
