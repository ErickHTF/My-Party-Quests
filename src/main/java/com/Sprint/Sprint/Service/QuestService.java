package com.Sprint.Sprint.Service;

import com.Sprint.Sprint.DTO.Request.CreateQuestDTO;
import com.Sprint.Sprint.DTO.Request.ReviewQuestDTO;
import com.Sprint.Sprint.DTO.Response.QuestResponseDTO;
import com.Sprint.Sprint.Enums.QuestStatus;
import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.Quest;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Enums.PartyStatus;
import com.Sprint.Sprint.Repository.QuestRepository;
import com.Sprint.Sprint.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class QuestService {

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private UserRepository userRepository;

    public Quest createQuest(CreateQuestDTO data, User adventurer) {

        Party currentParty = adventurer.getCurrentParty();

        if (adventurer.getCurrentParty() == null) {
            throw new RuntimeException("Party needed, adventurer");
        }

        if (currentParty == null || currentParty.getPartyStatus() != PartyStatus.PLANNING) {
            throw new RuntimeException("You can only create quests during the PLANNING phase.");
        }

        //mounting object
        Quest quest = new Quest();
        quest.setTitle(data.title());
        quest.setDescription(data.description());
        quest.setRarity(data.rarity());

        //linking object (quest) with current party
        quest.setAdventurer(adventurer);
        quest.setParty(adventurer.getCurrentParty());

        int goldAmount = switch (data.rarity()) {
            case COMMON -> 5;
            case RARE -> 10;
            case EPIC -> 15;
            case LEGENDARY -> 20;
        };
        quest.setGoldReward(goldAmount);

        int xpAmount = switch (data.rarity()) {
            case COMMON -> 50;
            case RARE -> 150;
            case EPIC -> 500;
            case LEGENDARY -> 1500;
        };
        quest.setXpReward(xpAmount);

        List<User> members = userRepository.findByCurrentParty(currentParty);
        Random random = new Random();

        List<User> potentialReviewers = members.stream()
                .filter(m -> !m.getId().equals(adventurer.getId()))
                .toList();

        if (!potentialReviewers.isEmpty()) {
            User selectedReviewer = potentialReviewers.get(random.nextInt(potentialReviewers.size()));
            quest.setReviewer(selectedReviewer);
        } else {
            quest.setReviewer(adventurer);
        }

        quest.setStatus(QuestStatus.PENDING_APPROVAL);

        return questRepository.save(quest);
    }

    public Quest reviewQuest(Long questId, ReviewQuestDTO data, User loggedUser) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Quest not found."));

        if (quest.getReviewer() == null || !quest.getReviewer().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("You are not the assigned reviewer for this quest.");
        }

        if (quest.getStatus() != QuestStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Quest not pending approval.");
        }

        if (data.approved()) {
            quest.setStatus(QuestStatus.APPROVED);
        } else {
            quest.setStatus(QuestStatus.REJECTED);
        }

        quest.setReviewerFeedback(data.feedback());

        return questRepository.save(quest);
    }

    public Quest completeQuest(Long questId, User loggedUser) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Quest not found."));

        if (!quest.getAdventurer().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("You are not the owner of this quest.");
        }

        if (quest.getStatus() != QuestStatus.APPROVED) {
            throw new RuntimeException("You can only complete approved quests. Current status: " + quest.getStatus());
        }

        quest.setStatus(QuestStatus.COMPLETED);

        return questRepository.save(quest);
    }

    public List<Quest> findAllQuests() {
        return questRepository.findAll();
    }

    public List<QuestResponseDTO> getMyQuests(User user) {
        if (user.getCurrentParty() == null) {
            return List.of();
        }

        List<Quest> quests = questRepository.findByPartyIdAndAdventurerId(
                user.getCurrentParty().getId(),
                user.getId()
        );

        return quests.stream()
                .map(QuestResponseDTO::new)
                .toList();
    }

    public List<QuestResponseDTO> getQuestsToReview(User user) {
        if (user.getCurrentParty() == null) return List.of();

        List<Quest> quests = questRepository.findByPartyIdAndReviewerIdAndStatus(
                user.getCurrentParty().getId(),
                user.getId(),
                QuestStatus.PENDING_APPROVAL
        );

        return quests.stream().map(QuestResponseDTO::new).toList();
    }

    @Transactional
    public Quest updateQuest(Long questId, CreateQuestDTO data, User loggedUser) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Quest not found."));

        if (!quest.getAdventurer().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("You can only edit your own quests.");
        }

        if (quest.getParty().getPartyStatus() != PartyStatus.PLANNING) {
            throw new RuntimeException("Quests can only be edited during PLANNING phase.");
        }

        // Updates
        quest.setTitle(data.title());
        quest.setDescription(data.description());
        quest.setRarity(data.rarity());

        // Set status to pending
        quest.setStatus(QuestStatus.PENDING_APPROVAL);

        return questRepository.save(quest);
    }

}
