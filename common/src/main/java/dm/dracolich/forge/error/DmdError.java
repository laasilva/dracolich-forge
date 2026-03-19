package dm.dracolich.forge.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DmdError implements Serializable {
    @Serial
    private static final long serialVersionUID = 20230928133348L;

    private String code;
    private String message;

    public static DmdError build(String code, String message) {
        return new DmdError(code, message);
    }
}
