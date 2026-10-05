package in.coderkerdos.exception;

import java.time.Instant;

public record ApiError(
    Instant timeStamp,
    int status,
    String message,
    String path
) {} 