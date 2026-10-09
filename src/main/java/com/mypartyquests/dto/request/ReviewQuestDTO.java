package com.mypartyquests.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReviewQuestDTO(
        @NotNull boolean approved,
        String feedback)
{
}
