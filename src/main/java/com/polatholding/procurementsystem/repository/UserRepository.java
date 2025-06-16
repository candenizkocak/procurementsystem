package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.polatholding.procurementsystem.model.Role;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    List<User> findByRolesContaining(Role role); // New method
}