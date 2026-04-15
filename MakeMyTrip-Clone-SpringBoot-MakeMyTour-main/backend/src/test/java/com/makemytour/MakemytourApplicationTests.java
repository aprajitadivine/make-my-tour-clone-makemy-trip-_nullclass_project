package com.makemytour;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the Spring application context loads without errors.
 */
@SpringBootTest
@ActiveProfiles("test")
class MakemytourApplicationTests {

    @Test
    void contextLoads() {
        // The context load itself is the assertion – if the app wires correctly, this passes
    }
}
