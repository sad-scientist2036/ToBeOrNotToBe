package mephi.theatre.controller;

import mephi.theatre.dto.BookRequest;
import mephi.theatre.dto.SeatResponse;
import mephi.theatre.dto.StatsResponse;
import mephi.theatre.entity.Ticket;
import mephi.theatre.service.BookingService;
import mephi.theatre.service.SeatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final SeatService seatService;
    private final BookingService bookingService;

    public BookingController(SeatService seatService, BookingService bookingService) {
        this.seatService = seatService;
        this.bookingService = bookingService;
    }

    @GetMapping("/seats")
    public List<SeatResponse> getAllSeats() {
        return seatService.getAllSeats();
    }

    @PostMapping("/seats/{seatId}/book")
    public ResponseEntity<?> bookSeat(@PathVariable Long seatId, @RequestBody BookRequest request) {
        request.setSeatId(seatId);
        Ticket ticket = bookingService.bookSeat(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Билет успешно оформлен", "ticketId", ticket.getId()));
    }

    @PostMapping("/seats/{seatId}/release")
    public ResponseEntity<?> releaseSeat(@PathVariable Long seatId) {
        bookingService.releaseSeat(seatId);
        return ResponseEntity.ok(Map.of("message", "Бронирование отменено"));
    }

    @GetMapping("/stats")
    public StatsResponse getStats() {
        List<SeatResponse> seats = seatService.getAllSeats();
        long total = seats.size();
        long booked = seats.stream().filter(s -> s.getStatus().name().equals("BOOKED")).count();
        StatsResponse resp = new StatsResponse();
        resp.setTotalSeats(total);
        resp.setBookedSeats(booked);
        resp.setFreeSeats(total - booked);
        resp.setOccupancyPercentage(total == 0 ? 0 : (booked * 100.0 / total));
        return resp;
    }
}