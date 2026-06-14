package eu.wdaqua.qanary.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class QanaryWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/static").setViewName("static");
    }

    /**
     * Serve the embedded single-page frontend (static assets) under /qanary-ui/.
     * An explicit handler is needed because this pipeline is a Spring Boot Admin
     * server, whose UI takes over the default classpath:/static/ serving.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/qanary-ui/**")
                .addResourceLocations("classpath:/static/qanary-ui/");
    }

}