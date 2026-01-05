package dm.dracolich.forge.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class Rollout {
    private String id;
    private Integer value;
    private Debug debug;

}
