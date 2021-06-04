package org.hzero.route.gateway;

/**
 * @author XCXCXCXCX
 * @date 2020/5/26 10:18 上午
 */
public class UserContext {

    private static final ThreadLocal<UserContext> USER_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    private String username;

    public static UserContext getContext() {
        UserContext context = USER_CONTEXT_THREAD_LOCAL.get();
        return context == null ? initContext() : context;
    }

    private static UserContext initContext() {
        UserContext userContext = new UserContext();
        USER_CONTEXT_THREAD_LOCAL.set(userContext);
        return userContext;
    }

    public static void clearContext() {
        USER_CONTEXT_THREAD_LOCAL.remove();
    }

    public UserContext setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getUsername() {
        return username;
    }
}
