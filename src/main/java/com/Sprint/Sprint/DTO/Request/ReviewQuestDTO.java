package com.Sprint.Sprint.DTO.Request;

import jakarta.validation.constraints.NotNull;

public record ReviewQuestDTO(
        @NotNull boolean approved,
        String feedback)
{
}
