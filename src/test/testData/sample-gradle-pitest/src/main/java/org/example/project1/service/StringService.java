package org.example.project1.service;

public class StringService
{
    public String removeFromString(String source, String remove) {
        return source.replace(remove, "");
    }
}
