package com.Sprint.Sprint.Entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity(name ="tb_weekly_scores ")
public class WeeklyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private LocalDate weekEndDate;

    @ManyToOne
    @JoinColumn(name = "adventurer_id")
    private User adventurer;

    @ManyToOne
    @JoinColumn(name = "party_id")
    private Party party;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
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
}
