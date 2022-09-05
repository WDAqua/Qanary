package eu.wdaqua.qanary.web;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

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
		String access = env.getProperty(QanaryConfigurationAccessParameters.ACCESSKEY);
		String username = env.getProperty(QanaryConfigurationAccessParameters.USERNAMEKEY);
		String password = env.getProperty(QanaryConfigurationAccessParameters.PASSWORDKEY);
		this.access = (access.length()==0)?QanaryConfigurationAccessParameters.DEFAULTACCESSTYPE:access;
		this.username = (username.length()==0)?QanaryConfigurationAccessParameters.DEFAULTUSERNAME:username;
		this.password = (password.length()==0)?QanaryConfigurationAccessParameters.DEFAULTPASSWORD:password;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();

        switch(access) {
            case QanaryConfigurationAccessParameters.DISALLOWACCESS:
	    		http
	    			.authorizeRequests()
	    				.antMatchers("/").denyAll()
						.antMatchers(QanaryConfigurationAccessParameters.CONFIGURATIONENDPOINT).denyAll()
	    				.anyRequest().permitAll();
				break;
            case QanaryConfigurationAccessParameters.WEBACCESS:
	    		http
	    			.authorizeRequests()
	    				.antMatchers("/").authenticated() 
						.antMatchers(QanaryConfigurationAccessParameters.CONFIGURATIONENDPOINT).authenticated()
	    				.anyRequest().permitAll()
	    				.and() 
	    			.formLogin()
	    				.loginPage(QanaryConfigurationAccessParameters.LOGINENDPOINT)
	    				.permitAll()
	    				.and()
	    			.logout()
	    				.permitAll();
				break;
            default:
                throw new Exception("undefined access type");
        }
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		auth.inMemoryAuthentication()
			.withUser(username)
			.password(encoder.encode(password))
			.roles("USER");
	}
}
