package com.supplygraph.application.security;

import com.supplygraph.domain.model.security.UserProfile;

/**
 * Thread-local security context holding the active user profile for the current invocation.
 */
public final class SecurityContext {

    private static final ThreadLocal<UserProfile> CURRENT_USER = new ThreadLocal<>();

    private SecurityContext() {
    }

    public static void setCurrentUser(UserProfile profile) {
        CURRENT_USER.set(profile);
    }

    public static UserProfile getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }

    /**
     * Executes the given task under the specified user context, clearing context upon completion.
     */
    public static void runAs(UserProfile profile, Runnable task) {
        UserProfile previous = CURRENT_USER.get();
        try {
            CURRENT_USER.set(profile);
            task.run();
        } finally {
            if (previous != null) {
                CURRENT_USER.set(previous);
            } else {
                CURRENT_USER.remove();
            }
        }
    }
}

