package org.group1.coffeeshopapi.realtime;

public enum ChangeType {
    CREATED, UPDATED, DELETED;

    public ChangeType merge(ChangeType next) {
        if (this == DELETED || next == DELETED) {
            return DELETED;
        }
        return this == CREATED ? CREATED : next;
    }
}
