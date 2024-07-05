package org.example.project1.listener;

import lombok.RequiredArgsConstructor;
import org.example.project1.service.StringService;

@RequiredArgsConstructor
public class ListenerString
{
    private final StringService stringService;

    public String removeFromString(String source, String remove) {
        return stringService.removeFromString(source, remove);
    }
}
