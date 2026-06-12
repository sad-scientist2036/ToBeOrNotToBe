package mephi.theatre.service;

import mephi.theatre.dto.BookRequest;
import mephi.theatre.entity.Seat;
import mephi.theatre.entity.Ticket;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import mephi.theatre.repository.TicketRepository;
import mephi.theatre.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseEmitters sseEmitters;

    @Mock
    private SeatService seatService;

    @InjectMocks
    private BookingService bookingService;

    private Seat seat;
    private BookRequest request;

    @BeforeEach
    void setUp() {
        seat = new Seat();
        seat.setId(1L);
        seat.setRowNum(5);
        seat.setSeatNum(10);
        seat.setStatus(SeatStatus.FREE);

        request = new BookRequest();
        request.setSeatId(1L);
        request.setCustomerName("Иван Петров");
        request.setCustomerPhone("89225457909");
    }

    @Test
    void holdSeat_ShouldChangeStatusToHold() {
        when(seatRepository.findByIdWithLock(1L)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenReturn(seat);

        Seat result = bookingService.holdSeat(1L);

        assertEquals(SeatStatus.HOLD, result.getStatus());
        verify(seatRepository).save(seat);
        verify(sseEmitters).sendSeatsUpdate(any());
    }

    @Test
    void holdSeat_WhenSeatAlreadyBooked_ShouldThrowException() {
        seat.setStatus(SeatStatus.BOOKED);
        when(seatRepository.findByIdWithLock(1L)).thenReturn(Optional.of(seat));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.holdSeat(1L);
        });

        assertEquals("Место уже не доступно", exception.getMessage());
        verify(seatRepository, never()).save(any());
    }

    @Test
    void confirmBooking_ShouldCreateTicket() {
        seat.setStatus(SeatStatus.HOLD);

        when(seatRepository.findByIdWithLock(1L)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenReturn(seat);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        Ticket result = bookingService.confirmBooking(request, "user@test.com");

        assertNotNull(result);
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        verify(ticketRepository).save(any(Ticket.class));
        verify(sseEmitters).sendSeatsUpdate(any());
    }
}