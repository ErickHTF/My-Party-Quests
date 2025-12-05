package com.Sprint.Sprint.Service;

import com.Sprint.Sprint.DTO.Request.CreatePartyDTO;
import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Repository.PartyRepository;
import com.Sprint.Sprint.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

}
