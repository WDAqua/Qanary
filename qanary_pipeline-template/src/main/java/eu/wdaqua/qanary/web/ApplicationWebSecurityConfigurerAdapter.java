package eu.wdaqua.qanary.web;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * configure Spring Security: CSRF protection is disabled
 */
@EnableWebSecurity
public class ApplicationWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();
	}
}
