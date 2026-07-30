package com.specpulse.auth.security;

public final class RbacPermissions {
    private RbacPermissions() {
    }

    // Permission names stored in DB.
    public static final String API_DOCS_ACCESS = "API_DOCS_ACCESS";
    public static final String API_TEST_EXECUTE = "API_TEST_EXECUTE";

    // Permission authorities exposed to Spring Security.
    public static final String API_DOCS_ACCESS_AUTHORITY = authorityFor(API_DOCS_ACCESS);
    public static final String API_TEST_EXECUTE_AUTHORITY = authorityFor(API_TEST_EXECUTE);

    public static String authorityFor(String permissionName) {
        return "PERM_" + permissionName;
    }
}
