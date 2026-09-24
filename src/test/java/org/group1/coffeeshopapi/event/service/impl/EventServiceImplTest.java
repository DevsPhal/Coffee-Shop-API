package org.group1.coffeeshopapi.event.service.impl;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.event.dto.request.CreateEventRequest;
import org.group1.coffeeshopapi.event.dto.request.UpdateEventRequest;
import org.group1.coffeeshopapi.event.entity.Event;
import org.group1.coffeeshopapi.event.mapper.EventMapper;
import org.group1.coffeeshopapi.event.repository.EventRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventMapper eventMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private TelegramEventService telegramEventService;
    @InjectMocks private EventServiceImpl service;

    private final LocalDateTime startAt = LocalDateTime.now().plusDays(1);
    private final LocalDateTime endAt = LocalDateTime.now().plusDays(1).plusHours(2);
    private final BigDecimal latitude = new BigDecimal("11.556800");
    private final BigDecimal longitude = new BigDecimal("104.928200");

    @Test
    void createRejectsAVenueWithOnlyLatitudeGiven() {
        var request = new CreateEventRequest("Latte Art Night", "Come watch the pros", latitude, null, startAt, endAt);

        assertThatThrownBy(() -> service.create(request, null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("latitude and longitude must be given together");
        verifyNoInteractions(eventRepository, telegramEventService);
    }

    @Test
    void createRejectsAVenueWithOnlyLongitudeGiven() {
        var request = new CreateEventRequest("Latte Art Night", "Come watch the pros", null, longitude, startAt, endAt);

        assertThatThrownBy(() -> service.create(request, null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("latitude and longitude must be given together");
        verifyNoInteractions(eventRepository, telegramEventService);
    }

    @Test
    void createRejectsAnEndDateThatIsNotAfterTheStartDate() {
        var request = new CreateEventRequest("Latte Art Night", "Come watch the pros", null, null, endAt, startAt);

        assertThatThrownBy(() -> service.create(request, null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("end date must be after the start date");
        verifyNoInteractions(eventRepository, telegramEventService);
    }

    @Test
    void createPersistsTheVenuePinAndAnnouncesTheNewEventOverTelegram() {
        var request = new CreateEventRequest("Latte Art Night", "Come watch the pros", latitude, longitude, startAt, endAt);
        Admin actor = new Admin();
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request, actor);

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        Event event = saved.getValue();
        assertThat(event.getTitle()).isEqualTo("Latte Art Night");
        assertThat(event.getLatitude()).isEqualTo(latitude);
        assertThat(event.getLongitude()).isEqualTo(longitude);
        assertThat(event.getCreatedByAdmin()).isSameAs(actor);
        verify(telegramEventService).announceNewEvent(event);
    }

    @Test
    void createLeavesTheVenuePinUnsetWhenNeitherCoordinateIsGiven() {
        var request = new CreateEventRequest("Latte Art Night", "Come watch the pros", null, null, startAt, endAt);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request, null);

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getLatitude()).isNull();
        assertThat(saved.getValue().getLongitude()).isNull();
    }

    @Test
    void updateRejectsAVenueWithOnlyOneCoordinateGiven() {
        Event existing = existingEvent();
        when(eventRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        var request = new UpdateEventRequest(null, null, latitude, null, null, null, null);

        assertThatThrownBy(() -> service.update(existing.getId(), request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("latitude and longitude must be given together");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateSetsBothCoordinatesWhenBothAreGiven() {
        Event existing = existingEvent();
        when(eventRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);
        var request = new UpdateEventRequest(null, null, latitude, longitude, null, null, null);

        service.update(existing.getId(), request);

        assertThat(existing.getLatitude()).isEqualTo(latitude);
        assertThat(existing.getLongitude()).isEqualTo(longitude);
    }

    @Test
    void updateLeavesAnExistingVenuePinUnchangedWhenNeitherCoordinateIsGiven() {
        Event existing = existingEvent();
        existing.setLatitude(latitude);
        existing.setLongitude(longitude);
        when(eventRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);
        var request = new UpdateEventRequest("Updated title", null, null, null, null, null, null);

        service.update(existing.getId(), request);

        assertThat(existing.getTitle()).isEqualTo("Updated title");
        assertThat(existing.getLatitude()).isEqualTo(latitude);
        assertThat(existing.getLongitude()).isEqualTo(longitude);
    }

    @Test
    void deletingAnEventAlsoRemovesItsImage() {
        Event existing = existingEvent();
        existing.setImageUrl("https://cdn.example/events/latte.png");
        when(eventRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.delete(existing.getId());

        verify(eventRepository).delete(existing);
        verify(fileStorageService).delete("https://cdn.example/events/latte.png");
    }

    private Event existingEvent() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Latte Art Night");
        event.setStartAt(startAt);
        event.setEndAt(endAt);
        return event;
    }
}
