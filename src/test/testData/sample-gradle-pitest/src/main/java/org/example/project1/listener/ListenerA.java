package org.example.project1.listener;

import lombok.RequiredArgsConstructor;
import org.example.project1.service.ServiceA;

@RequiredArgsConstructor
public class ListenerA
{
    private final ServiceA serviceA;

    public int sum(int a, int b) {
        return a + b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }

    public void invokeServiceMethod() {
        serviceA.publicMethod();
    }
}
