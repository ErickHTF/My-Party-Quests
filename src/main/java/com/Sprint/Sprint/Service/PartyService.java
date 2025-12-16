package com.Sprint.Sprint.Service;

import com.Sprint.Sprint.DTO.Request.CreatePartyDTO;
import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Repository.PartyRepository;
import com.Sprint.Sprint.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartyService {

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UserRepository userRepository;

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
}
