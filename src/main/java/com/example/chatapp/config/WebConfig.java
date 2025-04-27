package com.example.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置外部資源目錄 (使檔案系統中的uploads目錄可以通過/uploads路徑訪問)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .resourceChain(true)
                .addResolver(new ResourceResolver() {
                    @Override
                    public Resource resolveResource(HttpServletRequest request, String requestPath,
                                                    List<? extends Resource> locations, ResourceResolverChain chain) {
                        Resource resource = chain.resolveResource(request, requestPath, locations);
                        return resource != null ? resource : null;
                    }

                    @Override
                    public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
                        return chain.resolveUrlPath(resourcePath, locations);
                    }
                });

        // 保留預設的靜態資源處理
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}