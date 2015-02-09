package dto;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class ProductDto {

    private Long id;
    private String code;
    private String displayName;
    private Double price;
    private String description;
    private String imageUrl;

    public ProductDto(Long id, String code, String displayName, Double price, String description, String imageUrl) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
