package com.Sprint.Sprint.Controller;

import com.Sprint.Sprint.DTO.Request.AuthenticationDTO;
import com.Sprint.Sprint.DTO.Response.LoginResponseDTO;
import com.Sprint.Sprint.DTO.Request.RegisterDTO;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Security.TokenService;
import com.Sprint.Sprint.Repository.UserRepository;
import com.Sprint.Sprint.Service.UserService;
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

    //criptografa e armazena senha recebida por parametro e compara com hash do BD
    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.username(), data.password());
        var authentication = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) authentication.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }


}
