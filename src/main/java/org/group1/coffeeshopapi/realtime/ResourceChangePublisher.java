package org.group1.coffeeshopapi.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.realtime.dto.ResourceChangeMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Collects changes for the current transaction and sends them once, after commit. A bulk
// import touching the same product many times still sends one message for it.
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceChangePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void record(ResourceType resource, UUID id, ChangeType change) {
        if (id == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send(resource, id, change);
            return;
        }
        pendingChanges().merge(new Key(resource, id), change, ChangeType::merge);
    }

    @SuppressWarnings("unchecked")
    private Map<Key, ChangeType> pendingChanges() {
        Object existing = TransactionSynchronizationManager.getResource(this);
        if (existing != null) {
            return (Map<Key, ChangeType>) existing;
        }
        Map<Key, ChangeType> changes = new LinkedHashMap<>();
        TransactionSynchronizationManager.bindResource(this, changes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                changes.forEach((key, change) -> send(key.resource(), key.id(), change));
            }

            @Override
            public void afterCompletion(int status) {
                TransactionSynchronizationManager.unbindResourceIfPossible(ResourceChangePublisher.this);
            }
        });
        return changes;
    }

    private void send(ResourceType resource, UUID id, ChangeType change) {
        try {
            messagingTemplate.convertAndSend(resource.destination(),
                    new ResourceChangeMessage(resource, id, change, LocalDateTime.now()));
        } catch (RuntimeException ex) {
            // The data is already saved — a failed push must not turn into an error response.
            log.warn("Failed to push {} change for {}", resource, id, ex);
        }
    }

    private record Key(ResourceType resource, UUID id) {}
}
