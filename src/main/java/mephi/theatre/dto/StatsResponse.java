package mephi.theatre.dto;

public class StatsResponse {
    private long totalSeats;
    private long bookedSeats;
    private long freeSeats;
    private double occupancyPercentage;

    public long getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(long totalSeats) {
        this.totalSeats = totalSeats;
    }

    public long getBookedSeats() {
        return bookedSeats;
    }

    public void setBookedSeats(long bookedSeats) {
        this.bookedSeats = bookedSeats;
    }

    public long getFreeSeats() {
        return freeSeats;
    }

    public void setFreeSeats(long freeSeats) {
        this.freeSeats = freeSeats;
    }

    public double getOccupancyPercentage() {
        return occupancyPercentage;
    }

    public void setOccupancyPercentage(double occupancyPercentage) {
        this.occupancyPercentage = occupancyPercentage;
    }
}