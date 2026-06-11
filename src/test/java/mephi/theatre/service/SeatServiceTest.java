package mephi.theatre.service;

import mephi.theatre.dto.SeatResponse;
import mephi.theatre.entity.Seat;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatService seatService;

    private List<Seat> seats;

    @BeforeEach
    void setUp() {
        Seat seat1 = new Seat();
        seat1.setId(1L);
        seat1.setRowNum(1);
        seat1.setSeatNum(1);
        seat1.setStatus(SeatStatus.FREE);

        Seat seat2 = new Seat();
        seat2.setId(2L);
        seat2.setRowNum(1);
        seat2.setSeatNum(2);
        seat2.setStatus(SeatStatus.BOOKED);

        seats = Arrays.asList(seat1, seat2);
    }

    @Test
    void getAllSeats_ShouldReturnSortedSeats() {
        when(seatRepository.findAllByOrderByRowNumAscSeatNumAsc()).thenReturn(seats);

        List<SeatResponse> result = seatService.getAllSeats();

        assertEquals(2, result.size());
        assertEquals(SeatStatus.FREE, result.get(0).getStatus());
        assertEquals(SeatStatus.BOOKED, result.get(1).getStatus());
        verify(seatRepository).findAllByOrderByRowNumAscSeatNumAsc();
    }

    @Test
    void getAllSeats_WhenEmpty_ShouldReturnEmptyList() {
        when(seatRepository.findAllByOrderByRowNumAscSeatNumAsc()).thenReturn(List.of());

        List<SeatResponse> result = seatService.getAllSeats();

        assertTrue(result.isEmpty());
        verify(seatRepository).findAllByOrderByRowNumAscSeatNumAsc();
    }

}