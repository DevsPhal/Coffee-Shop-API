package org.group1.coffeeshopapi.table.service.impl;

import org.group1.coffeeshopapi.common.enums.TableSize;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.TableProperties;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.table.dto.request.CreateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.mapper.DiningTableMapper;
import org.group1.coffeeshopapi.table.repository.DiningTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiningTableServiceImplTest {

    @Mock private DiningTableRepository diningTableRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private DiningTableMapper diningTableMapper;
    @Spy private TableProperties tableProperties = new TableProperties();
    @InjectMocks private DiningTableServiceImpl service;

    @Test
    void creatingATableNormalizesTheNumberAndDefaultsCapacityFromSize() {
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateTableRequest(" a1 ", TableSize.LARGE, null));

        ArgumentCaptor<DiningTable> captor = ArgumentCaptor.forClass(DiningTable.class);
        verify(diningTableRepository).save(captor.capture());
        assertThat(captor.getValue().getTableNumber()).isEqualTo("A1");
        assertThat(captor.getValue().getCapacity()).isEqualTo(6);
        verify(diningTableMapper).toResponse(captor.getValue(), "https://590stcafe.shop/table/A1");
    }

    @Test
    void creatingADuplicateTableNumberIsRejected() {
        when(diningTableRepository.existsByTableNumberIgnoreCase("5")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateTableRequest("5", TableSize.SMALL, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void occupyingATableRequiresAtLeastOneGuest() {
        DiningTable table = table(TableStatus.AVAILABLE, 4, 0);
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> service.updateStatus(table.getId(),
                new UpdateTableStatusRequest(TableStatus.OCCUPIED, 0)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void guestsCannotExceedCapacity() {
        DiningTable table = table(TableStatus.AVAILABLE, 2, 0);
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> service.updateStatus(table.getId(),
                new UpdateTableStatusRequest(TableStatus.OCCUPIED, 3)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void freeingATableResetsGuestCount() {
        DiningTable table = table(TableStatus.OCCUPIED, 4, 3);
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(diningTableRepository.save(table)).thenReturn(table);

        service.updateStatus(table.getId(), new UpdateTableStatusRequest(TableStatus.AVAILABLE, 2));

        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(table.getGuestCount()).isZero();
    }

    @Test
    void placingADineInOrderMarksAFreeTableOccupied() {
        DiningTable table = table(TableStatus.AVAILABLE, 4, 0);
        when(diningTableRepository.findByTableNumberForUpdate("A1")).thenReturn(Optional.of(table));
        when(diningTableRepository.save(table)).thenReturn(table);

        service.seatForOrder("a1");

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(table.getGuestCount()).isEqualTo(1);
    }

    @Test
    void dineInWithoutATableNumberIsRejected() {
        assertThatThrownBy(() -> service.seatForOrder(" "))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("table number");
    }

    @Test
    void anOccupiedTableCannotBeDeleted() {
        DiningTable table = table(TableStatus.OCCUPIED, 4, 2);
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> service.delete(table.getId())).isInstanceOf(InvalidOperationException.class);
        verify(orderRepository, never()).detachDiningTable(eq(table.getId()));
    }

    private DiningTable table(TableStatus status, int capacity, int guests) {
        DiningTable table = new DiningTable();
        table.setId(UUID.randomUUID());
        table.setTableNumber("A1");
        table.setSize(TableSize.MEDIUM);
        table.setCapacity(capacity);
        table.setGuestCount(guests);
        table.setStatus(status);
        return table;
    }
}
