package mephi.theatre.integration;

import mephi.theatre.dto.BookRequest;
import mephi.theatre.entity.Seat;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.SeatRepository;
import mephi.theatre.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

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
    @WithMockUser(roles = "USER")
    void holdSeat_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/seats/" + testSeatId + "/hold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Место забронировано. Введите данные для подтверждения."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void confirmBooking_ShouldCreateTicket() throws Exception {
        // Сначала Hold
        mockMvc.perform(post("/api/seats/" + testSeatId + "/hold"));

        // Затем Confirm
        BookRequest request = new BookRequest();
        request.setCustomerName("Тест Тестов");
        request.setCustomerPhone("89220000000");

        mockMvc.perform(post("/api/seats/" + testSeatId + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.ticketId").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getSeats_ShouldReturnSeatsList() throws Exception {
        mockMvc.perform(get("/api/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}