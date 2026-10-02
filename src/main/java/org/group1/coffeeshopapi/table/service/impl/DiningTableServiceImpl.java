package org.group1.coffeeshopapi.table.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.TableProperties;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.table.dto.request.CreateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.mapper.DiningTableMapper;
import org.group1.coffeeshopapi.table.repository.DiningTableRepository;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiningTableServiceImpl implements DiningTableService {

    private final DiningTableRepository diningTableRepository;
    private final OrderRepository orderRepository;
    private final DiningTableMapper diningTableMapper;
    private final TableProperties tableProperties;

    @Override
    @Transactional
    public TableResponse create(CreateTableRequest request) {
        String tableNumber = normalize(request.tableNumber());
        if (diningTableRepository.existsByTableNumberIgnoreCase(tableNumber)) {
            throw new DuplicateResourceException("Table " + tableNumber + " already exists");
        }
        DiningTable table = new DiningTable();
        table.setTableNumber(tableNumber);
        table.setSize(request.size());
        table.setCapacity(request.capacity() != null ? request.capacity() : request.size().defaultCapacity());
        return toResponse(diningTableRepository.save(table));
    }

    @Override
    public TableResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public Page<TableResponse> list(TableStatus status, Pageable pageable) {
        Page<DiningTable> tables = status == null
                ? diningTableRepository.findAllByOrderByTableNumberAsc(pageable)
                : diningTableRepository.findByStatusOrderByTableNumberAsc(status, pageable);
        return tables.map(this::toResponse);
    }

    @Override
    @Transactional
    public TableResponse update(UUID id, UpdateTableRequest request) {
        DiningTable table = findById(id);
        if (StringUtils.hasText(request.tableNumber())) {
            String tableNumber = normalize(request.tableNumber());
            if (diningTableRepository.existsByTableNumberIgnoreCaseAndIdNot(tableNumber, id)) {
                throw new DuplicateResourceException("Table " + tableNumber + " already exists");
            }
            table.setTableNumber(tableNumber);
        }
        if (request.size() != null) {
            table.setSize(request.size());
            if (request.capacity() == null) {
                table.setCapacity(request.size().defaultCapacity());
            }
        }
        if (request.capacity() != null) {
            table.setCapacity(request.capacity());
        }
        if (table.getGuestCount() > table.getCapacity()) {
            throw new InvalidOperationException("Table " + table.getTableNumber() + " currently has "
                    + table.getGuestCount() + " guests — capacity can't be lower than that");
        }
        return toResponse(diningTableRepository.save(table));
    }

    @Override
    @Transactional
    public TableResponse updateStatus(UUID id, UpdateTableStatusRequest request) {
        DiningTable table = findById(id);
        int guestCount = request.guestCount() != null ? request.guestCount() : 0;
        if (request.status() == TableStatus.AVAILABLE) {
            guestCount = 0;
        } else if (request.status() == TableStatus.OCCUPIED && guestCount < 1) {
            throw new InvalidOperationException("An occupied table needs at least 1 guest");
        }
        if (guestCount > table.getCapacity()) {
            throw new InvalidOperationException("Table " + table.getTableNumber() + " seats at most "
                    + table.getCapacity() + " guests");
        }
        table.setStatus(request.status());
        table.setGuestCount(guestCount);
        return toResponse(diningTableRepository.save(table));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DiningTable table = findById(id);
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new InvalidOperationException("Table " + table.getTableNumber()
                    + " is occupied — set it to available before deleting");
        }
        orderRepository.detachDiningTable(id);
        diningTableRepository.delete(table);
    }

    @Override
    public TableResponse getByNumber(String tableNumber) {
        return toResponse(requireByNumber(tableNumber));
    }

    @Override
    public List<TableResponse> listPublic(TableStatus status) {
        List<DiningTable> tables = status == null
                ? diningTableRepository.findAllByOrderByTableNumberAsc()
                : diningTableRepository.findByStatusOrderByTableNumberAsc(status);
        return tables.stream().map(this::toResponse).toList();
    }

    @Override
    public DiningTable requireByNumber(String tableNumber) {
        String normalized = requireTableNumber(tableNumber);
        return diningTableRepository.findByTableNumberIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Table " + normalized + " not found"));
    }

    @Override
    @Transactional
    public DiningTable seatForOrder(String tableNumber) {
        String normalized = requireTableNumber(tableNumber);
        DiningTable table = diningTableRepository.findByTableNumberForUpdate(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Table " + normalized + " not found"));
        if (table.getStatus() != TableStatus.OCCUPIED) {
            table.setStatus(TableStatus.OCCUPIED);
            table.setGuestCount(Math.max(table.getGuestCount(), 1));
            table = diningTableRepository.save(table);
        }
        return table;
    }

    private String requireTableNumber(String tableNumber) {
        if (!StringUtils.hasText(tableNumber)) {
            throw new InvalidOperationException("Please enter your table number");
        }
        return normalize(tableNumber);
    }

    private String normalize(String tableNumber) {
        return tableNumber.strip().toUpperCase(Locale.ROOT);
    }

    private TableResponse toResponse(DiningTable table) {
        return diningTableMapper.toResponse(table, scanUrl(table));
    }

    private String scanUrl(DiningTable table) {
        String base = tableProperties.getScanBaseUrl();
        return (base.endsWith("/") ? base : base + "/") + table.getTableNumber();
    }

    private DiningTable findById(UUID id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
    }
}
