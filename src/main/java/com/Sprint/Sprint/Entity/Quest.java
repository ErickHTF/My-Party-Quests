package com.Sprint.Sprint.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_quests")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column (nullable = false)
    private String title;

    @Column (nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private QuestRarity rarity;

    @Column (nullable = false)
    private Integer goldReward;

    @ManyToOne
    @JoinColumn(name = "adventurer_id")
    private User adventurer;

    @ManyToOne
    @JoinColumn(name = "party_id")
    private Party party;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Quest() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuestRarity getRarity() {
        return rarity;
    }

    public void setRarity(QuestRarity rarity) {
        this.rarity = rarity;
    }

    public Integer getGoldReward() {
        return goldReward;
    }

    public void setGoldReward(Integer goldReward) {
        this.goldReward = goldReward;
    }

    public User getAdventurer() {
        return adventurer;
    }

    public void setAdventurer(User adventurer) {
        this.adventurer = adventurer;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
