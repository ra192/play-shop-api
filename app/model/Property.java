package model;

/**
 * Created by yakov_000 on 16.03.2015.
 */
public class Property {

    private Long id;
    private String name;
    private String displayName;

    public Property(Long id, String name, String displayName) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
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
}
