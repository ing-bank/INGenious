package com.ing.ingenious.api.types;

import java.util.regex.Pattern;

/**
 * The primitive value grammars that an action's {@code Input} (or second
 * object) can take. Each {@code ArgType} carries a human label, a canonical
 * example and an optional validator over a <em>concrete literal</em> value
 * (already unwrapped of any {@code @} prefix).
 *
 * <p>These are the building blocks shared by the IDE IntelliSense and the AI
 * step-authoring tools so both reason about action inputs identically. The
 * universal INGenious value wrappers ({@code @literal}, {@code Sheet:Column},
 * {@code {Sheet:Column}}, {@code %var%}, {@code =function}, {@code #alias}) are
 * handled by the catalog, not here; a validator only ever sees a plain literal.
 *
 * @see com.ing.ingenious.api.annotation.Args
 */
public enum ArgType {
    /** Free text; accepts anything. */
    TEXT("text", "@value", null, false),
    /** Absolute URL. */
    URL("URL", "@https://example.com", Pattern.compile("(?i)^[a-z][a-z0-9+.\\-]*://.+"), false),
    /** HTTP status code (100-599). */
    HTTP_STATUS("HTTP status code", "@200", Pattern.compile("^[1-5][0-9]{2}$"), false),
    /** Whole number. */
    INTEGER("integer", "@10", Pattern.compile("^-?[0-9]+$"), false),
    /** Decimal number. */
    NUMBER("number", "@10.5", Pattern.compile("^-?[0-9]+(\\.[0-9]+)?$"), false),
    /** Boolean flag. */
    BOOLEAN("boolean (true/false)", "@true", Pattern.compile("(?i)^(true|false)$"), false),
    /** JSONPath expression. */
    JSONPATH("JSONPath", "@$.store.book[0].title", Pattern.compile("^\\$.*"), false),
    /** XPath expression. */
    XPATH("XPath", "@//div[@id='x']", Pattern.compile("^(\\.?//|\\().+"), false),
    /** CSS selector. */
    CSS_SELECTOR("CSS selector", "@#id .class", null, false),
    /** Object-Repository locator (role/testId/css/xpath ladder). */
    LOCATOR("locator", "role=button[name=\"Login\"]", null, false),
    /** SQL statement. */
    SQL(
        "SQL statement",
        "@SELECT * FROM users",
        Pattern.compile("(?is)^\\s*(select|insert|update|delete|merge|with|call|exec|create|drop|alter|truncate)\\b.*"),
        true
    ),
    /** Raw JSON request body. */
    JSON_BODY("JSON request body", "{ \"key\": \"{Sheet:Column}\" }", null, true),
    /** Raw XML request body. */
    XML_BODY("XML request body", "<root/>", null, true),
    /** Header pair in {@code key=value} (or {@code key|value}) form. */
    HEADER_KV("header (key=value)", "@Content-Type=application/json", Pattern.compile("^[^=|]+[=|].*$"), false),
    /** Messaging topic name. */
    TOPIC("topic name", "@orders", null, false),
    /** Messaging partition number. */
    PARTITION("partition", "@0", Pattern.compile("^[0-9]+$"), false),
    /** Timeout in milliseconds. */
    TIMEOUT_MS("timeout in ms", "@5000", Pattern.compile("^[0-9]+$"), false),
    /** Duration ({@code 5000}, {@code 5s}, {@code 2m}). */
    DURATION("duration", "@5000", Pattern.compile("(?i)^[0-9]+(ms|s|m)?$"), false),
    /** Filesystem path. */
    FILE_PATH("file path", "@/tmp/data.csv", null, false),
    /** Regular expression. */
    REGEX("regular expression", "@[A-Z]+", null, false),
    /** One of a fixed set supplied by the spec's allowed values. */
    ENUM("one of a fixed set", "", null, false),
    /** {@code Sheet:Column} data-sheet reference (whole input). */
    DATA_REF("Sheet:Column data reference", "LoginData:Username", null, false),
    /** {@code #dbAlias} database connection alias. */
    ALIAS_DB("#dbAlias", "#PostgresMain", Pattern.compile("^#.+"), false),
    /** {@code #apiAlias} API context alias. */
    ALIAS_API("#apiAlias", "#PetStore", Pattern.compile("^#.+"), false),
    /** {@code #contextAlias} context alias. */
    ALIAS_CONTEXT("#contextAlias", "#mobileCtx", Pattern.compile("^#.+"), false),
    /** {@code #producerAlias} Kafka producer config alias. */
    ALIAS_KAFKA_PRODUCER("#producerAlias", "#OrdersProducer", Pattern.compile("^#.+"), false),
    /** {@code #consumerAlias} Kafka consumer config alias. */
    ALIAS_KAFKA_CONSUMER("#consumerAlias", "#OrdersConsumer", Pattern.compile("^#.+"), false),
    /** GlobalData environment id. */
    GLOBAL_ID("GlobalData id", "#test", Pattern.compile("^#.+"), false),
    /** Engine directive such as {@code @Enter}, {@code @Browser}. */
    DIRECTIVE("engine directive", "@Enter", null, false);

    private final String label;
    private final String example;
    private final Pattern validator;
    private final boolean body;

    ArgType(String label, String example, Pattern validator, boolean body) {
        this.label = label;
        this.example = example;
        this.validator = validator;
        this.body = body;
    }

    /** Human-readable label, e.g. {@code "HTTP status code"}. */
    public String label() {
        return label;
    }

    /** Canonical example, e.g. {@code "@200"}. */
    public String example() {
        return example;
    }

    /**
     * True when this type is a raw request/query body (SQL, JSON, XML). Body
     * inputs are never {@code @}-prefixed; individual values inside them are
     * parameterized with {@code {Sheet:Column}} tokens instead.
     */
    public boolean isBody() {
        return body;
    }

    /** True when a concrete (unwrapped) literal is valid for this type. */
    public boolean matchesLiteral(String literal) {
        if (validator == null) {
            return true;
        }
        return literal != null && validator.matcher(literal.trim()).matches();
    }

    /** True when this type has a strict validator that can reject a literal. */
    public boolean isStrict() {
        return validator != null;
    }
}
