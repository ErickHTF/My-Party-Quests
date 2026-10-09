package com.mypartyquests.dto.response;

public record UserResponseDTO(
        Long id,
        String username,
        String nickname,
        Integer gold,
        Integer xp,
        Integer level,
        Integer nextLevelXp,
        Integer progressPercentage,
        String partyName
)
{
}
