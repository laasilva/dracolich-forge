package dm.dracolich.forge.exception;

import dm.dracolich.forge.error.ErrorCode;
import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends IllegalArgumentException {

    private final List<ErrorCode> errors;

    public ValidationException(List<ErrorCode> errors, String message) {
      super(message);
      this.errors = errors;
    }
}
