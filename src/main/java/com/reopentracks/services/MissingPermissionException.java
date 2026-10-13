package com.reopentracks.services;

import com.reopentracks.util.PermissionRequester;

public class MissingPermissionException extends RuntimeException {

    private final PermissionRequester permissionRequester;

    public MissingPermissionException(PermissionRequester permissionRequester) {
        this.permissionRequester = permissionRequester;
    }
}
