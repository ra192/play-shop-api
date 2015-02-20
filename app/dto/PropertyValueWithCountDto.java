package dto;

/**
 * Created by yakov_000 on 20.02.2015.
 */
public class PropertyValueWithCountDto extends PropertyValueDto {

    private Long count;

    public PropertyValueWithCountDto(Long id, String name, String displayName, Long propertyId,Long count) {
        super(id, name, displayName, propertyId);
        this.count=count;
    }

    public Long getCount() {
        return count;
    }
}
