package com.Sprint.Sprint.Repository;

import com.Sprint.Sprint.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>{
}
