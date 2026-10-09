package com.mypartyquests.service;

import com.mypartyquests.dto.response.UserResponseDTO;
import com.mypartyquests.exception.ResourceNotFoundException;
import com.mypartyquests.entity.User;
import com.mypartyquests.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LevelProgressionService levelService;

    @Autowired
    public UserService(UserRepository userRepository, LevelProgressionService levelService) {
        this.userRepository = userRepository;
        this.levelService = levelService;
    }

    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return convertToDTO(user);
    }

    private UserResponseDTO convertToDTO(User user) {
        int currentLevel = levelService.calculateLevel(user.getXp());
        int nextLevelThreshold = levelService.calculateNextLevelThreshold(currentLevel);
        int progressPercentage = levelService.calculateProgressPercentage(user.getXp());

        String partyName = (user.getCurrentParty() != null)
                ? user.getCurrentParty().getPartyName()
                : "Lone Wolf";

        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getGold(),
                user.getXp(),
                currentLevel,
                nextLevelThreshold,
                progressPercentage,
                partyName
        );
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public void  deleteUser(Long id) {
        User user =  userRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found on:" + id));

        userRepository.delete(user);
    }
}
