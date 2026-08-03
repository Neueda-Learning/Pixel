package com.pixel.portfolio.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI pixelOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Pixel Portfolio API")
						.description("Backend APIs for portfolio investment, trading, analytics, and AI assistance")
						.version("1.0.0")
						.contact(new Contact().name("Team Pixel")))
				.addServersItem(new Server().url("http://localhost:8080"))
				.externalDocs(new ExternalDocumentation().description("Pixel Project"));
	}
}

