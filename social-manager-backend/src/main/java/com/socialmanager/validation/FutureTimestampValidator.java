package com.socialmanager.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FutureTimestampValidator implements ConstraintValidator<FutureTimestamp, Long> {
    
    private long min;
    private long max;
    
    @Override
    public void initialize(FutureTimestamp annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }
    
    @Override
    public boolean isValid(Long timestamp, ConstraintValidatorContext context) {
        // Null is valid (optional field)
        if (timestamp == null) {
            return true;
        }
        
        long now = System.currentTimeMillis() / 1000; // Current Unix timestamp
        long diff = timestamp - now;
        
        if (diff < min) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Thời gian lên lịch phải ít nhất " + (min / 60) + " phút trong tương lai"
            ).addConstraintViolation();
            return false;
        }
        
        if (diff > max) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Thời gian lên lịch không được quá " + (max / 86400) + " ngày trong tương lai"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
