#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {
	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/
	@Bean
	public QanaryComponent qanaryComponent(
			@Value("${spring.application.name}") final String applicationName) {
		return new ${classname}(applicationName);
	}

	@Autowired
	public QanaryComponentConfiguration qanaryComponentConfiguration; 

	public static void main(String[] args) { 
		SpringApplication.run(Application.class, args); 
	}

	@Bean
	public OpenAPI customOpenAPI(
			@Value("${spring.application.name}") final String appName) {
		String appVersion = getClass().getPackage().getImplementationVersion();
		// TODO: change the title and description as you see fit 
		return new OpenAPI().info(new Info() //
				.title("Qanary component " + appName) //
				.version(appVersion) //
				.description("Initial springdocs for Qanary component " + appName) //
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}
}
