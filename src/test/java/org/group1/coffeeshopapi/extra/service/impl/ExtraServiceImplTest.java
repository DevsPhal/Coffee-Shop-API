package org.group1.coffeeshopapi.extra.service.impl;

import org.group1.coffeeshopapi.extra.dto.request.CreateExtraRequest;
import org.group1.coffeeshopapi.extra.dto.request.UpdateExtraRequest;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.mapper.ExtraMapper;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExtraServiceImplTest {

    @Mock private ExtraRepository extraRepository;
    @Mock private ExtraMapper extraMapper;
    @InjectMocks private ExtraServiceImpl service;

    @Test
    void createLeavesQuantityOnHandUntrackedWhenNotGiven() {
        var request = new CreateExtraRequest("Pearl", new BigDecimal("0.50"), null);
        when(extraRepository.save(any(Extra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<Extra> saved = ArgumentCaptor.forClass(Extra.class);
        verify(extraRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantityOnHand()).isNull();
    }

    @Test
    void createStoresAnExplicitInitialQuantity() {
        var request = new CreateExtraRequest("Pearl", new BigDecimal("0.50"), new BigDecimal("100"));
        when(extraRepository.save(any(Extra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<Extra> saved = ArgumentCaptor.forClass(Extra.class);
        verify(extraRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantityOnHand()).isEqualByComparingTo("100");
    }

    @Test
    void updateLeavesQuantityOnHandUnchangedWhenNotGiven() {
        Extra extra = new Extra();
        extra.setId(UUID.randomUUID());
        extra.setName("Pearl");
        extra.setQuantityOnHand(new BigDecimal("20"));
        when(extraRepository.findById(extra.getId())).thenReturn(Optional.of(extra));
        when(extraRepository.save(extra)).thenReturn(extra);

        service.update(extra.getId(), new UpdateExtraRequest(null, null, null, null));

        assertThat(extra.getQuantityOnHand()).isEqualByComparingTo("20");
    }

    @Test
    void updateRestocksByOverwritingQuantityOnHand() {
        Extra extra = new Extra();
        extra.setId(UUID.randomUUID());
        extra.setName("Pearl");
        extra.setQuantityOnHand(BigDecimal.ZERO);
        when(extraRepository.findById(extra.getId())).thenReturn(Optional.of(extra));
        when(extraRepository.save(extra)).thenReturn(extra);

        service.update(extra.getId(), new UpdateExtraRequest(null, null, null, new BigDecimal("50")));

        assertThat(extra.getQuantityOnHand()).isEqualByComparingTo("50");
    }
}
