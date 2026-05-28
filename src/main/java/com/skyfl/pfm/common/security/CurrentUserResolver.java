package com.skyfl.pfm.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserResolver {

    private CurrentUserResolver() {
    }

    public static CurrentUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (CurrentUser) authentication.getPrincipal();
    }
}
