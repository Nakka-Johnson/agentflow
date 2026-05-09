package com.nakka.agentflow;

import com.nakka.agentflow.config.BedrockProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentflowApplication.class, args);
	}

}
