package com.mypartyquests.dto.response;

import com.mypartyquests.entity.Quest;
import com.mypartyquests.enums.QuestRarity;
import com.mypartyquests.enums.QuestStatus;

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