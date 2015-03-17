package dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yakov_000 on 23.02.2015.
 */
public class PropertyDto {

    private final String name;
    private final String displayName;
    private final List<PropertyValueDto>propertyValues;

    public PropertyDto(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.propertyValues=new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<PropertyValueDto> getPropertyValues() {
        return propertyValues;
    }
}
