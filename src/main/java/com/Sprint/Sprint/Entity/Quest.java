package com.Sprint.Sprint.Entity;

import com.Sprint.Sprint.Enums.QuestRarity;
import com.Sprint.Sprint.Enums.QuestStatus;
import jakarta.persistence.*;

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

    @Enumerated(EnumType.STRING)
    private QuestStatus status = QuestStatus.DRAFT; //Standard DRAFT

    @Column(length = 500)
    private String reviewerFeedback;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "adventurer_id")
    private User adventurer;

    @ManyToOne
    @JoinColumn(name = "party_id")

    private Party party;

    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean rewardClaimed = false;

    private Integer xpReward;

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

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }

    public Integer getXpReward() {
        return xpReward;
    }

    public void setXpReward(Integer xpReward) {
        this.xpReward = xpReward;
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

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    public String getReviewerFeedback() {
        return reviewerFeedback;
    }

    public void setReviewerFeedback(String reviewerFeedback) {
        this.reviewerFeedback = reviewerFeedback;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }
}
