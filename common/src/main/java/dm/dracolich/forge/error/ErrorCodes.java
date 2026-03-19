package dm.dracolich.forge.error;

import lombok.Getter;

@Getter
public enum ErrorCodes {
    DMD001("DMD001", "Unknown issue happened."),
    DMD002("DMD002", "Error while creating [%s]={%s}"),
    DMD003("DMD003", "[%s] cannot be null or empty."),
    DMD004("DMD004", "Level not allowed for this operation. Character level={%s}, allowed levels=[%s]."),
    DMD005("DMD005", "Only one class can be added by level. Character level={%s}, allowed classes=[%s]."),
    DMD006("DMD006", "{%s} is not an allowed value for [%s]."),
    DMD007("DMD007", "Total class levels must equal character level. Total class levels={%s}, character level={%s}."),
    DMD008("DMD008", "Total {%s} count must be {%s}."),
    DMD009("DMD009", "{%s} [%s]={%s} not found."),
    DMD010("DMD010", "Failed to upload image to Cloudinary: %s"),
    DMD011("DMD011", "Failed to delete image from Cloudinary: %s");

    private final String code;
    private final String message;

    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String format(String... args) {
        return String.format(message, args);
    }
}
