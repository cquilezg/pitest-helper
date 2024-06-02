package org.example.project1.service;

import org.example.project1.service.ServiceB;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ServiceBTest
{
    @InjectMocks
    private ServiceB serviceB;

    @Test
    void testServiceB() {
        assertTrue("HelloWorld!".equals(serviceB.concatStrings("Hello", "World!")));
    }
}
