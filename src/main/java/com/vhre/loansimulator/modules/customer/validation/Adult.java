package com.vhre.loansimulator.modules.customer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AdultValidator.class)
public @interface Adult {

    int value() default 18;

    String message() default "El solicitante debe tener al menos 18 años.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
