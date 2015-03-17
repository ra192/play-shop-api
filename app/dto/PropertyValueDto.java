package dto;

/**
 * Created by yakov_000 on 10.02.2015.
 */
public class PropertyValueDto {

    private String name;
    private String displayName;

    public PropertyValueDto(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
