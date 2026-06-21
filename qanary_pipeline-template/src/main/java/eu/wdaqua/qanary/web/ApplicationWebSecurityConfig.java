package eu.wdaqua.qanary.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

/**
 * Configure access to specific URLs of the application, using application.properties.
 */
@Configuration
@EnableWebSecurity
public class ApplicationWebSecurityConfig {
	private final Logger logger = LoggerFactory.getLogger(ApplicationWebSecurityConfig.class);
	private String access;
	private String username;
	private String password;
	private boolean passwordProtected;
	private String[] publicUrls = new String[] { // CSRF protection is disabled for these URLs
		"/*question*",
		"/qa",
		"/gerbil*/**",
		"/login",
		"/instances",
		"/applications/**",
		"/sparql",
		"/explain"
	};

	public ApplicationWebSecurityConfig(@Autowired Environment env) {
		this.setAccessConfiguration(env);
	}

	private void setAccessConfiguration(Environment env) {
		String access = env.getProperty(QanaryConfigurationAccessParameters.ACCESSKEY, "");
		String username = env.getProperty(QanaryConfigurationAccessParameters.USERNAMEKEY, "");
		String password = env.getProperty(QanaryConfigurationAccessParameters.PASSWORDKEY, "");
		this.access = access;
		this.username = username;
		this.password = password;
		if (username.length() == 0 ) {
			this.passwordProtected = false;
		} else {this.passwordProtected = true;}
	}

	@Bean
	public SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
		// Spring Security 6 loads the CSRF token lazily (deferred). The Spring Boot Admin
		// UI template sba-settings.js reads ${_csrf.parameterName} eagerly while rendering,
		// which makes the deferred supplier throw and truncates the response (blank SBA
		// dashboard). Opting out of deferred loading (request-attribute name = null) makes
		// the token available at render time, restoring the dashboard.
		XorCsrfTokenRequestAttributeHandler csrfRequestHandler = new XorCsrfTokenRequestAttributeHandler();
		csrfRequestHandler.setCsrfRequestAttributeName(null);
		http.csrf(csrf -> csrf
				.csrfTokenRequestHandler(csrfRequestHandler)
				.ignoringRequestMatchers(publicUrls));

		switch(access) {
			case QanaryConfigurationAccessParameters.DISALLOWACCESS:
				http
					.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/").denyAll()
						.requestMatchers(QanaryConfigurationAccessParameters.CONFIGURATIONENDPOINT).denyAll()
						.requestMatchers(QanaryConfigurationAccessParameters.APPLICATIONSENDPOINT).denyAll()
						.anyRequest().permitAll());
				break;
			case QanaryConfigurationAccessParameters.WEBACCESS:
				if (this.passwordProtected) {
					http
						.authorizeHttpRequests(authorize -> authorize
							.requestMatchers("/").authenticated()
							.requestMatchers(QanaryConfigurationAccessParameters.CONFIGURATIONENDPOINT).authenticated()
							.requestMatchers(QanaryConfigurationAccessParameters.APPLICATIONSENDPOINT).authenticated()
							.anyRequest().permitAll())
						.formLogin(form -> form
							.loginPage(QanaryConfigurationAccessParameters.LOGINENDPOINT)
							.permitAll())
						.logout(logout -> logout
							.permitAll());
					break;
				} else {
					http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
					break;
				}
			default:
				throw new Exception("undefined access type");
		}
		return http.build();
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsService() {
		if (!this.passwordProtected) {
			return new InMemoryUserDetailsManager();
		}
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		UserDetails user = User.withUsername(username)
				.password(encoder.encode(password))
				.roles("USER")
				.build();
		return new InMemoryUserDetailsManager(user);
	}
}
