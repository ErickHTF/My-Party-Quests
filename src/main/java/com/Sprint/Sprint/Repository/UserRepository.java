package com.Sprint.Sprint.Repository;

import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    User findByUsername(String username);

    List<User> findByCurrentParty(Party party);

}
