package com.example.eventsphere;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;


@OpenAPIDefinition(
		info = @Info(
				title = "EventSphere API",
				version = "1.0",
				description = "API documentation for EventSphere"
		)
)
@SpringBootApplication
@EnableAutoConfiguration
public class EventsphereApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventsphereApplication.class, args);
	}
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Karachi"));
	}
}
