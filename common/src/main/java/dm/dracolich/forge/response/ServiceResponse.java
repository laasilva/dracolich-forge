package dm.dracolich.forge.response;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCodes;
import lombok.Getter;

import java.util.List;

public class ServiceResponse<T> {
    private Boolean success;

    @Getter
    private T payload;
    @Getter
    private List<ApiError> errors;

    public ServiceResponse(Boolean success, T payload, List<ApiError> errors) {
        this.success = success;
        this.payload = payload;
        this.errors = errors;
    }

    public ServiceResponse(T payload) {
        this.success = true;
        this.payload = payload;
    }

    public ServiceResponse(List<ApiError> errors) {
        this.success = false;
        this.errors = errors;
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

    public void addErrorFromErrorCode(ErrorCodes errorCode) {
        this.errors.add(new ApiError(errorCode));
    }

    public static<T> ServiceResponse<T> of(T payload) {
        return new ServiceResponse<>(payload);
    }

    public static<T> ServiceResponse<T> of(List<ApiError> errors) {
        return new ServiceResponse<>(errors);
    }
}
