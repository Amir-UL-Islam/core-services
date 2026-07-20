package com.central.security.core.security.oauth.clientcredentials.config;

import com.central.security.core.security.oauth.clientcredentials.interceptor.ClientCredentialsOnlyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ClientCredentialsInterceptorConfig implements WebMvcConfigurer {

    private final ClientCredentialsOnlyInterceptor clientCredentialsOnlyInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(clientCredentialsOnlyInterceptor).addPathPatterns("/api/**");
    }
}

