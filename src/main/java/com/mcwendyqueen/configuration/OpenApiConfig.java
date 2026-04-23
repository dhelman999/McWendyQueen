package com.mcwendyqueen.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "McWendyQueen",
        version = "v1",
        description = "Personal fast food management APP",
        contact = @Contact(name = "David Helman"),
        license = @License(name = "Internal Use")
    ),
    servers = {
        @Server(url = "http://localhost:3000", description = "Local development server")
    }
)
public class OpenApiConfig {
}
