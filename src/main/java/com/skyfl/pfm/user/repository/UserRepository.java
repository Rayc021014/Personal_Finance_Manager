package com.skyfl.pfm.user.repository;

import com.skyfl.pfm.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("select (count(u) > 0) from User u where u.email = :email")
    boolean existsByEmail(@Param("email") String email);
}
