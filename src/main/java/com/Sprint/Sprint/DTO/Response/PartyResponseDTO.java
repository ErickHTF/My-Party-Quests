package com.Sprint.Sprint.DTO.Response;

import com.Sprint.Sprint.Entity.Party;

public record PartyResponseDTO(Long id, String name, String description, String ownerName) {
    public PartyResponseDTO(Party party) {
        this(party.getId(), party.getPartyName(), party.getPartyDescription(), party.getOwner().getNickname());
    }
}