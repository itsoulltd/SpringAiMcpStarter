package com.infoworks.mcp.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.infoworks.mcp.webapp.config"
		, "com.infoworks.mcp.webapp.filters"
		, "com.infoworks.mcp.domain"
		, "com.infoworks.mcp.services"
		, "com.infoworks.mcp.controllers" })
public class SpringAiMcpStarter extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiMcpStarter.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(SpringAiMcpStarter.class);
	}

}
