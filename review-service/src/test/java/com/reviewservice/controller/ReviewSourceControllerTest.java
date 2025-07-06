package com.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcore.model.*;
import com.reviewservice.service.CredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Disabled;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Disabled("Disabled due to context issues")
@WebMvcTest(ReviewSourceController.class)
public class ReviewSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CredentialService credentialService;

    @Autowired
    private ObjectMapper objectMapper;

    private BasicCredential testCredential;

    @BeforeEach
    void setUp() {
        testCredential = new BasicCredential("testuser", "testpass");
    }

    @Test
    void testCreateBasicCredential() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("username", "testuser");
        params.put("password", "testpass");

        when(credentialService.createBasicCredential("testuser", "testpass")).thenReturn(testCredential);
        when(credentialService.validateCredential(any(BasicCredential.class))).thenReturn(true);
        when(credentialService.encryptCredential(any(BasicCredential.class))).thenReturn("encrypted_credential");

        mockMvc.perform(post("/api/credentials/basic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("encrypted_credential"));
    }

    @Test
    void testCreateApiKeyCredential() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("apiKey", "test-api-key");
        params.put("headerName", "X-Custom-Header");

        ApiKeyCredential apiKeyCredential = new ApiKeyCredential("test-api-key", "X-Custom-Header");
        when(credentialService.createApiKeyCredential("test-api-key", "X-Custom-Header")).thenReturn(apiKeyCredential);
        when(credentialService.validateCredential(any(ApiKeyCredential.class))).thenReturn(true);
        when(credentialService.encryptCredential(any(ApiKeyCredential.class))).thenReturn("encrypted_api_key");

        mockMvc.perform(post("/api/credentials/apikey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("encrypted_api_key"));
    }

    @Test
    void testCreateOAuthCredential() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("clientId", "client-id");
        params.put("clientSecret", "client-secret");
        params.put("accessToken", "access-token");
        params.put("refreshToken", "refresh-token");
        params.put("tokenUrl", "https://token.url");

        OAuthCredential oauthCredential = new OAuthCredential("client-id", "client-secret", "access-token", "refresh-token", "https://token.url");
        when(credentialService.createOAuthCredential("client-id", "client-secret", "access-token", "refresh-token", "https://token.url"))
                .thenReturn(oauthCredential);
        when(credentialService.validateCredential(any(OAuthCredential.class))).thenReturn(true);
        when(credentialService.encryptCredential(any(OAuthCredential.class))).thenReturn("encrypted_oauth");

        mockMvc.perform(post("/api/credentials/oauth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("encrypted_oauth"));
    }

    @Test
    void testCreateAwsCredential() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("accessKeyId", "aws-access-key");
        params.put("secretAccessKey", "aws-secret-key");

        AwsCredential awsCredential = new AwsCredential("aws-access-key", "aws-secret-key");
        when(credentialService.createAwsCredential("aws-access-key", "aws-secret-key")).thenReturn(awsCredential);
        when(credentialService.validateCredential(any(AwsCredential.class))).thenReturn(true);
        when(credentialService.encryptCredential(any(AwsCredential.class))).thenReturn("encrypted_aws");

        mockMvc.perform(post("/api/credentials/aws")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(content().string("encrypted_aws"));
    }

    @Test
    void testCreateCredentialWithInvalidData() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("username", ""); // Invalid empty username
        params.put("password", "testpass");

        when(credentialService.createBasicCredential("", "testpass")).thenReturn(new BasicCredential("", "testpass"));
        when(credentialService.validateCredential(any(BasicCredential.class))).thenReturn(false);

        mockMvc.perform(post("/api/credentials/basic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid credential data"));
    }

    @Test
    void testCreateCredentialWithMissingData() throws Exception {
        Map<String, String> params = new HashMap<>();
        // Missing required fields

        mockMvc.perform(post("/api/credentials/basic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateCredentialWithException() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("username", "testuser");
        params.put("password", "testpass");

        when(credentialService.createBasicCredential("testuser", "testpass")).thenReturn(testCredential);
        when(credentialService.validateCredential(any(BasicCredential.class))).thenReturn(true);
        when(credentialService.encryptCredential(any(BasicCredential.class)))
                .thenThrow(new RuntimeException("Encryption failed"));

        mockMvc.perform(post("/api/credentials/basic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to create credential: Encryption failed"));
    }
} 