package com.Sprint.Sprint.Entity;

import com.Sprint.Sprint.Enums.PartyStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_parties")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String partyName;

    @Column(nullable = false)
    private String partyDescription;

    @OneToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    private PartyStatus partyStatus = PartyStatus.LOBBY;

    @OneToMany(mappedBy = "currentParty", fetch = FetchType.EAGER)
    private java.util.List<User> members;

    @Column(columnDefinition = "boolean default false")
    private boolean isPrivate = false;

    @ManyToMany
    @JoinTable(
            name = "party_pending_members",
            joinColumns = @JoinColumn(name = "party_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )

    private List<User> pendingMembers = new ArrayList<>();

    private Integer maxMembers = 8;

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

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public List<User> getPendingMembers() {
        return pendingMembers;
    }

    public void setPendingMembers(List<User> pendingMembers) {
        this.pendingMembers = pendingMembers;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }
}
