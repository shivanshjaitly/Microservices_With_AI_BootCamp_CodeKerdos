package in.coderkerdos.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice 
public class GloabalExceptionHandler {
    
    @ExceptionHandler (ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex,
        HttpServletRequest request){
            return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());


        }

        // HomeWORK

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, 
        HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }



        private ResponseEntity<ApiError> build(HttpStatus status, String message,
            String path){
                return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), message, path));
            }
    
}
