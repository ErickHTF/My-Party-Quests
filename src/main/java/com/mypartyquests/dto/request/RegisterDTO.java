package com.mypartyquests.dto.request;

import com.mypartyquests.enums.UserRole;

public record RegisterDTO(
        String username,
        String nickname,
        String password,
        UserRole role)
{
}
