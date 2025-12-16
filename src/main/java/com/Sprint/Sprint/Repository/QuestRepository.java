package com.Sprint.Sprint.Repository;

import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestRepository extends JpaRepository<Quest, Long> {

    List<Quest> findByPartyId(Long partyId);

    List<Quest> findByParty(Party party);
}

