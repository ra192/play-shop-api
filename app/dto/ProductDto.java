package dto;

import model.Category;
import model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class ProductDto {

    private String code;
    private String displayName;
    private Double price;
    private String description;
    private String imageUrl;
    private String category;
    private List<String> propertyValues;

    public ProductDto(Product product, Category category) {
        this.code = product.getCode();
        this.displayName = product.getDisplayName();
        this.price = product.getPrice();
        this.description = product.getDescription();
        this.imageUrl = product.getImageUrl();
        this.category=category.getName();
        this.propertyValues=new ArrayList<>();
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

    public String getCategory() {
        return category;
    }

    public List<String> getPropertyValues() {
        return propertyValues;
    }
}
