package com.reviewservice.helper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MinIOHelperTest {
    @Test
    void testConstructor() {
        MinIOHelper helper = new MinIOHelper("http://localhost:9000", "accessKey", "secretKey", "test-bucket");
        assertNotNull(helper);
    }
} 