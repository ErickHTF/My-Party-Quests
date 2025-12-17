package com.Sprint.Sprint.DTO.Request;

import com.Sprint.Sprint.Enums.UserRole;

public record RegisterDTO(
        String username,
        String nickname,
        String password,
        UserRole role)
{
}
