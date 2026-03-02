package com.ftn.research.knowledgeuniverse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("uploads/**")
//                .addResourceLocations("file:uploads/"); // Map /uploads/ to the local 'uploads' directory
//    }


    // Define messageSource, default Spring's Bean for resolving messages
    @Bean(name= {"messageSource"})
    public ResourceBundleMessageSource messageSource() {

        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        // directory/property-prefix where Spring can look for messages
        source.setBasenames("messages/messages");
        // write only the key of the message if its content is not present
        source.setUseCodeAsDefaultMessage(true);
        source.setDefaultEncoding("UTF-8");
        // set default localization for the App
        source.setDefaultLocale(Locale.ENGLISH);
        return source;
    }

    //LocaleResolver određuju lokalizaciju na osnovu podataka iz HTTP zahteva
    //SessionLocaleResolver - određuje lokalizaciju i skladišti je u HttpSession klijenta
    @Bean
    public LocaleResolver localeResolver() {
//        // keep language info in http session
//        SessionLocaleResolver res = new SessionLocaleResolver();
//        // set fallback localization
//        res.setDefaultLocale(Locale.forLanguageTag("en"));
//        return res;

        // keep language info in http cookies
        // else it invalidates with the whole http session on logout
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        // set fallback localization
        resolver.setDefaultLocale(Locale.ENGLISH);
        // optional: set a custom cookie name
        resolver.setCookieName("locale");
        // optional: persist for 1 day
        resolver.setCookieMaxAge(3600 * 24 * 1);
        return resolver;
    }

    // Interceptor that allows for changing the current locale on every request,
    // via a configurable request parameter (default param name: "locale").
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("locale");
        return interceptor;
    }

    // Registering an interceptor with Spring(Boot) by implementing WebMvcConfigurer interface
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

}
