package dm.dracolich.forge.exception;

import dm.dracolich.forge.error.ErrorCodes;
import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends IllegalArgumentException {

    private final List<ErrorCodes> errors;

    public ValidationException(List<ErrorCodes> errors, String message) {
      super(message);
      this.errors = errors;
    }
}
