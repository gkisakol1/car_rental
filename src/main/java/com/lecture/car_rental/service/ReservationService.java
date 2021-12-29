package com.lecture.car_rental.service;

import com.lecture.car_rental.domain.Car;
import com.lecture.car_rental.domain.Reservation;
import com.lecture.car_rental.domain.User;
import com.lecture.car_rental.domain.enumeration.ReservationStatus;
import com.lecture.car_rental.dto.ReservationDTO;
import com.lecture.car_rental.exception.BadRequestException;
import com.lecture.car_rental.exception.ResourceNotFoundException;
import com.lecture.car_rental.repository.CarRepository;
import com.lecture.car_rental.repository.ReservationRepository;
import com.lecture.car_rental.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private static final String USER_NOT_FOUND_MSG = "user with id %d not found";
    private static final String CAR_NOT_FOUND_MSG = "car with id %d not found";
    private static final String RESERVATION_NOT_FOUND_MSG = "reservation with id %d not found";

    public List<ReservationDTO> fetchAllReservations() {
        return reservationRepository.findAllReservation();
    }

    public ReservationDTO findById(Long id) throws ResourceNotFoundException{
        return reservationRepository.findReservationById(id).orElseThrow(() ->
                new ResourceNotFoundException(String.format(RESERVATION_NOT_FOUND_MSG, id)));
    }

    public ReservationDTO findByIdAndUserId(Long id, Long userId) throws ResourceNotFoundException {
        return reservationRepository.findReservationByUserId(id, userId).orElseThrow(() ->
                new ResourceNotFoundException(String.format(RESERVATION_NOT_FOUND_MSG, id)));
    }

    public List<ReservationDTO> findAllByUserId(Long userId) throws ResourceNotFoundException {
        return reservationRepository.findReservationsByUserId(userId);
    }
    public void addReservation(Reservation reservation, Long userId, Car carId) throws BadRequestException {
        boolean checkStatus = carAvailability(carId.getId(), reservation.getPickUpTime(), reservation.getDropOfTime());

        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

        if (!checkStatus)
            reservation.setStatus(ReservationStatus.CREATED);
        else
            throw new BadRequestException("Car is already reserved! Please choose another");

        reservation.setCarId(carId);
        reservation.setUserId(user);

        Double totalPrice = totalPrice(reservation.getPickUpTime(), reservation.getDropOfTime(), carId.getId());
        reservation.setTotalPrice(totalPrice);

        reservationRepository.save(reservation);
    }

    public void updateReservation(Car carId, Long id, Reservation reservation) throws BadRequestException {
        boolean checkStatus = carAvailability(carId.getId(), reservation.getPickUpTime(), reservation.getDropOfTime());

        Optional<Reservation> reservationExist = reservationRepository.findById(id);

        if (reservationExist.isEmpty())
            throw new ResourceNotFoundException("Error: Reservation does not exist!");

        if (reservation.getPickUpTime().compareTo(reservationExist.get().getPickUpTime()) == 0 &&
             reservation.getDropOfTime().compareTo(reservationExist.get().getDropOfTime()) == 0 &&
                carId == reservationExist.get().getCarId())
            System.out.println();
        else if (checkStatus)
            throw new BadRequestException("Car is already reserved! Please choose another");

        Double totalPrice = totalPrice(reservation.getPickUpTime(), reservation.getDropOfTime(), carId.getId());
        reservationExist.get().setTotalPrice(totalPrice);

        reservationExist.get().setCarId(carId);
        reservationExist.get().setPickUpTime(reservation.getPickUpTime());
        reservationExist.get().setDropOfTime(reservation.getDropOfTime());
        reservationExist.get().setPickUpLocation(reservation.getPickUpLocation());
        reservationExist.get().setDropOfLocation(reservation.getDropOfLocation());
        reservationExist.get().setStatus(reservation.getStatus());

        reservationRepository.save(reservationExist.get());
    }


    public void removeById(Long id) throws ResourceNotFoundException {
        boolean reservationExists = reservationRepository.existsById(id);

        if (!reservationExists){
            throw new ResourceNotFoundException("reservation does not exist");
        }

        reservationRepository.deleteById(id);
    }



    public boolean carAvailability(Long carId, LocalDateTime pickUpTime, LocalDateTime dropOfTime) {
        List<Reservation> checkStatus = reservationRepository.checkStatus(carId, pickUpTime, dropOfTime,
                ReservationStatus.DONE, ReservationStatus.CANCELED);

        return checkStatus.size() > 0;
    }

    public Double totalPrice(LocalDateTime pickUpTime, LocalDateTime dropOfTime, Long carId) {
        Car car = carRepository.findById(carId).orElseThrow(() ->
                new ResourceNotFoundException(String.format(CAR_NOT_FOUND_MSG, carId)));

        Long hours = (new Reservation()).getTotalHours(pickUpTime, dropOfTime);
        return car.getPricePerHour() * hours;
    }
}
