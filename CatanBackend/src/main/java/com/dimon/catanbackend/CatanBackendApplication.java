package com.dimon.catanbackend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Write Readme file
// Add Aspect
// Check if redis work
@SpringBootApplication
public class CatanBackendApplication {

    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure().load();

        // Set environment variables
        setSystemProperty("MAIL_USERNAME", dotenv.get("MAIL_USERNAME"));
        setSystemProperty("MAIL_PASSWORD", dotenv.get("MAIL_PASSWORD"));
        setSystemProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));
        setSystemProperty("CLIENT_ID", dotenv.get("CLIENT_ID"));
        setSystemProperty("CLIENT-SECRET", dotenv.get("CLIENT-SECRET"));
        setSystemProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        setSystemProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));

        SpringApplication.run(CatanBackendApplication.class, args);
    }

    private static void setSystemProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.err.println("Warning: Environment variable for " + key + " is not set.");
        }
    }

}
