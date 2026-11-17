package com.demo.homeloan.configuration;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Home Loan Demo API")
                .version("1.0.0")
                .description(
                    "Fictional portfolio demo. Tenures are in months; rates are annual percentages."
                        + " Eligibility is preliminary, not approval."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "basicAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")))
        .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
  }
}
