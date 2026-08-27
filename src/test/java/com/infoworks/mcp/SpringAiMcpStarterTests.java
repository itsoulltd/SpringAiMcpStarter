package com.infoworks.mcp;

import com.infoworks.mcp.config.TestJPAH2Config;
import com.infoworks.mcp.webapp.config.BeanConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {SpringAiMcpStarterTests.class, BeanConfig.class, TestJPAH2Config.class})
//@ComponentScan(basePackages = {"com.infoworks.mcp.controllers", "com.infoworks.mcp.domain", "com.infoworks.mcp.services"})
class SpringAiMcpStarterTests {

	@Test
	void contextLoads() {
		System.out.println("Loaded");
	}

}
