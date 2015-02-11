package dto;

/**
 * Created by yakov_000 on 10.02.2015.
 */
public class PropertyValueDto {

    private Long id;
    private String name;
    private String displayName;
    private Long propertyId;

    public PropertyValueDto(Long id, String name, String displayName, Long propertyId) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.propertyId = propertyId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getPropertyId() {
        return propertyId;
    }
}
