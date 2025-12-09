package com.Sprint.Sprint.DTO.Request;

import com.Sprint.Sprint.Entity.QuestRarity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestDTO(

        @NotBlank(message = "Title is mandatory")
        String title,

        String description,

        @NotNull(message = "Difficulty is mandatory.")
        QuestRarity rarity
) {}