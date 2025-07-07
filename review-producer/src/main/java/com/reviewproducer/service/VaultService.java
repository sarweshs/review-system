package com.reviewproducer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
public class VaultService {

    private static final String REDIS_KEY = "vault:aes-key";
    private static final String ALGORITHM = "AES";
    
    @Value("${vault.host:localhost}")
    private String vaultHost;
    
    @Value("${vault.port:8200}")
    private int vaultPort;
    
    @Value("${vault.token:devroot}")
    private String vaultToken;
    
    @Value("${vault.scheme:http}")
    private String vaultScheme;
    
    @Value("${vault.secret-path:/v1/secret/data/aes-key}")
    private String secretPath;
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private byte[] keyBytes;

    public VaultService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // Debug: Log configuration values
        log.info("VaultService initialization - vaultScheme: '{}', vaultHost: '{}', vaultPort: {}, vaultToken: '{}', secretPath: '{}'",
                vaultScheme, vaultHost, vaultPort, vaultToken != null ? "***" : "null", secretPath);
        
        // Validate configuration before loading key
        validateConfiguration();
        
        this.keyBytes = loadKey();
    }
    
    private void validateConfiguration() {
        if (vaultScheme == null || vaultScheme.trim().isEmpty()) {
            log.error("Vault scheme is null or empty. Current value: '{}'", vaultScheme);
            throw new RuntimeException("Vault scheme is null or empty. Check vault.scheme configuration.");
        }
        if (vaultHost == null || vaultHost.trim().isEmpty()) {
            log.error("Vault host is null or empty. Current value: '{}'", vaultHost);
            throw new RuntimeException("Vault host is null or empty. Check vault.host configuration.");
        }
        if (vaultToken == null || vaultToken.trim().isEmpty()) {
            log.error("Vault token is null or empty. Current value: '{}'", vaultToken);
            throw new RuntimeException("Vault token is null or empty. Check vault.token configuration.");
        }
        if (secretPath == null || secretPath.trim().isEmpty()) {
            log.error("Vault secret path is null or empty. Current value: '{}'", secretPath);
            throw new RuntimeException("Vault secret path is null or empty. Check vault.secret-path configuration.");
        }
        
        log.info("Vault configuration validation passed");
    }


    private byte[] loadKey() {
        String hexKey = redisTemplate.opsForValue().get(REDIS_KEY);
        if (hexKey == null || hexKey.trim().isEmpty()) {
            try {
                hexKey = fetchKeyFromVault();
                // Cache the key for 24 hours
                redisTemplate.opsForValue().set(REDIS_KEY, hexKey);
                log.info("Fetched and cached AES key from Vault");
            } catch (Exception e) {
                log.error("Failed to fetch AES key from Vault", e);
                throw new RuntimeException("Failed to fetch AES key from Vault", e);
            }
        }
        return hexToBytes(hexKey);
    }

    public String fetchKeyFromVault() throws Exception {
        String vaultUrl = vaultScheme + "://" + vaultHost + ":" + vaultPort + secretPath;
        log.info("Attempting to fetch AES key from Vault at: {}", vaultUrl);
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(vaultUrl))
                .header("X-Vault-Token", vaultToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Vault request failed with status: " + response.statusCode());
        }

        // Parse the Vault response
        JsonNode root = objectMapper.readTree(response.body());
        String hexKey = root.path("data").path("data").path("value").asText();
        
        if (hexKey == null || hexKey.trim().isEmpty()) {
            throw new RuntimeException("No AES key found in Vault response");
        }

        return hexKey;
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return bytes;
    }

    public String encrypt(String plaintext) {
        try {
            return encrypt(plaintext, keyBytes);
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            return decrypt(encryptedData, keyBytes);
        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new RuntimeException("Error decrypting data", e);
        }
    }

    private String encrypt(String plaintext, byte[] keyBytes) throws Exception {
        javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(keyBytes, ALGORITHM);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decrypt(String ciphertext, byte[] keyBytes) throws Exception {
        javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(keyBytes, ALGORITHM);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
} 