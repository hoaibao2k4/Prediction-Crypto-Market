package com.market.prediction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.market.prediction.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsernameOrEmail(String username, String email);
}
