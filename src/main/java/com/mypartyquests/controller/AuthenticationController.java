package com.mypartyquests.controller;

import com.mypartyquests.dto.request.AuthenticationDTO;
import com.mypartyquests.dto.response.LoginResponseDTO;
import com.mypartyquests.dto.request.RegisterDTO;
import com.mypartyquests.entity.User;
import com.mypartyquests.security.TokenService;
import com.mypartyquests.repository.UserRepository;
import com.mypartyquests.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
public class AuthenticationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Operation(summary = "Create User",
            description = "Creates a new user.")
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody @Valid RegisterDTO data) {
        if(this.userRepository.findByUsername(data.username())!= null) return ResponseEntity.badRequest().build();

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User newUser = new User(data.username(), data.nickname(), encryptedPassword, data.role());

        User userSaved = userService.createUser(newUser);

        return ResponseEntity.status(201).body(userSaved);
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.username(), data.password());
        var authentication = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) authentication.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }


}
