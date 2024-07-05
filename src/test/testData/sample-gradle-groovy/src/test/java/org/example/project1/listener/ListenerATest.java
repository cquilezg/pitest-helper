package org.example.project1.listener;

import org.example.project1.service.ServiceA;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ListenerATest
{
    @Mock
    private ServiceA serviceA;

    @InjectMocks
    private ListenerA listenerA;

    @Test
    void testSum1() {
        assertEquals(2, listenerA.sum(1, 1));
    }

    @Test
    void testSum2() {
        assertEquals(5, listenerA.sum(2, 3));
    }

    @Test
    void testServiceMethod() {
        listenerA.invokeServiceMethod();
        verify(serviceA).publicMethod();
    }
}
