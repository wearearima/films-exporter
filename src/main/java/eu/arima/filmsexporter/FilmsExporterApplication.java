package eu.arima.filmsexporter;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class FilmsExporterApplication {

	public static void main(String[] args) {
		SpringApplication.run(FilmsExporterApplication.class, args);
	}

}
