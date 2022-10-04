package eu.wdaqua.qanary.web;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private boolean passwordProtected;
	private String[] publicUrls = new String[] {
		"/*question*",
		"/qa",
		"/gerbil",
		"/login",
		"/instances",
		"/sparql"
	};

	public ApplicationWebSecurityConfigurerAdapter(@Autowired Environment env) {
		this.setAccessConfiguration(env);
	}

	private void setAccessConfiguration(Environment env) {
		String access = env.getProperty(QanaryConfigurationAccessParameters.ACCESSKEY, "");
		String username = env.getProperty(QanaryConfigurationAccessParameters.USERNAMEKEY, "");
		String password = env.getProperty(QanaryConfigurationAccessParameters.PASSWORDKEY, "");
		this.access = access;
		this.username = username;
		this.password = password;
		if (username.length() == 0 || username.length() == 0) {
			this.passwordProtected = false;
		} else {this.passwordProtected = true;}
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().ignoringAntMatchers(publicUrls);

        switch(access) {
            case QanaryConfigurationAccessParameters.DISALLOWACCESS:
	    		http
	    			.authorizeRequests()
	    				.antMatchers("/").denyAll()
						.antMatchers(QanaryConfigurationAccessParameters.CONFIGURATIONENDPOINT).denyAll()
	    				.anyRequest().permitAll();
				break;
            case QanaryConfigurationAccessParameters.WEBACCESS:
				if (this.passwordProtected) {
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
				} else {
					http.authorizeRequests().anyRequest().permitAll();
					break;
				}
            default:
                throw new Exception("undefined access type");
        }
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		if (!this.passwordProtected) {
			return;
		}
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		auth.inMemoryAuthentication()
			.withUser(username)
			.password(encoder.encode(password))
			.roles("USER");
	}
}
