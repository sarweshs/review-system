package com.reviewservice.service;

import com.reviewservice.helper.VaultAESHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Service
public class EncryptionService {

    private static final String REDIS_KEY = "vault:aes-key";
    private static final String ALGORITHM = "AES";
    private final StringRedisTemplate redisTemplate;
    private byte[] keyBytes;

    @Autowired
    public EncryptionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.keyBytes = loadKey();
    }

    private byte[] loadKey() {
        String hexKey = redisTemplate.opsForValue().get(REDIS_KEY);
        if (hexKey == null) {
            try {
                hexKey = VaultAESHelper.fetchKeyFromVault();
                // Optionally set an expiry (e.g., 24 hours)
                redisTemplate.opsForValue().set(REDIS_KEY, hexKey);
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch AES key from Vault", e);
            }
        }
        return VaultAESHelper.hexToBytes(hexKey);
    }

    public String encrypt(String data) {
        try {
            return VaultAESHelper.encrypt(data, keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            return VaultAESHelper.decrypt(encryptedData, keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
} 