package dm.dracolich.forge.controller;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCodes;
import dm.dracolich.forge.response.DmdResponse;
import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.forge.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ControllerAdvice {
    @ExceptionHandler(ResponseException.class)
    public ResponseEntity<DmdResponse<?>> handleResponseException(ResponseException ex) {
        return new ResponseEntity<>(new DmdResponse<>(false, null, ex.getErrors(),
                ex.getHttpStatus(), ex.getMessage()), ex.getHttpStatus());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<DmdResponse<?>> handleValidationException(ValidationException ex) {
        List<ApiError> errors = ex.getErrors().stream()
                .map(ApiError::new)
                .toList();
        return new ResponseEntity<>(new DmdResponse<>(false, null, errors,
                HttpStatus.BAD_REQUEST, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DmdResponse<?>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        return new ResponseEntity<>(new DmdResponse<>(false, null, errors,
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
