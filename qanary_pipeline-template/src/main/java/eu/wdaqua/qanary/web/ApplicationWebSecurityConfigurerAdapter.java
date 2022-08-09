package eu.wdaqua.qanary.web;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * configure Spring Security: CSRF protection is disabled
 */
@EnableWebSecurity
public class ApplicationWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();

		// TODO: use application.properties
        String configurationAccess = "web";

        switch(configurationAccess) {
            case "disallow":
	    		http
	    			.authorizeRequests()
	    				.antMatchers("/").denyAll()
	    				.anyRequest().permitAll();
				break;
            case "web":
	    		http
	    			.authorizeRequests()
	    				.antMatchers("/").authenticated() 
	    				.anyRequest().permitAll()
	    				.and() 
	    			.formLogin()
	    				.loginPage("/login")
	    				.permitAll()
	    				.and()
	    			.logout()
	    				.permitAll();
				break;
            default:
                throw new Exception("undefined access type");
        }
	}

	@Bean
	@Override
	public UserDetailsService userDetailsService() {
		UserDetails user =
			// TODO: use correct encoder for production
			// TODO: use credentials from config
			 User.withDefaultPasswordEncoder()
				.username("admin")
				.password("admin")
				.roles("USER")
				.build();

		return new InMemoryUserDetailsManager(user);
	}
}
