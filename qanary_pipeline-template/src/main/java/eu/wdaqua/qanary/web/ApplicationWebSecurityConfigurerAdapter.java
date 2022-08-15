package eu.wdaqua.qanary.web;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * configure Spring Security: CSRF protection is disabled
 */
@EnableWebSecurity
public class ApplicationWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	private String access;
	private String username;
	private String password;

	public ApplicationWebSecurityConfigurerAdapter(@Autowired Environment env) {
		this.setAccessConfiguration(env);
	}

	private void setAccessConfiguration(Environment env) {
		String access = env.getProperty("configuration.access", "disallow");
		String username = env.getProperty("configuration.username", "admin");
		String password = env.getProperty("configuration.password", "admin");
		this.access = (access.length()==0)?"disallow":access;
		this.username = (username.length()==0)?"admin":username;
		this.password = (password.length()==0)?"admin":password;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();

        switch(access) {
            case "disallow":
	    		http
	    			.authorizeRequests()
	    				.antMatchers("/").denyAll()
						.antMatchers("/configuration").denyAll()
	    				.anyRequest().permitAll();
				break;
            case "web":
	    		http
	    			.authorizeRequests()
	    				.antMatchers("/").authenticated() 
						.antMatchers("/configuration").authenticated()
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
			 User.withDefaultPasswordEncoder()
				.username(username)
				.password(password)
				.roles("USER")
				.build();

		return new InMemoryUserDetailsManager(user);
	}
}
