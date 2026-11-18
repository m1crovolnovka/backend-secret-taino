package com.taino.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String>, ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        EncryptedStringConverter.context = applicationContext;
    }

    private FieldCrypto crypto() {
        return context != null ? context.getBean(FieldCrypto.class) : null;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        FieldCrypto crypto = crypto();
        if (crypto == null || attribute == null) {
            return attribute;
        }
        return crypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        FieldCrypto crypto = crypto();
        if (crypto == null || dbData == null) {
            return dbData;
        }
        return crypto.decrypt(dbData);
    }
}
