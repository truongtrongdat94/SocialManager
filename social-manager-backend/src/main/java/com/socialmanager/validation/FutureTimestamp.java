package com.socialmanager.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureTimestampValidator.class)
@Documented
public @interface FutureTimestamp {
    String message() default "Thời gian lên lịch phải trong khoảng 10 phút đến 75 ngày trong tương lai";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum seconds from now (default: 600 = 10 minutes)
     */
    long min() default 600;
    
    /**
     * Maximum seconds from now (default: 6480000 = 75 days)
     */
    long max() default 6480000;
}
