package mephi.theatre.service;

import mephi.theatre.dto.BookRequest;
import mephi.theatre.entity.Seat;
import mephi.theatre.entity.Ticket;
import mephi.theatre.entity.User;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import mephi.theatre.repository.TicketRepository;
import mephi.theatre.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SseEmitters sseEmitters;
    private final SeatService seatService;

    public BookingService(SeatRepository seatRepository,
                          TicketRepository ticketRepository,
                          UserRepository userRepository,
                          SseEmitters sseEmitters,
                          SeatService seatService) {
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.sseEmitters = sseEmitters;
        this.seatService = seatService;
    }

    @Transactional
    public Seat holdSeat(Long seatId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new RuntimeException("Место не найдено"));

        if (seat.getStatus() != SeatStatus.FREE) {
            throw new RuntimeException("Место уже не доступно");
        }

        seat.setStatus(SeatStatus.HOLD);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        return seatRepository.save(seat);
    }

    @Transactional
    public Ticket confirmBooking(BookRequest request, String userEmail) {
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new RuntimeException("Имя обязательно для заполнения");
        }

        String phone = request.getCustomerPhone();
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("Телефон обязателен для заполнения");
        }

        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() < 10) {
            throw new RuntimeException("Номер телефона должен содержать минимум 10 цифр");
        }
        request.setCustomerPhone(cleanPhone);

        Seat seat = seatRepository.findByIdWithLock(request.getSeatId())
                .orElseThrow(() -> new RuntimeException("Место не найдено"));

        if (seat.getStatus() != SeatStatus.HOLD) {
            throw new RuntimeException("Место не забронировано. Возможно, время вышло.");
        }

        if (seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            seat.setStatus(SeatStatus.FREE);
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
            throw new RuntimeException("Время бронирования истекло");
        }

        seat.setStatus(SeatStatus.BOOKED);
        seat.setHoldExpiresAt(null);
        seatRepository.save(seat);

        Ticket ticket = new Ticket();
        ticket.setSeat(seat);
        ticket.setCustomerName(request.getCustomerName().trim());
        ticket.setCustomerPhone(request.getCustomerPhone());
        ticket.setBookedAt(LocalDateTime.now());

        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                ticket.setUser(user);
            }
        }

        try {
            Ticket saved = ticketRepository.save(ticket);
            sseEmitters.sendSeatsUpdate(seatService.getAllSeats());
            return saved;
        } catch (DataIntegrityViolationException e) {
            seat.setStatus(SeatStatus.FREE);
            seatRepository.save(seat);
            throw new RuntimeException("Место уже занято");
        }
    }

    @Transactional
    public void cancelHold(Long seatId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new RuntimeException("Место не найдено"));

        if (seat.getStatus() == SeatStatus.HOLD) {
            seat.setStatus(SeatStatus.FREE);
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
        }
    }

    @Transactional
    public void releaseSeat(Long seatId, String userEmail) {
        Ticket ticket = ticketRepository.findBySeatId(seatId)
                .orElseThrow(() -> new RuntimeException("Бронирование не найдено"));

        if (userEmail != null && ticket.getUser() != null && !ticket.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Вы не можете отменить чужое бронирование");
        }

        Seat seat = ticket.getSeat();
        seat.setStatus(SeatStatus.FREE);
        seat.setHoldExpiresAt(null);
        seatRepository.save(seat);

        ticketRepository.delete(ticket);

        sseEmitters.sendSeatsUpdate(seatService.getAllSeats());
    }

    @Transactional(readOnly = true)
    public List<Ticket> getMyBookings(String userEmail) {
        if (userEmail == null) {
            return List.of();
        }
        return ticketRepository.findByUserEmail(userEmail);
    }
}