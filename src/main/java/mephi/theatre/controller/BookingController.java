package mephi.theatre.controller;

import mephi.theatre.dto.BookRequest;
import mephi.theatre.dto.SeatResponse;
import mephi.theatre.dto.StatsResponse;
import mephi.theatre.entity.Seat;
import mephi.theatre.entity.Ticket;
import mephi.theatre.service.BookingService;
import mephi.theatre.service.SeatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
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

    @PostMapping("/seats/{seatId}/hold")
    public ResponseEntity<Map<String, Object>> holdSeat(@PathVariable Long seatId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Seat seat = bookingService.holdSeat(seatId);
            response.put("success", true);
            response.put("message", "Место забронировано на 5 минут. Введите данные для подтверждения.");
            response.put("expiresAt", seat.getHoldExpiresAt());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    @PostMapping("/seats/{seatId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmBooking(
            @PathVariable Long seatId,
            @RequestBody BookRequest request,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();

        try {
            request.setSeatId(seatId);
            String userEmail = principal != null ? principal.getName() : null;
            Ticket ticket = bookingService.confirmBooking(request, userEmail);

            response.put("success", true);
            response.put("message", "Билет успешно оформлен!");
            response.put("ticketId", ticket.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    @PostMapping("/seats/{seatId}/cancel-hold")
    public ResponseEntity<Map<String, Object>> cancelHold(@PathVariable Long seatId) {
        Map<String, Object> response = new HashMap<>();

        try {
            bookingService.cancelHold(seatId);
            response.put("success", true);
            response.put("message", "Бронирование отменено");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/seats/{seatId}/release")
    public ResponseEntity<Map<String, Object>> releaseSeat(@PathVariable Long seatId, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        try {
            String userEmail = principal != null ? principal.getName() : null;
            bookingService.releaseSeat(seatId, userEmail);
            response.put("success", true);
            response.put("message", "Бронирование отменено, место освобождено");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
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

    @GetMapping("/my/bookings")
    public ResponseEntity<List<Map<String, Object>>> getMyBookings(Principal principal) {
        List<Map<String, Object>> bookings = new ArrayList<>();

        if (principal == null) {
            return ResponseEntity.ok(bookings);
        }

        try {
            List<Ticket> tickets = bookingService.getMyBookings(principal.getName());
            for (Ticket ticket : tickets) {
                Map<String, Object> booking = new HashMap<>();
                booking.put("seatId", ticket.getSeat().getId());
                booking.put("row", ticket.getSeat().getRowNum());
                booking.put("number", ticket.getSeat().getSeatNum());
                booking.put("bookedAt", ticket.getBookedAt());
                booking.put("customerName", ticket.getCustomerName());
                booking.put("customerPhone", ticket.getCustomerPhone());
                bookings.add(booking);
            }
        } catch (Exception e) {
            // ignore
        }

        return ResponseEntity.ok(bookings);
    }
}