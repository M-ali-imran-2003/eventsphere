package com.example.eventsphere;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling
@EnableAsync // This unlocks background thread pools// Add this to turn on the background worker
public class EventsphereApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventsphereApplication.class, args);
	}
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Karachi"));
	}
}
