package dm.dracolich.forge.error;

public interface ErrorCode {
    String getCode();
    String getMessage();

    default String format(String... args) {
        return String.format(getMessage(), (Object[]) args);
    }
}
