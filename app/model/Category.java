package model;

/**
 * Created by yakov_000 on 10.03.2015.
 */
public class Category {

    private Long id;
    private String name;
    private String displayName;
    private Long parentId;

    public Category(Long id, String name, String displayName, Long parentId) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.parentId = parentId;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
