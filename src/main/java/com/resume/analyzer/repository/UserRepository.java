package com.resume.analyzer.repository;

import com.resume.analyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    User findByPhone(String phone);

    User findByEmail(String email);

    java.util.List<User> findByRole(String role);
}