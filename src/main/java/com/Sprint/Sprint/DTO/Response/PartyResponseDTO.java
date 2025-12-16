package com.Sprint.Sprint.DTO.Response;

import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Enums.PartyStatus;
import java.time.LocalDateTime;

public record PartyResponseDTO(Long id,
                               String name,
                               String description,
                               String ownerName,
                               PartyStatus status,
                               LocalDateTime currentPhaseExpiration
)
{
    public PartyResponseDTO(Party party) {
        this(
                party.getId(),
                party.getPartyName(),
                party.getPartyDescription(),
                party.getOwner().getNickname(),
                party.getPartyStatus(),
                party.getCurrentPhaseExpiration()
        );
    }
}