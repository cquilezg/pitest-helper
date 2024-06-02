package org.example.project1.listener;

import org.example.project1.listener.ListenerString;
import org.example.project1.service.StringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListenerStringTest
{
    @Mock
    private StringService stringService;

    @InjectMocks
    private ListenerString listenerString;

    @Test
    void testRemoveFromString() {
        when(stringService.removeFromString("HelloWorld!", "Hello")).thenReturn("World!");
        String result = listenerString.removeFromString("HelloWorld!", "Hello");
        assertEquals("World!", result);
    }
}
