package com.reviewservice.helper;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.net.http.*;
import java.net.URI;
import java.io.IOException;

@Slf4j
public class VaultAESHelper {

    private static final String VAULT_ADDR = "http://localhost:8200";
    private static final String VAULT_TOKEN = "devroot";
    private static final String SECRET_PATH = "/v1/secret/data/aes-key";

    public static String fetchKeyFromVault() throws IOException, InterruptedException {
        log.debug("Fetching AES key from Vault at: {}", VAULT_ADDR + SECRET_PATH);
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VAULT_ADDR + SECRET_PATH))
                .header("X-Vault-Token", VAULT_TOKEN)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Simple extraction assuming the response has: {"data":{"data":{"value":"hexkey"}}}
        String body = response.body();
        int start = body.indexOf("\"value\":\"") + 9;
        int end = body.indexOf("\"", start);
        String hexKey = body.substring(start, end);

        log.debug("Successfully fetched AES key from Vault");
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

    public static String encrypt(String plaintext, byte[] keyBytes) throws Exception {
        log.debug("Encrypting plaintext using AES");
        
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String ciphertext, byte[] keyBytes) throws Exception {
        log.debug("Decrypting ciphertext using AES");
        
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }

    public static void main(String[] args) throws Exception {
        log.info("Starting VaultAESHelper test");
        
        String hexKey = fetchKeyFromVault();
        byte[] key = hexToBytes(hexKey);

        String plaintext = "Hello Vault!";
        String encrypted = encrypt(plaintext, key);
        String decrypted = decrypt(encrypted, key);

        log.info("Original: {}", plaintext);
        log.info("Encrypted: {}", encrypted);
        log.info("Decrypted: {}", decrypted);
        
        log.info("VaultAESHelper test completed successfully");
    }
}