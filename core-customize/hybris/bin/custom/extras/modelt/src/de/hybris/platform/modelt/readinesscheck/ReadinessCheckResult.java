package de.hybris.platform.modelt.readinesscheck;

import org.springframework.http.HttpStatus;

public class ReadinessCheckResult {
    private final ReadinessStatus readinessStatus;
    private final HttpStatus httpStatus;
    private final String message;
 
    public ReadinessCheckResult(final ReadinessStatus readinessStatus, final HttpStatus httpStatus,
                                final String message) {
        this.readinessStatus = readinessStatus;
        this.httpStatus = httpStatus;
        this.message = message;
    }
 
    public ReadinessStatus getReadinessStatus() {
        return readinessStatus;
    }
 
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
 
    public String getMessage() {
        return message;
    }
 
    public static ReadinessCheckResult ready() {
        return new ReadinessCheckResult(ReadinessStatus.READY, HttpStatus.OK, null);
    }
 
    public static ReadinessCheckResult notReady() {
        return new ReadinessCheckResult(ReadinessStatus.NOT_READY, HttpStatus.SERVICE_UNAVAILABLE, null);
    }
 
    public static ReadinessCheckResult error(final String message) {
        return new ReadinessCheckResult(ReadinessStatus.ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
