package com.Sprint.Sprint.Service;

import com.Sprint.Sprint.Exception.ResourceNotFoundException;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository UserRepository;

    public UserService(UserRepository UserRepository) {this.UserRepository = UserRepository;}

    public List<User> findAllUsers() {
        return UserRepository.findAll();
    }

    public User createUser(User user) {
        return UserRepository.save(user);
    }

    public void  deleteUser(Long id) {
        User user =  UserRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found on:" + id));

        UserRepository.delete(user);
    }
}
