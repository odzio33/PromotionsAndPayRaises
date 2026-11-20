package com.example.payraisemodule.config;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/** Wires a JSON-API-friendly session security chain (no HTML form-login flow). */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    // No CSRF token exists yet before the first successful login, so the
                    // login endpoint itself must be exempt — it's the one request that can
                    // never carry a token issued by a prior response. The H2 console is a
                    // local-dev-only tool (disabled outside the default profile) with its
                    // own form handling, so it's exempted too.
                    .ignoringRequestMatchers("/api/login", "/h2-console/**"))
        // The H2 console renders itself in an iframe; the default DENY would block it.
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/login", "/actuator/health", "/h2-console/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (request, response, authException) -> {
                          writeJsonError(response, 401, "Authentication required");
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          writeJsonError(response, 403, "Access denied");
                        }))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(
            l ->
                l.logoutUrl("/api/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(200)));
    return http.build();
  }

  private static void writeJsonError(
      jakarta.servlet.http.HttpServletResponse response, int status, String message)
      throws IOException {
    response.setContentType("application/json");
    response.setStatus(status);
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }
}
