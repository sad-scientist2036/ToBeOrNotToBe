package mephi.theatre.integration;

import mephi.theatre.entity.Seat;
import mephi.theatre.entity.Ticket;
import mephi.theatre.entity.User;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import mephi.theatre.repository.TicketRepository;
import mephi.theatre.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class RepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void seatRepository_ShouldSaveAndFind() {
        Seat seat = new Seat();
        seat.setRowNum(5);
        seat.setSeatNum(10);
        seat.setStatus(SeatStatus.FREE);

        Seat saved = seatRepository.save(seat);
        Optional<Seat> found = seatRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(5, found.get().getRowNum());
        assertEquals(10, found.get().getSeatNum());
        assertEquals(SeatStatus.FREE, found.get().getStatus());
    }

    @Test
    void seatRepository_ShouldFindAllOrdered() {
        Seat seat1 = new Seat();
        seat1.setRowNum(2);
        seat1.setSeatNum(1);
        seat1.setStatus(SeatStatus.FREE);
        seatRepository.save(seat1);

        Seat seat2 = new Seat();
        seat2.setRowNum(1);
        seat2.setSeatNum(1);
        seat2.setStatus(SeatStatus.FREE);
        seatRepository.save(seat2);

        var seats = seatRepository.findAllByOrderByRowNumAscSeatNumAsc();

        assertEquals(2, seats.size());
        assertEquals(1, seats.get(0).getRowNum());
        assertEquals(2, seats.get(1).getRowNum());
    }

    @Test
    void userRepository_ShouldSaveAndFindByEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setName("Тест");
        user.setRole("USER");

        userRepository.save(user);
        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("Тест", found.get().getName());
    }

    @Test
    void ticketRepository_ShouldHaveUniqueSeatConstraint() {
        Seat seat1 = new Seat();
        seat1.setRowNum(3);
        seat1.setSeatNum(5);
        seat1.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat1);

        Ticket ticket1 = new Ticket();
        ticket1.setSeat(seat1);
        ticket1.setBookedAt(LocalDateTime.now());
        ticketRepository.save(ticket1);

        Ticket ticket2 = new Ticket();
        ticket2.setSeat(seat1);
        ticket2.setBookedAt(LocalDateTime.now());

        assertThrows(Exception.class, () -> ticketRepository.save(ticket2));
    }
}