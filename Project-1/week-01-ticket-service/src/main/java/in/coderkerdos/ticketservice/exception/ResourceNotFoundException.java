package in.coderkerdos.ticketservice.exception;
 
public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message){
        super(message);
    }
}
