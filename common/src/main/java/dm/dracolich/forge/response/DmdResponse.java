package dm.dracolich.forge.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCode;
import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Data
@JsonPropertyOrder({ "success", "httpStatus", "message", "payload", "errors" })
public class DmdResponse<T> {
    @Getter
    private Boolean success;
    @Getter
    private final HttpStatus httpStatus;
    @Getter
    private T payload;
    @Getter
    private final String message;
    @Getter
    private List<ApiError> errors;

    public DmdResponse(Boolean success, T payload, List<ApiError> errors,
                       HttpStatus httpStatus, String message) {
        this.success = success;
        this.payload = payload;
        this.errors = errors;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public DmdResponse(T payload) {
        this.payload = payload;
        this.httpStatus = HttpStatus.OK;
        this.message = "Request processed successfully";
        this.success = true;
        this.errors = null;
    }

    public DmdResponse(T payload, HttpStatus httpStatus, String message) {
        this.payload = payload;
        this.httpStatus = httpStatus;
        this.message = message;
        this.success = false;
        this.errors = null;
    }

    public DmdResponse(T payload, String message) {
        this.success = true;
        this.payload = payload;
        this.httpStatus = HttpStatus.OK;
        this.message = message;
        this.errors = null;
    }

    public DmdResponse(List<ApiError> errors) {
        this.success = false;
        this.payload = null;
        this.errors = errors;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.message = "Request ended with errors";
    }

    public DmdResponse(List<ApiError> errors, HttpStatus httpStatus) {
        this.success = false;
        this.payload = null;
        this.errors = errors;
        this.httpStatus = httpStatus;
        this.message = "Request ended with errors";
    }

    public DmdResponse(List<ApiError> errors, HttpStatus httpStatus, String message) {
        this.success = false;
        this.payload = null;
        this.errors = errors;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public ResponseEntity<T> toResponseEntity() {
        return new ResponseEntity<T>(this.payload, this.httpStatus);
    }

    public void addError(ApiError error) {
        this.errors.add(error);
    }

    public void setError(List<ApiError> errors) {
        this.errors = errors;
        this.success = false;
    }

    public void setPayload(T payload) {
        this.payload = payload;
        this.success = true;
    }

    public Boolean success() {
        return this.success;
    }

    public Boolean failure() {
        return !this.success;
    }

    public T payload() {
        return this.payload;
    }

    public void addErrorFromErrorCode(ErrorCode errorCode) {
        this.errors.add(new ApiError(errorCode));
    }

    public static<T> DmdResponse<T> of(T payload, HttpStatus httpStatus, String message) {
        return new DmdResponse<>(true, payload, null, httpStatus, message);
    }

    public static<T> DmdResponse<T> of(List<ApiError> errors, HttpStatus httpStatus, String message) {
        return new DmdResponse<>(false, null, errors, httpStatus, message);
    }
}
