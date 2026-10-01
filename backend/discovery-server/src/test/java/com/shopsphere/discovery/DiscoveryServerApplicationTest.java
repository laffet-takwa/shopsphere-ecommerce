package com.shopsphere.discovery;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

class DiscoveryServerApplicationTest {
    @Test
    void enablesEurekaServer() {
        assertTrue(DiscoveryServerApplication.class.isAnnotationPresent(EnableEurekaServer.class));
    }
}