package org.group1.coffeeshopapi.realtime;

public enum ChangeType {
    CREATED, UPDATED, DELETED;

    // Several changes to the same row in one transaction collapse into one: a delete wins,
    // and a create followed by updates is still a create.
    public ChangeType merge(ChangeType next) {
        if (this == DELETED || next == DELETED) {
            return DELETED;
        }
        return this == CREATED ? CREATED : next;
    }
}
