package com.Sprint.Sprint.Entity;

import com.Sprint.Sprint.Enums.PartyStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

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
    private PartyStatus partyStatus = PartyStatus.LOBBY; // Standard open

    @OneToMany(mappedBy = "currentParty", fetch = FetchType.EAGER)
    private java.util.List<User> members;

    private LocalDateTime currentPhaseExpiration;

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

    public LocalDateTime getCurrentPhaseExpiration() {
        return currentPhaseExpiration;
    }

    public void setCurrentPhaseExpiration(LocalDateTime currentPhaseExpiration) {
        this.currentPhaseExpiration = currentPhaseExpiration;
    }

    public java.util.List<User> getMembers() {
        return members;
    }

    public void setMembers(java.util.List<User> members) {
        this.members = members;
    }
}
