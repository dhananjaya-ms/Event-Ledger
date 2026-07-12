package com.dj.account.config;

import com.dj.account.metrics.MetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration to register the metrics interceptor
 */
@Configuration
public class MetricsConfig implements WebMvcConfigurer {

    private final MetricsInterceptor metricsInterceptor;

    public MetricsConfig(MetricsInterceptor metricsInterceptor) {
        this.metricsInterceptor = metricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metricsInterceptor);
    }
}
