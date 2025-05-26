package com.budget.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.budget.model.Person;

public interface PersonDao extends JpaRepository<Person, Long> {

    boolean existsByUsername(String username);
    Person findByUsername(String username);
}
