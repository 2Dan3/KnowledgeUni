package com.ftn.research.knowledgeuniverse.repository.entity;

import com.ftn.research.knowledgeuniverse.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    @Query(value =
            "SELECT u " +
            "FROM user u " +
            "WHERE u.email = :email AND u.password = :password;")
    Optional<User> findByCredentials(@Param("email") String email, @Param("password") String password);
}
