package com.ecom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${path.img.profile}")
    private String pathImgProfile;
    @Value("${path.img.product}")
    private String pathImgProduct;
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/profile_img/**")
                .addResourceLocations("file:"+pathImgProfile);

        registry.addResourceHandler("/product_img/**")
                .addResourceLocations("file:"+pathImgProduct);
    }
}
