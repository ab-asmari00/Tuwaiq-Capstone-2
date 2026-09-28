package com.example.jura.Repository;

import com.example.jura.Model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    List<User> findByRole(String role);

    User findUserById(Integer id);

    User findUserByEmail(String email);

    boolean existsByEmail(String email);

}
