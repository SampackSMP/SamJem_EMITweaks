package com.carjem.sampackemitweaks.itemgroups.config;

public enum Sort {
    DEFAULT, ALPHABETICALLY;

    private final String key;

    Sort() {
        this.key = "enum.sort." + this.name().toLowerCase();
    }

    public String getKey() {
        return this.key;
    }
}
