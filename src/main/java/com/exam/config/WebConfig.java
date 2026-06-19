package com.exam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // If the resource exists (like /assets/main.js) or if it's an API call, return it.
                        // Otherwise, return index.html to let React Router handle the deep link.
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        } else if (resourcePath.startsWith("api/") || resourcePath.startsWith("/api/")) {
                            return null;
                        } else {
                            return location.createRelative("index.html");
                        }
                    }
                });
    }
}
