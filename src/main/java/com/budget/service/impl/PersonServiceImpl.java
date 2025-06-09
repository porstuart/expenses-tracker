package com.budget.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.budget.dao.PersonDao;
import com.budget.exception.ApiException;
import com.budget.model.Person;
import com.budget.service.PersonService;
import com.budget.utilities.StringUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PersonServiceImpl implements PersonService {

    private static int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_PASSWORD_LENGTH = 128;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerUser(String username, String password) {
        validateRegistrationInput(username, password);
        checkUsernameAvailability(username);

        if (personDao.existsByUsername(username)) {
            log.error("Registration failed: Username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        Person person = new Person();
        person.setUsername(username);
        person.setPassword(passwordEncoder.encode(password));
        person.setRoles("USER");
        personDao.save(person);
    }

    private void validateRegistrationInput(String username, String password) {
        validateUsername(username);
        validatePassword(password);
        createAndSaveUser(username, password);
    }

    private void validateUsername(String username) {
        if (StringUtil.checkEmptyString(username)) {
            log.error("Registration failed: Username cannot be empty");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }

        String trimmedUsername = username.trim();
        if (trimmedUsername.length() < MIN_USERNAME_LENGTH) {
            log.error("Registration failed: Username must be at least {} characters long", MIN_USERNAME_LENGTH);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username must be at least " + MIN_USERNAME_LENGTH + " characters long");
        }

        if (trimmedUsername.length() > MAX_USERNAME_LENGTH) {
            log.error("Registration failed: Username cannot be more than {} characters long", MAX_USERNAME_LENGTH);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username cannot be more than " + MAX_USERNAME_LENGTH + " characters long");
        }

        if (!trimmedUsername.matches("^[a-zA-Z0-9._-]+$")) {
            log.error("Registration failed: Username can only contain letters, numbers, dots, underscores, and hyphens");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username can only contain letters, numbers, dots, underscores, and hyphens");
        }
    }

    private void validatePassword(String password) {
        if (StringUtil.checkEmptyString(password)) {
            log.error("Registration failed: Password cannot be empty");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.error("Registration failed: Password must be at least {} characters long", MIN_PASSWORD_LENGTH);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            log.error("Registration failed: Password cannot be more than {} characters long", MAX_PASSWORD_LENGTH);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password cannot be more than " + MAX_PASSWORD_LENGTH + " characters long");
        }

        if (!isPasswordStrong(password)) {
            log.error("Registration failed: Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }

    }

    /**
     * Checks if the given password is strong.
     * A strong password must contain at least one lowercase letter, one uppercase letter,
     * one digit, and one special character from the set @$!%*?&.
     *
     * @param password the password to check
     * @return true if the password is strong, false otherwise
     */
    private boolean isPasswordStrong(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$");
    }

    private void checkUsernameAvailability(String username) {
        String normalizedUsername = username.trim().toLowerCase();

        if (personDao.existsByUsername(normalizedUsername)) {
            log.error("Registration failed: Username already exists: {}", username);
            throw new ApiException(HttpStatus.CONFLICT, "A user with a similar username already exists");
        }
    }

    private void createAndSaveUser(String username, String password) {
        try {
            Person person = new Person();
            person.setUsername(username);
            person.setPassword(passwordEncoder.encode(password));
            person.setRoles("USER");
            personDao.save(person);
        } catch (DataAccessException e) {
            log.error("Database error while saving user {}, Reason: {}", username, e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create user account due to database error", e);
        }
    }

}
