package com.polatholding.procurementsystem.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public class CustomUserDetails extends User {

    private final Integer userId;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Integer userId) {
        super(username, password, authorities);
        this.userId = userId;
    }

    public Integer getUserId() {
        return userId;
    }
}