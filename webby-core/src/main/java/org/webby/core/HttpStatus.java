package org.webby.core;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enumeration of HTTP status codes and their reason phrases.
 */
public enum HttpStatus {
    /** HTTP status code 100. */
    CONTINUE(100, "Continue"),
    /** HTTP status code 101. */
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    /** HTTP status code 102. */
    PROCESSING(102, "Processing"),
    /** HTTP status code 103. */
    EARLY_HINTS(103, "Early Hints"),

    /** HTTP status code 200. */
    OK(200, "OK"),
    /** HTTP status code 201. */
    CREATED(201, "Created"),
    /** HTTP status code 202. */
    ACCEPTED(202, "Accepted"),
    /** HTTP status code 203. */
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    /** HTTP status code 204. */
    NO_CONTENT(204, "No Content"),
    /** HTTP status code 205. */
    RESET_CONTENT(205, "Reset Content"),
    /** HTTP status code 206. */
    PARTIAL_CONTENT(206, "Partial Content"),
    /** HTTP status code 207. */
    MULTI_STATUS(207, "Multi-Status"),
    /** HTTP status code 208. */
    ALREADY_REPORTED(208, "Already Reported"),
    /** HTTP status code 226. */
    IM_USED(226, "IM Used"),

    /** HTTP status code 300. */
    MULTIPLE_CHOICES(300, "Multiple Choices"),
    /** HTTP status code 301. */
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    /** HTTP status code 302. */
    FOUND(302, "Found"),
    /** HTTP status code 303. */
    SEE_OTHER(303, "See Other"),
    /** HTTP status code 304. */
    NOT_MODIFIED(304, "Not Modified"),
    /** HTTP status code 305. */
    USE_PROXY(305, "Use Proxy"),
    /** HTTP status code 307. */
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    /** HTTP status code 308. */
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    /** HTTP status code 400. */
    BAD_REQUEST(400, "Bad Request"),
    /** HTTP status code 401. */
    UNAUTHORIZED(401, "Unauthorized"),
    /** HTTP status code 402. */
    PAYMENT_REQUIRED(402, "Payment Required"),
    /** HTTP status code 403. */
    FORBIDDEN(403, "Forbidden"),
    /** HTTP status code 404. */
    NOT_FOUND(404, "Not Found"),
    /** HTTP status code 405. */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    /** HTTP status code 406. */
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    /** HTTP status code 407. */
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    /** HTTP status code 408. */
    REQUEST_TIMEOUT(408, "Request Timeout"),
    /** HTTP status code 409. */
    CONFLICT(409, "Conflict"),
    /** HTTP status code 410. */
    GONE(410, "Gone"),
    /** HTTP status code 411. */
    LENGTH_REQUIRED(411, "Length Required"),
    /** HTTP status code 412. */
    PRECONDITION_FAILED(412, "Precondition Failed"),
    /** HTTP status code 413. */
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    /** HTTP status code 414. */
    URI_TOO_LONG(414, "URI Too Long"),
    /** HTTP status code 415. */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    /** HTTP status code 416. */
    RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),
    /** HTTP status code 417. */
    EXPECTATION_FAILED(417, "Expectation Failed"),
    /** HTTP status code 418. */
    IM_A_TEAPOT(418, "I'm a teapot"),
    /** HTTP status code 421. */
    MISDIRECTED_REQUEST(421, "Misdirected Request"),
    /** HTTP status code 422. */
    UNPROCESSABLE_CONTENT(422, "Unprocessable Content"),
    /** HTTP status code 423. */
    LOCKED(423, "Locked"),
    /** HTTP status code 424. */
    FAILED_DEPENDENCY(424, "Failed Dependency"),
    /** HTTP status code 425. */
    TOO_EARLY(425, "Too Early"),
    /** HTTP status code 426. */
    UPGRADE_REQUIRED(426, "Upgrade Required"),
    /** HTTP status code 428. */
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    /** HTTP status code 429. */
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    /** HTTP status code 431. */
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
    /** HTTP status code 451. */
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),

    /** HTTP status code 500. */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    /** HTTP status code 501. */
    NOT_IMPLEMENTED(501, "Not Implemented"),
    /** HTTP status code 502. */
    BAD_GATEWAY(502, "Bad Gateway"),
    /** HTTP status code 503. */
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    /** HTTP status code 504. */
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    /** HTTP status code 505. */
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
    /** HTTP status code 506. */
    VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
    /** HTTP status code 507. */
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
    /** HTTP status code 508. */
    LOOP_DETECTED(508, "Loop Detected"),
    /** HTTP status code 510. */
    NOT_EXTENDED(510, "Not Extended"),
    /** HTTP status code 511. */
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

    private static final Map<Integer, HttpStatus> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(HttpStatus::code, status -> status));

    private final int code;
    private final String reasonPhrase;

    HttpStatus(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * Returns the numeric form of this status code.
     *
     * @return numeric HTTP status code.
     */
    public int code() {
        return code;
    }

    /**
     * Returns the canonical reason phrase defined for the status.
     *
     * @return canonical reason phrase defined for the status code.
     */
    public String reasonPhrase() {
        return reasonPhrase;
    }

    /**
     * Resolves the enum constant for the provided HTTP status code.
     *
     * @param code HTTP status code
     * @return the matching enum value or {@code null} when no match exists
     */
    public static HttpStatus fromCode(int code) {
        return BY_CODE.get(code);
    }
}
