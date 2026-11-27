package com.example.ubmager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())

                .addPathPatterns(
        "/profil/**", "/pesanan/**", "/review/**",
        "/jasa/**", "/pemesanan/**", "/pembayaran/**",
        "/admin/**", "/chat/**", "/notifikasi/**",
        "/uploads/**", "/api/chat/**", "/api/notifikasi/**"
)
.excludePathPatterns("/admin/login");
}

    // Gambar chat yang diunggah disajikan dari folder uploads/
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/");
    }
}
