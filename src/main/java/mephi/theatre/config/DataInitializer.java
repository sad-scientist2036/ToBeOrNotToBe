package mephi.theatre.config;

import mephi.theatre.entity.Hall;
import mephi.theatre.entity.Seat;
import mephi.theatre.enums.SeatStatus;
import mephi.theatre.repository.HallRepository;
import mephi.theatre.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SeatRepository seatRepository;
    private final HallRepository hallRepository;

    public DataInitializer(SeatRepository seatRepository, HallRepository hallRepository) {
        this.seatRepository = seatRepository;
        this.hallRepository = hallRepository;
    }

    @Override
    public void run(String... args) {
        if (hallRepository.count() == 0) {
            System.out.println("Создание зала...");

            // Создаем зал
            Hall hall = new Hall();
            hall.setName("Главный зал");
            hall.setRows(10);
            hall.setSeatsPerRow(20);
            hall = hallRepository.save(hall);

            // Создаем места
            for (int row = 1; row <= hall.getRows(); row++) {
                for (int seatNum = 1; seatNum <= hall.getSeatsPerRow(); seatNum++) {
                    Seat seat = new Seat();
                    seat.setRowNum(row);
                    seat.setSeatNum(seatNum);
                    seat.setStatus(SeatStatus.FREE);
                    seat.setHall(hall);
                    seatRepository.save(seat);
                }
            }

            System.out.println(" Зал создан: " + hall.getName() +
                    " (" + hall.getRows() + " рядов × " + hall.getSeatsPerRow() + " мест)");
            System.out.println(" Всего создано мест: " + seatRepository.count());
        } else {
            System.out.println("Зал уже существует. Количество мест: " + seatRepository.count());
        }
    }
}