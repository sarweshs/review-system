package com.reviewservice.helper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VaultAESHelperTest {
    @Test
    void testHexToBytes_encrypt_decrypt() throws Exception {
        String hexKey = "00112233445566778899aabbccddeeff";
        byte[] keyBytes = VaultAESHelper.hexToBytes(hexKey);
        String plaintext = "Hello, World!";
        String encrypted = VaultAESHelper.encrypt(plaintext, keyBytes);
        String decrypted = VaultAESHelper.decrypt(encrypted, keyBytes);
        assertEquals(plaintext, decrypted);
    }
} 