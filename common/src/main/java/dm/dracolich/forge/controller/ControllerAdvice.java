package dm.dracolich.forge.controller;

import dm.dracolich.forge.response.DmdResponse;
import dm.dracolich.forge.exception.ResponseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerAdvice {
    @ExceptionHandler(ResponseException.class)
    public ResponseEntity<DmdResponse<?>> handleResponseException(ResponseException ex) {
        return new ResponseEntity<>(new DmdResponse<>(false, null, ex.getErrors(),
                ex.getHttpStatus(), ex.getMessage()), ex.getHttpStatus());
    }
}
