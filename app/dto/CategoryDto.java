package dto;

import model.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yakov_000 on 04.02.2015.
 */
public class CategoryDto {

    private final String name;
    private final String displayName;
    private final List<CategoryDto> children;

    public CategoryDto(Category category) {
        this.name = category.getName();
        this.displayName = category.getDisplayName();
        this.children = new ArrayList<>();
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
