package com.devfocus.auth.repository;

import com.devfocus.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByCognitoSub(String cognitoSub);

    Optional<User> findByGithubId(Long githubId);

    boolean existsByGithubId(Long githubId);
}
