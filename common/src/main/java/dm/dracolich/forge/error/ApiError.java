package dm.dracolich.forge.error;

import lombok.Data;

@Data
public class ApiError {
    private ErrorCodes error;
    private ErrorSeverity severity;
    private String field;

    public ApiError(ErrorCodes error, ErrorSeverity severity, String field) {
        this.error = error;
        this.severity = severity;
        this.field = field;
    }

    public ApiError(ErrorCodes error) {
        this.error = error;
    }
}
