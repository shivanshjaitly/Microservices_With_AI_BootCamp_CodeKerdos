package in.codekerdos.classifier.exception;

import java.time.Instant;

public record ApiError(
    Instant timeStamp,
    int status,
    String message,
    String path
) {} 