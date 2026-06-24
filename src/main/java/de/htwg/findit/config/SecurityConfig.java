package de.htwg.findit.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contact-requests").permitAll()

                        .requestMatchers("/api/profile").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/items").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/items/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/items/**").authenticated()

                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/categories").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/contact-requests/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/contact-requests/**").authenticated()

                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.security.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .build();
    }
}