package com.Sprint.Sprint.DTO.Response;

import com.Sprint.Sprint.Entity.Quest;
import com.Sprint.Sprint.Enums.QuestRarity;
import com.Sprint.Sprint.Enums.QuestStatus;

public record QuestResponseDTO(
        Long id,
        String title,
        String description,
        QuestRarity rarity,
        Integer goldReward,
        QuestStatus status,
        String ownerName,
        String reviewerName,
        String reviewerFeedback
) {
    public QuestResponseDTO(Quest quest) {
        this(
                quest.getId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getRarity(),
                quest.getGoldReward(),
                quest.getStatus(),
                quest.getAdventurer().getNickname() != null ? quest.getAdventurer().getNickname() : "Unknown",
                quest.getReviewer() != null ? quest.getReviewer().getNickname() : null,
                quest.getReviewerFeedback()
        );
    }
}