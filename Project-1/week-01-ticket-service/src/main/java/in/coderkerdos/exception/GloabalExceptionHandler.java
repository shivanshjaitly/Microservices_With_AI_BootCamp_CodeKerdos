package in.coderkerdos.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        // @ExceptionHandler (MethodArgumentNotValidException.class)
        // public ResponseEntity<ApiError> handleValuation(
        //     ResourceNotFoundException ex, HttpServletRequest request){

        //     String message = ex.getBindingResult()
        //     .getField
        //     return 
        
        //     }



        private ResponseEntity<ApiError> build(HttpStatus status, String message,
            String path){
                return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), message, path));
            }
    
}
