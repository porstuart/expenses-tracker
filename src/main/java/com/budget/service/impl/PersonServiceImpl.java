package com.budget.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.budget.dao.PersonDao;
import com.budget.model.Person;
import com.budget.service.PersonService;
import com.budget.utilities.StringUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PersonServiceImpl implements PersonService {

    @Autowired
    private PersonDao personDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerUser(String username, String password) {
        if (StringUtil.checkEmptyString(username)) {
            log.error("Registration failed: Username cannot be empty");
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (StringUtil.checkEmptyString(password)) {
            log.error("Registration failed: Password cannot be empty");
            throw new IllegalArgumentException("Password cannot be empty");
        }

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

}
