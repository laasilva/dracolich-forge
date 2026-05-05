package dm.dracolich.forge.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String error;
    private ErrorSeverity severity;
    private String field;

    public ApiError(ErrorCode code, ErrorSeverity severity, String field) {
        this.error = code.getCode();
        this.severity = severity;
        this.field = field;
    }

    public ApiError(ErrorCode code) {
        this.error = code.getCode();
    }
}
