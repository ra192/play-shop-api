package dto;

/**
 * Created by yakov_000 on 20.02.2015.
 */
public class PropertyValueWithCountDto extends PropertyValueDto {

    private Long count;

    public PropertyValueWithCountDto(String name, String displayName,Long count) {
        super(name, displayName);
        this.count=count;
    }

    public Long getCount() {
        return count;
    }
}
