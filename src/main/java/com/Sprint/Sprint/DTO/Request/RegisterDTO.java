package com.Sprint.Sprint.DTO.Request;

import com.Sprint.Sprint.Entity.UserRole;

public record RegisterDTO(String username, String nickname, String password, UserRole role) {
}
