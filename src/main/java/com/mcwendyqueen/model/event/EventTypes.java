package com.mcwendyqueen.model.event;

import lombok.Getter;

@Getter
public enum EventTypes {
    ORDER("order");

    private final String shortName;

    EventTypes(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }
}
