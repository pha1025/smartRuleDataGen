package com.smartdata.smartruledatagen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 对所有接口生效
                // .allowedOrigins("https://your-vue-app.vercel.app") // 生产环境建议指定具体域名
                .allowedOriginPatterns("*") // 开发阶段允许所有来源，方便调试
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的方法
                .allowedHeaders("*")
                .allowCredentials(true) // 是否允许携带 cookie/认证信息
                .maxAge(3600); // 预检请求的有效期
    }
}
