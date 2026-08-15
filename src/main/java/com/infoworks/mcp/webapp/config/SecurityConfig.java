package com.infoworks.mcp.webapp.config;

import com.infoworks.connect.JDBCDriverClass;
import com.infoworks.mcp.webapp.filters.AuthorizationFilter;
import com.infoworks.mcp.webapp.filters.ByPassAuthorizationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.disable.security}")
    private boolean disableSecurity;

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    public static final String[] URL_WHITELIST = {
            "/v2/api-docs"
            , "/swagger-ui.html"
            , "/swagger-ui.html/**"
            , "/webjars/springfox-swagger-ui/**"
            , "/swagger-resources/**"
            , "/swagger-resources/configuration/**"
            , "/actuator/health"
            , "/actuator/prometheus"
            , "/h2-console/**"
            , "/v3/api-docs/**"
            , "/swagger-ui/**"
    };

    @Value("${spring.datasource.driver-class-name}")
    String activeDriverClass;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(URL_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        (disableSecurity ? new ByPassAuthorizationFilter() : new AuthorizationFilter())
                        , BasicAuthenticationFilter.class);
        //Disable for H2 DB:
        if (activeDriverClass.equalsIgnoreCase(JDBCDriverClass.H2_EMBEDDED.toString())){
            http.headers(hdr -> hdr.frameOptions(opts -> opts.disable()));
        }
        return http.build();
    }

}
