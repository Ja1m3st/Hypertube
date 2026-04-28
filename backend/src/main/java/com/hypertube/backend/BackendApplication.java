package com.hypertube.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BackendApplication {
	public static void main(String[] args) {
        try {

            Dotenv dotenv = Dotenv.configure()
                                  .ignoreIfMissing()
                                  .load();

            if (dotenv.entries().size() > 0) {
                System.setProperty("DB_NAME", dotenv.get("DB_NAME"));
                System.setProperty("DB_URL", dotenv.get("DB_URL"));
                System.setProperty("DB_USER", dotenv.get("DB_USER"));
                System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
                System.setProperty("JWT_KEY", dotenv.get("JWT_KEY"));
                System.setProperty("JWT_EXPIRATION", dotenv.get("JWT_EXPIRATION"));
                System.setProperty("TMDB_KEY", dotenv.get("TMDB_KEY"));
                System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
                System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));
                System.setProperty("FORTYTWO_CLIENT_ID", dotenv.get("FORTYTWO_CLIENT_ID"));
                System.setProperty("FORTYTWO_CLIENT_SECRET", dotenv.get("FORTYTWO_CLIENT_SECRET"));
            }
            
        } catch (Exception e) {
        }

        SpringApplication.run(BackendApplication.class, args);
    }

}
