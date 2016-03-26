package eu.wdaqua.qanary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import de.codecentric.boot.admin.config.EnableAdminServer;

@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient // registers itself as client for the admin server,
						// removeable
public class QanaryPipeline {

	public static void main(String[] args) {
		SpringApplication.run(QanaryPipeline.class, args);
	}

}
