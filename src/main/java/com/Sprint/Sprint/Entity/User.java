package com.Sprint.Sprint.Entity;

import jakarta.persistence.*;

@Table(name = "tb_users")
@Entity
public class User {

    @Id
    @GeneratedValue

    @Column
    private Long id;

    @Column
    private String username;

    @Column
    private String nickname;

    @Column
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
