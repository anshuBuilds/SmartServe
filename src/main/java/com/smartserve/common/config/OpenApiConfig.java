package com.smartserve.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "SmartServe API",
				version = "v1",
				description = "Analytics-first restaurant order and kitchen management backend"
		)
)
public class OpenApiConfig {
}
