package com.devfocus.auth.repository;

import com.devfocus.auth.entity.User;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS devfocus_auth",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    public UserRepository userRepository;


    @Test
    void findUserByGithubId_ReturnsUser(){

        User user = User.builder()
                .githubId(111L)
                .cognitoSub("something-wonderful")
                .githubUsername("ofentse")
                .build();

        userRepository.save(user);

        Optional<User> userFound = userRepository.findByGithubId(111L);

        assertThat(userFound).isPresent();
        assertThat(userFound.get().getGithubId()).isEqualTo(111L);
        assertThat(userFound.get().getCognitoSub()).isEqualTo("something-wonderful");
    }

    @Test
    void findUserByCognito_Sub_ReturnsUser(){

        User user = User.builder()
                .githubId(111L)
                .cognitoSub("something-wonderful")
                .githubUsername("ofentse")
                .build();

        userRepository.save(user);

        Optional<User> userFound = userRepository.findByCognitoSub("something-wonderful");

        assertThat(userFound).isPresent();
        assertThat(userFound.get().getGithubId()).isEqualTo(111L);
        assertThat(userFound.get().getCognitoSub()).isEqualTo("something-wonderful");
    }

    @Test
    void findUserByCognito_Sub_ReturnsUserNotFound(){
        Optional<User> userFound = userRepository.findByCognitoSub("something-wonderful");

        assertThat(userFound).isNotPresent();
    }
}
