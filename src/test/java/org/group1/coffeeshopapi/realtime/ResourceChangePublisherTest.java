package org.group1.coffeeshopapi.realtime;

import org.group1.coffeeshopapi.realtime.dto.ResourceChangeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResourceChangePublisherTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private ResourceChangePublisher publisher;

    @Test
    void stockChangesReachStaffAndTheCustomerMenu() {
        UUID productId = UUID.randomUUID();

        publisher.record(ResourceType.INVENTORY, productId, ChangeType.UPDATED);

        verify(messagingTemplate).convertAndSend(eq(RealtimeDestinations.INVENTORY), argThat((Object message) ->
                message instanceof ResourceChangeMessage change && change.id().equals(productId)));
        verify(messagingTemplate).convertAndSend(eq(RealtimeDestinations.STOCK), argThat((Object message) ->
                message instanceof ResourceChangeMessage change && change.id().equals(productId)));
    }

    @Test
    void oneFailedDestinationDoesNotStopTheOthers() {
        doThrow(new MessagingException("broker down"))
                .when(messagingTemplate).convertAndSend(eq(RealtimeDestinations.INVENTORY), any(Object.class));

        publisher.record(ResourceType.INVENTORY, UUID.randomUUID(), ChangeType.UPDATED);

        verify(messagingTemplate).convertAndSend(eq(RealtimeDestinations.STOCK), any(Object.class));
    }

    @Test
    void bannersAndEventsReachTheCatalog() {
        publisher.record(ResourceType.BANNER, UUID.randomUUID(), ChangeType.UPDATED);
        publisher.record(ResourceType.EVENT, UUID.randomUUID(), ChangeType.CREATED);

        verify(messagingTemplate).convertAndSend(eq(RealtimeDestinations.CATALOG), argThat((Object message) ->
                message instanceof ResourceChangeMessage change && change.resource() == ResourceType.BANNER));
        verify(messagingTemplate).convertAndSend(eq(RealtimeDestinations.CATALOG), argThat((Object message) ->
                message instanceof ResourceChangeMessage change && change.resource() == ResourceType.EVENT));
    }
}
