package com.demo.homeloan.configuration;

import com.demo.homeloan.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

  @Bean
  public UserDetailsService userDetailsService(UserRepository users) {
    return username -> {
      var user = users.findByEmail(username.trim().toLowerCase(Locale.ROOT))
          .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
      return User.withUsername(user.getEmail()).password(user.getPasswordHash())
          .roles(user.getRole().name()).disabled(!user.isEnabled()).build();
    };
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider(UserDetailsService details, PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(details);
    provider.setPasswordEncoder(encoder);
    return provider;
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http, DaoAuthenticationProvider provider,
      @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
    // JSON errors keep the SPA in control of login; no browser-native credential popup.
    AuthenticationEntryPoint unauthorized = (request, response, error) -> {
      response.setStatus(401);
      response.setContentType("application/json");
      response.getWriter().write("{\"status\":401,\"message\":\"Invalid credentials or authentication required\"}");
    };
    http.csrf(c -> c.disable())
        .cors(c -> c.configurationSource(cors))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .logout(c -> c.disable())
        .authenticationProvider(provider)
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/loan-products", "/api/loan-products/*").permitAll()
            .anyRequest().authenticated())
        .httpBasic(b -> b.authenticationEntryPoint(unauthorized))
        .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized)
            .accessDeniedHandler((request, response, error) -> {
              response.setStatus(403);
              response.setContentType("application/json");
              response.getWriter().write("{\"status\":403,\"message\":\"Access denied\"}");
            }));
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(@Value("${app.allowed-origins}") String origins) {
    var cors = new CorsConfiguration();
    cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
    cors.setExposedHeaders(List.of("Location", "Content-Disposition"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }
}
