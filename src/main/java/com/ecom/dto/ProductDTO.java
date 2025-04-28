package com.ecom.dto;

import com.ecom.model.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductDTO {
    private Integer id;


    private String title;


    private String description;

    private String category;

    private String price;

    private int stock;

    private String image;

    private int discount;

    private String discountPrice;

    private Boolean isActive;

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.description = product.getDescription();
        this.category = product.getCategory();
        this.stock = product.getStock();
        this.image = product.getImage();
        this.discount = product.getDiscount();
        this.isActive = product.getIsActive();
    }
}
