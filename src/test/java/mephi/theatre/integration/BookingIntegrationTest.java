package mephi.theatre.integration;

import mephi.theatre.dto.BookRequest;
import mephi.theatre.entity.Seat;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import mephi.theatre.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingIntegrationTest {

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
    private TestRestTemplate restTemplate;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Long testSeatId;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        seatRepository.deleteAll();

        Seat seat = new Seat();
        seat.setRowNum(1);
        seat.setSeatNum(1);
        seat.setStatus(SeatStatus.FREE);
        seat = seatRepository.save(seat);
        testSeatId = seat.getId();
    }

    @Test
    void holdSeat_ShouldReturnSuccess() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/seats/" + testSeatId + "/hold",
                null,
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("Место забронировано на 5 минут. Введите данные для подтверждения.",
                response.getBody().get("message"));
    }

    @Test
    void confirmBooking_ShouldCreateTicket() {
        // Сначала Hold
        restTemplate.postForEntity("/api/seats/" + testSeatId + "/hold", null, Map.class);

        // Затем Confirm
        BookRequest request = new BookRequest();
        request.setCustomerName("Тест Тестов");
        request.setCustomerPhone("89220000000");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/seats/" + testSeatId + "/confirm",
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertNotNull(response.getBody().get("ticketId"));
    }

    @Test
    void getSeats_ShouldReturnSeatsList() {
        ResponseEntity<Seat[]> response = restTemplate.getForEntity(
                "/api/seats",
                Seat[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }
}