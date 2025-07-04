package com.reviewdashboard.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Map;

@Configuration
public class EnvConfig {

    private final Environment environment;

    public EnvConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void loadEnvFile() {
        // Load .env file from the project root
        Dotenv dotenv = Dotenv.configure()
                .directory(".") // Look in the current directory
                .ignoreIfMissing() // Don't fail if .env file doesn't exist
                .load();

        // Set environment variables for AWS credentials
        if (dotenv.get("AWS_ACCESS_KEY_ID") != null) {
            setEnvironmentVariable("AWS_ACCESS_KEY_ID", dotenv.get("AWS_ACCESS_KEY_ID"));
        }
        if (dotenv.get("AWS_SECRET_ACCESS_KEY") != null) {
            setEnvironmentVariable("AWS_SECRET_ACCESS_KEY", dotenv.get("AWS_SECRET_ACCESS_KEY"));
        }
        if (dotenv.get("AWS_ENDPOINT") != null) {
            setEnvironmentVariable("AWS_ENDPOINT", dotenv.get("AWS_ENDPOINT"));
        }
        if (dotenv.get("AWS_REGION") != null) {
            setEnvironmentVariable("AWS_REGION", dotenv.get("AWS_REGION"));
        }
    }
    
    private void setEnvironmentVariable(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            // Fallback to system properties
            System.setProperty(key, value);
        }
    }
} 