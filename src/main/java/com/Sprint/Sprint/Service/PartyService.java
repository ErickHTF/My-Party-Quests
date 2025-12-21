package com.Sprint.Sprint.Service;

import com.Sprint.Sprint.DTO.Request.CreatePartyDTO;
import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.Quest;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Enums.PartyStatus;
import com.Sprint.Sprint.Enums.QuestStatus;
import com.Sprint.Sprint.Repository.PartyRepository;
import com.Sprint.Sprint.Repository.QuestRepository;
import com.Sprint.Sprint.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class PartyService {

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestRepository questRepository;

    public Party createParty(CreatePartyDTO data, User loggedUser) {
        Party party = new Party();

        // Validates previous party binding: blocks owner from abandoning the group without disbanding it and removes common members from the old party
        if (loggedUser.getCurrentParty() != null) {
            if (loggedUser.getCurrentParty().getOwner().getId().equals(loggedUser.getId())) {
                throw new RuntimeException("You are the OWNER of an active party. Disband it before creating a new one.");
            }
            loggedUser.getCurrentParty().getMembers().remove(loggedUser);
            partyRepository.save(loggedUser.getCurrentParty());
        }

        // Set data BEFORE saving to avoid null errors or saving empty objects
        boolean isPrivate = data.isPrivate() != null ? data.isPrivate() : false;
        int maxMembers = data.maxMembers() != null ? data.maxMembers() : 10;

        // Populating the party object
        party.setPartyName(data.partyName());
        party.setPartyDescription(data.partyDescription());
        party.setPrivate(isPrivate);
        party.setMaxMembers(maxMembers);
        party.setOwner(loggedUser);


        // Try to save the populated object
        try {
            partyRepository.save(party);

            loggedUser.setCurrentParty(party);
            userRepository.save(loggedUser);

            return party;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A party with the name '" + party.getPartyName() + "' already exists.");
        }
    }

    public Party joinParty(Long partyId, User user) {

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        // Validates previous affiliation: prevents OWNER from leaving the group without disbanding it and removes common members from the old party.
        if (user.getCurrentParty() != null) {
            if (user.getCurrentParty().getOwner().getId().equals(user.getId())) {
                throw new RuntimeException("You are the OWNER of your current party. You must Disband it before joining another.");
            }
            user.getCurrentParty().getMembers().remove(user);
            partyRepository.save(user.getCurrentParty());
        }

        if (party.getMembers().contains(user)) throw new RuntimeException("Already a member");
        if (party.getPendingMembers().contains(user)) throw new RuntimeException("Request already sent");

        if (party.getMembers().size() >= party.getMaxMembers()) {
            throw new RuntimeException("Party is full! Max: " + party.getMaxMembers());
        }

        // Public/Private logic
        if (party.isPrivate()) {
            party.getPendingMembers().add(user);
            partyRepository.save(party);
        } else {
            user.setCurrentParty(party);
            party.getMembers().add(user);
            userRepository.save(user);
            partyRepository.save(party);
        }

        return party;
    }

    public void approveRequest(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        // Validates the candidate's current state: blocks approval if the user is leading another party (to prevent orphan groups) and automatically removes them from their previous party if they are just a member
        if (user.getCurrentParty() != null) {
            if (user.getCurrentParty().getOwner().getId().equals(user.getId())) {
                throw new RuntimeException("Cannot approve: The user is currently leading another party.");
            }
            user.getCurrentParty().getMembers().remove(user);
            partyRepository.save(user.getCurrentParty());
        }

        if (party.getPendingMembers().contains(user)) {
            if (party.getMembers().size() >= party.getMaxMembers()) {
                throw new RuntimeException("Cannot approve: Party is full.");
            }

            party.getPendingMembers().remove(user);
            party.getMembers().add(user);
            user.setCurrentParty(party);

            userRepository.save(user);
            partyRepository.save(party);
        }
    }

    public void rejectRequest(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        party.getPendingMembers().remove(user);
        partyRepository.save(party);
    }

    public void leaveParty(User loggedUser) {

        Party currentParty = loggedUser.getCurrentParty();
        if (currentParty == null) {
            throw new RuntimeException("You're not assigned to any party.");
        }

        if (currentParty.getOwner().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("The owner cannot leave their own party. Delete the party or transfer its leadership.");
        }

        if (currentParty.getPartyStatus() != PartyStatus.LOBBY) {
            throw new IllegalStateException("Please wait for the return to the LOBBY phase. (Current Phase: " + currentParty.getPartyStatus() + ")");
        }

        loggedUser.setCurrentParty(null);

        userRepository.save(loggedUser);

    }

    @Transactional
    public void deleteParty(Long id, User loggedUser) {

        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (!party.getOwner().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("Only the owner can delete this party.");
        }

        List<User> members = userRepository.findAll()
                .stream()
                .filter(u ->
                        //ignore users with no party
                        u.getCurrentParty() != null &&
                                //get this party
                                u.getCurrentParty().getId().equals(id)
                )
                .toList();

        List<Quest> questsToDelete = questRepository.findByParty(party);
        questRepository.deleteAll(questsToDelete);

        members.forEach(u -> u.setCurrentParty(null));
        userRepository.saveAll(members);

        partyRepository.delete(party);

    }

    public List<Party> findAllParties() {
        return partyRepository.findAll();
    }

    @Transactional
    public Party startPlanningPhase(Long partyId, User loggedUser) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (!party.getOwner().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("Only the owner can start the sprint.");
        }

        if (party.getPartyStatus() != PartyStatus.LOBBY && party.getPartyStatus() != PartyStatus.REVIEW) {
            throw new RuntimeException("Cannot start planning phase. Current status: " + party.getPartyStatus());
        }

        party.setPartyStatus(PartyStatus.PLANNING);

        party.setCurrentPhaseExpiration(LocalDateTime.now().plusDays(2));

        return partyRepository.save(party);
    }

    @Transactional
    public Party startExecutionPhase(Long partyId, User loggedUser) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (!party.getOwner().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("Only the owner can start the execution phase.");
        }

        if (party.getPartyStatus() != PartyStatus.PLANNING) {
            throw new RuntimeException("Cannot start execution. Current status is " + party.getPartyStatus());
        }

        List<Quest> partyQuests = questRepository.findByParty(party);

        if (partyQuests.isEmpty()) {
            throw new RuntimeException("Cannot start the adventure without quests.");
        }

        boolean hasPendingIssues = partyQuests.stream()
                .anyMatch(q -> q.getStatus() != QuestStatus.APPROVED);

        if (hasPendingIssues) {
            throw new RuntimeException("Cannot start the adventure! All quests must be APPROVED first.");
        }

        party.setPartyStatus(PartyStatus.EXECUTION);
        party.setCurrentPhaseExpiration(LocalDateTime.now().plusDays(5));

        return partyRepository.save(party);
    }

    @Transactional
    public Party startReviewPhase(Long partyId, User loggedUser) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (!party.getOwner().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("Only the owner can finish the execution!");
        }

        party.setPartyStatus(PartyStatus.REVIEW);

        List<Quest> partyQuests = questRepository.findByPartyId(partyId);

        //Iterates through quests to process approved payouts and increment the adventurer's completed quests counter
        for (Quest quest : partyQuests) {
            if (quest.getStatus() == QuestStatus.COMPLETED && !quest.isRewardClaimed()) {

                User owner = quest.getAdventurer();

                int currentGold = owner.getGold() != null ? owner.getGold() : 0;
                int rewardGold = quest.getGoldReward() != null ? quest.getGoldReward() : 0;
                owner.setGold(currentGold + rewardGold);

                int currentXp = owner.getXp() != null ? owner.getXp() : 0;
                int rewardXp = quest.getXpReward() != null ? quest.getXpReward() : 0;
                owner.setXp(currentXp + rewardXp);

                int currentQuests = owner.getQuestsCompleted() != null ? owner.getQuestsCompleted() : 0;
                owner.setQuestsCompleted(owner.getQuestsCompleted() + 1);

                quest.setRewardClaimed(true);

                userRepository.save(owner);
                questRepository.save(quest);
            }
        }
        return partyRepository.save(party);
    }

    @Transactional
    public Party resetToLobby(Long partyId, User loggedUser) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (!party.getOwner().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("Only the owner can reset the party!");
        }

        party.setPartyStatus(PartyStatus.LOBBY);
        questRepository.deleteByParty(party);

        return partyRepository.save(party);
    }

    @Transactional
    public Party kickMember(Long partyId, Long userIdToKick, User requester) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        // Segurança: Só o dono pode expulsar
        if (!party.getOwner().getId().equals(requester.getId())) {
            throw new RuntimeException("Only the owner can kick members!");
        }

        User memberToKick = userRepository.findById(userIdToKick)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Remove da lista
        party.getMembers().remove(memberToKick);
        memberToKick.setCurrentParty(null); // Desvincula no lado do User (JPA)

        userRepository.save(memberToKick);
        return partyRepository.save(party);
    }



}
