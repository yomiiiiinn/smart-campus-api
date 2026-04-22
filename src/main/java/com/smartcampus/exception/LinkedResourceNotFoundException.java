package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String referencedId;

    public LinkedResourceNotFoundException(String resourceType, String referencedId) {
        super("Referenced " + resourceType + " '" + referencedId + "' does not exist.");
        this.resourceType = resourceType;
        this.referencedId = referencedId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getReferencedId() {
        return referencedId;
    }
}
