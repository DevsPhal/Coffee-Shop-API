package org.group1.coffeeshopapi.realtime;

import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.realtime.dto.ResourceChangeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Real JPA + transactions: entity changes reach the broker once, and only after commit.
@SpringBootTest
@ActiveProfiles("test")
class ResourceChangePublisherIntegrationTest {

    @Autowired private ExtraRepository extraRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @MockitoSpyBean private SimpMessagingTemplate messagingTemplate;

    @AfterEach
    void cleanUp() {
        extraRepository.deleteAll();
    }

    @Test
    void aCommittedChangeIsPushedOnceAfterCommit() {
        clearInvocations(messagingTemplate);

        UUID id = transactionTemplate.execute(status -> {
            Extra extra = extraRepository.save(newExtra());
            // Nothing goes out while the transaction is still open.
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
            extra.setPrice(new BigDecimal("0.75"));
            return extra.getId();
        });

        // Created then updated in one transaction collapses into a single CREATED message.
        verify(messagingTemplate, times(1)).convertAndSend(eq(RealtimeDestinations.CATALOG),
                argThat((Object message) -> message instanceof ResourceChangeMessage change
                        && change.resource() == ResourceType.EXTRA
                        && change.id().equals(id)
                        && change.change() == ChangeType.CREATED));
    }

    @Test
    void aRolledBackChangeIsNeverPushed() {
        clearInvocations(messagingTemplate);

        transactionTemplate.executeWithoutResult(status -> {
            extraRepository.saveAndFlush(newExtra());
            status.setRollbackOnly();
        });

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        assertThat(extraRepository.count()).isZero();
    }

    private Extra newExtra() {
        Extra extra = new Extra();
        extra.setName("Pearl " + UUID.randomUUID());
        extra.setPrice(new BigDecimal("0.50"));
        return extra;
    }
}
