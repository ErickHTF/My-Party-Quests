package com.Sprint.Sprint.Controller;

import com.Sprint.Sprint.Entity.RegisterDTO;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Repository.UserRepository;
import com.Sprint.Sprint.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Operation(summary = "Get All Users",
            description = "Returns all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserController.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/all")
    public List<User> getAllUsers() {

        return userService.findAllUsers();
    }

    @Operation(summary = "Delete User",
            description = "Delete a existent user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}
