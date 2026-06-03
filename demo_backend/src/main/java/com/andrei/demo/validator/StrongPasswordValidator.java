package com.andrei.demo.validator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;

        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()".indexOf(ch) >= 0);

        return password.length() >= 8 && hasDigit && hasLower && hasUpper && hasSpecial;
    }
}

