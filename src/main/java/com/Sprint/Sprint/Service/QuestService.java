package com.Sprint.Sprint.Service;

import com.Sprint.Sprint.DTO.Request.CreateQuestDTO;
import com.Sprint.Sprint.Entity.Quest;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Repository.QuestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestService {

    @Autowired
    private QuestRepository questRepository;

    public Quest createQuest(CreateQuestDTO data, User adventurer) {
        if (adventurer.getCurrentParty() == null) {
            throw new RuntimeException("Party need, adventurer");
        }

        //montando objeto
        Quest quest = new Quest();
        quest.setTitle(data.title());
        quest.setDescription(data.description());
        quest.setRarity(data.rarity());

        //vinculando objeto com current party
        quest.setAdventurer(adventurer);
        quest.setParty(adventurer.getCurrentParty());

        int goldAmount = switch (data.rarity()) {
            case COMMON -> 100;
            case RARE -> 300;
            case EPIC -> 600;
            case LEGENDARY -> 1000;
        };
        quest.setGoldReward(goldAmount);

        return questRepository.save(quest);
    }

}
