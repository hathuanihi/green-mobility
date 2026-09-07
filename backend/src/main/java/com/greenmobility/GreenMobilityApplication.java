package com.greenmobility;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class GreenMobilityApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(GreenMobilityApplication.class, args);
    }

    private static void loadDotenv() {
        try {
            Dotenv dotenv = null;
            if (new File(".env").exists()) {
                dotenv = Dotenv.configure().ignoreIfMissing().load();
            } else if (new File("backend/.env").exists()) {
                dotenv = Dotenv.configure().directory("backend").ignoreIfMissing().load();
            } else if (new File("../backend/.env").exists()) {
                dotenv = Dotenv.configure().directory("../backend").ignoreIfMissing().load();
            }
            if (dotenv != null) {
                dotenv.entries().forEach(entry -> {
                    if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }
}
