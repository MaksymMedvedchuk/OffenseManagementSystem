package com.arestmanagement.config;

import com.arestmanagement.security.filter.JwtAuthorizationFilter;
import com.arestmanagement.security.model.CustomUserServiceDetails;
import com.arestmanagement.security.service.AccessTokenInvalidationService;
import com.arestmanagement.security.service.JwtTokenAuthenticationService;
import com.arestmanagement.security.tokengenerator.TokenSecretKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebConfig {

	private static final String[]
		SWAGGER_PATHS =
		{"/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**", "/webjars/swagger-ui/**"};

	private final CustomUserServiceDetails customUserServiceDetails;

	private final JwtTokenAuthenticationService jwtTokenAuthenticationService;

	private final ObjectMapper objectMapper;

	private final TokenSecretKey tokenSecretKey;

	private final AccessTokenInvalidationService accessTokenInvalidationService;

	public WebConfig(
		final CustomUserServiceDetails customUserServiceDetails,
		final JwtTokenAuthenticationService jwtTokenAuthenticationService,
		final ObjectMapper objectMapper,
		final TokenSecretKey tokenSecretKey,
		final AccessTokenInvalidationService accessTokenInvalidationService
	) {
		this.customUserServiceDetails = customUserServiceDetails;
		this.jwtTokenAuthenticationService = jwtTokenAuthenticationService;
		this.objectMapper = objectMapper;
		this.tokenSecretKey = tokenSecretKey;
		this.accessTokenInvalidationService = accessTokenInvalidationService;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http.csrf().disable()
			.authorizeRequests(authorizeRequests ->
				authorizeRequests
					.requestMatchers(SWAGGER_PATHS).permitAll()
					.requestMatchers(HttpMethod.DELETE, "/v1.0/user/delete/*").hasRole("PERSONE")
					.requestMatchers(HttpMethod.POST, "/arrestManagement/create_arrest").hasRole("ADMIN")
					.requestMatchers(HttpMethod.GET, "/arrestManagement/get_arrest/*").hasAnyRole("ADMIN", "PERSONE")
					.requestMatchers(HttpMethod.POST,
						"/v1.0/auth/registration",
						"/v1.0/auth/login",
						"/v1.0/auth/verify_user",
						"/v1.0/token/generate-access-token",
						"/v1.0/token/generate-refresh-token",
						"/v1.0/email"
						).permitAll()
					.anyRequest().authenticated()
			)
			.sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
			.addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public JwtAuthorizationFilter jwtAuthorizationFilter() {
		return new JwtAuthorizationFilter(customUserServiceDetails, jwtTokenAuthenticationService, tokenSecretKey,
			accessTokenInvalidationService
		);
	}
}
