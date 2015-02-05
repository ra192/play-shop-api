package dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yakov_000 on 04.02.2015.
 */
public class CategoryDto {

    private Long id;
    private String name;
    private String displayName;
    private List<CategoryDto> children;

    public CategoryDto(Long id, String name, String displayName) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.children = new ArrayList<>();
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

    public List<CategoryDto> getChildren() {
        return children;
    }
}
