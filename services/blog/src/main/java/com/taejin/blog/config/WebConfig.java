package com.taejin.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * CORS is handled by SecurityConfig to avoid conflicts with Spring Security's filter chain.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
