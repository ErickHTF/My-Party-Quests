package com.Sprint.Sprint.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Vamos criar o Enum aqui ou em arquivo separado
enum PartyStatus {
    OPEN,
    CLOSED,
    IN_GAME
}

@Entity
@Table(name = "tb_parties")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String partyName;

    @Column(nullable = false)
    private String partyDescription;

    @OneToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    private PartyStatus partyStatus = PartyStatus.OPEN; // Standard open

    private LocalDateTime RunEndDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public String getPartyDescription() {
        return partyDescription;
    }

    public void setPartyDescription(String partyDescription) {
        this.partyDescription = partyDescription;
    }

    public PartyStatus getPartyStatus() {
        return partyStatus;
    }

    public void setPartyStatus(PartyStatus partyStatus) {
        this.partyStatus = partyStatus;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDateTime getRunEndDate() {
        return RunEndDate;
    }

    public void setRunEndDate(LocalDateTime runEndDate) {
        RunEndDate = runEndDate;
    }
}
