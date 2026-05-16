package mephi.theatre.dto;


import java.util.List;

public class HallSchemaResponse {
    private String hallName;
    private int rows;
    private int seatsPerRow;
    private List<SeatResponse> seats;
}