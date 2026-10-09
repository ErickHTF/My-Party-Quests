package com.mypartyquests.dto.request;

import com.mypartyquests.enums.QuestRarity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestDTO(

        @NotBlank(message = "Title is mandatory")
        String title,

        String description,

        @NotNull(message = "Difficulty is mandatory.")
        QuestRarity rarity)
{
}