package ro.unibuc.hello.controller;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.hello.data.BookingEntity;
import ro.unibuc.hello.service.BookingService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @Timed(value = "booking.controller.get.all.time", description = "Time taken to retrieve all bookings")
    @Counted(value = "booking.controller.get.all.count", description = "Number of times all bookings were retrieved")
    public List<BookingEntity> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{id}")
    @Timed(value = "booking.controller.get.by.id.time", description = "Time taken to retrieve a booking by ID")
    @Counted(value = "booking.controller.get.by.id.count", description = "Number of times a booking was retrieved by ID")
    public Optional<BookingEntity> getBookingById(@PathVariable String id) {
        return bookingService.getBookingById(id);
    }

    @PostMapping
    @Timed(value = "booking.controller.create.time", description = "Time taken to create a booking")
    @Counted(value = "booking.controller.create.count", description = "Number of booking creation requests")
    public ResponseEntity<?> createBooking(@RequestBody BookingEntity booking) {
        try {
            BookingEntity createdBooking = bookingService.createBooking(booking);
            return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @Timed(value = "booking.controller.delete.time", description = "Time taken to delete a booking")
    @Counted(value = "booking.controller.delete.count", description = "Number of booking deletion requests")
    public void deleteBooking(@PathVariable String id) {
        bookingService.deleteBooking(id);
    }
    
    @GetMapping("/by-apartment/{apartmentId}")
    @Timed(value = "booking.controller.get.by.apartment.time", description = "Time taken to retrieve bookings by apartment")
    @Counted(value = "booking.controller.get.by.apartment.count", description = "Number of requests for bookings by apartment")
    public List<BookingEntity> getBookingsForApartment(@PathVariable String apartmentId) {
        return bookingService.getBookingsForApartment(apartmentId);
    }
    
    @GetMapping("/by-apartment-and-user")
    @Timed(value = "booking.controller.get.by.apartment.and.user.time", description = "Time taken to retrieve bookings by apartment and user")
    @Counted(value = "booking.controller.get.by.apartment.and.user.count", description = "Number of requests for bookings by apartment and user")
    public List<BookingEntity> getBookingsForApartmentAndUser(
            @RequestParam String apartmentId, 
            @RequestParam String userId) {
        return bookingService.getBookingsForApartmentAndUser(apartmentId, userId);
    }

    @GetMapping("/check-availability/{apartmentId}")
    @Timed(value = "booking.controller.check.availability.time", description = "Time taken to check apartment availability")
    @Counted(value = "booking.controller.check.availability.count", description = "Number of apartment availability check requests")
    public ResponseEntity<?> checkAvailability(
            @PathVariable String apartmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            boolean isAvailable = bookingService.isApartmentAvailable(apartmentId, startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("apartmentId", apartmentId);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("available", isAvailable);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/available-apartments")
    @Timed(value = "booking.controller.available.apartments.time", description = "Time taken to find available apartments")
    @Counted(value = "booking.controller.available.apartments.count", description = "Number of requests for available apartments")
    public ResponseEntity<?> getAvailableApartments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<String> availableApartmentIds = bookingService.findAvailableApartmentIds(startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("availableApartmentIds", availableApartmentIds);
            response.put("count", availableApartmentIds.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
