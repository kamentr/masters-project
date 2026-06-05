package com.plovdiv.advisor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    default void saveDemoUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setDisplayName("Demo User");
        save(user);
    }
}
