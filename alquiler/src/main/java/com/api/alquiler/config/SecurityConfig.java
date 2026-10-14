package com.api.alquiler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Desactiva CSRF para permitir POST/PUT/DELETE
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Permite el acceso sin contraseña a todos los endpoints
            );
        return http.build();
    }
}