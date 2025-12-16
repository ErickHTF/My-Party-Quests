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
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

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

        //montando objeto
        party.setPartyName(data.partyName());
        party.setPartyDescription(data.partyDescription());

        party.setOwner(loggedUser);

        partyRepository.save(party);

        loggedUser.setCurrentParty(party);
        userRepository.save(loggedUser);

        return party;
    }

    public Party joinParty(Long partyId, User user) {

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        if (party.equals(user.getCurrentParty())) {
            throw new RuntimeException("You're already part of this party!");
        }

        user.setCurrentParty(party);

        userRepository.save(user);

        return party;
    }

    public void leaveParty(User loggedUser) {

        Party currentParty = loggedUser.getCurrentParty();
        if (currentParty == null) {
            throw new RuntimeException("You're not assigned to any party.");
        }

        if (currentParty.getOwner().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("The owner cannot leave their own party. Delete the party or transfer its leadership.");
        }

        loggedUser.setCurrentParty(null);

        userRepository.save(loggedUser);

    }

    public void deleteParty(Long id, User loggedUser) {

        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (!party.getOwner().getId().equals(loggedUser.getId())) {
            throw new RuntimeException("Only the owner can delete this party.");
        }

        List<User> members = userRepository.findAll()
                //transforma lista
                .stream()
                .filter(u ->
                        //ignore users with no party
                        u.getCurrentParty() != null &&
                                //get this party
                                u.getCurrentParty().getId().equals(id)
                )
                .toList();

        members.forEach(u -> u.setCurrentParty(null));
        userRepository.saveAll(members);

        partyRepository.delete(party);

    }

    @Transactional
    public Party startPlanningPhase(Long partyId, User loggedUser) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (party.getOwner().getId().equals(loggedUser.getId())) {
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
        List<User> members = party.getMembers();

        if (members.size() < 2) {// In case of solo playing
        }

        Random random = new Random();

        for (Quest quest : partyQuests) {
            if (quest.getReviewer() != null) continue;

            // Choose a random member who is NOT the quest owner.
            List<User> potentialReviewers = members.stream()
                    .filter(m -> !m.getId().equals(quest.getAdventurer().getId()))
                    .toList();

            if (!potentialReviewers.isEmpty()) {
                User selectedReviewer = potentialReviewers.get(random.nextInt(potentialReviewers.size()));

                quest.setReviewer(selectedReviewer);
                quest.setStatus(QuestStatus.PENDING_APPROVAL);
            } else { // Fallback / Auto Approve
                quest.setReviewer(quest.getAdventurer());
                quest.setStatus(QuestStatus.APPROVED); //
            }
                questRepository.save(quest);
        }
        party.setPartyStatus(PartyStatus.EXECUTION);
        party.setCurrentPhaseExpiration(LocalDateTime.now().plusDays(5)); // Sprint duration

        return partyRepository.save(party);

    }

}
