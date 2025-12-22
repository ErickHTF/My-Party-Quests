package com.Sprint.Sprint.Repository;

import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.Quest;
import com.Sprint.Sprint.Enums.QuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestRepository extends JpaRepository<Quest, Long> {

    List<Quest> findByPartyId(Long partyId);

    List<Quest> findByParty(Party party);

    List<Quest> findByPartyIdAndAdventurerId(Long partyId, Long adventurerId);

    List<Quest> findByPartyIdAndReviewerIdAndStatus(Long partyId, Long reviewerId, QuestStatus status);

    List<Quest> findByPartyIdAndStatus(Long partyId, QuestStatus status);

    @Modifying
    void deleteByParty(Party party);
}

