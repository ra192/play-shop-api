package model;

/**
 * Created by yakov_000 on 10.03.2015.
 */
public class PropertyValue {

    private Long id;
    private String name;
    private String displayName;
    private Long propertyId;

    public PropertyValue(Long id, String name, String displayName, Long propertyId) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.propertyId=propertyId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }
}
