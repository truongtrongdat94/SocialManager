package com.socialmanager.config;

import com.socialmanager.model.Platform;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PlatformConverter implements Converter<String, Platform> {
    @Override
    public Platform convert(String source) {
        if (source == null) return null;
        try {
            return Platform.valueOf(source.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported platform: " + source);
        }
    }
}
