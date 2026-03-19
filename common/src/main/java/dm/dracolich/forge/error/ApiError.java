package dm.dracolich.forge.error;

import lombok.Data;

@Data
public class ApiError {
    private ErrorCode error;
    private ErrorSeverity severity;
    private String field;

    public ApiError(ErrorCode error, ErrorSeverity severity, String field) {
        this.error = error;
        this.severity = severity;
        this.field = field;
    }

    public ApiError(ErrorCode error) {
        this.error = error;
    }
}
