package ro.unibuc.hello.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.unibuc.hello.data.BookingEntity;
import ro.unibuc.hello.repository.BookingRepository;
import ro.unibuc.hello.repository.UserRepository;
import ro.unibuc.hello.repository.ApartmentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final MeterRegistry meterRegistry;
    
    private final Counter bookingCreationAttempts;
    private final Counter bookingCreationSuccesses;
    private final Counter bookingCreationFailures;
    private final Counter availabilityCheckCounter;
    
    private final Timer availabilityCheckTimer;

    @Autowired
    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, ApartmentRepository apartmentRepository, @Autowired(required = false) MeterRegistry meterRegistry) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
        
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();

        this.bookingCreationAttempts = this.meterRegistry.counter("booking.creation.attempts", "type", "total");
        this.bookingCreationSuccesses = this.meterRegistry.counter("booking.creation.attempts", "type", "success");
        this.bookingCreationFailures = this.meterRegistry.counter("booking.creation.attempts", "type", "failure");
        this.availabilityCheckCounter = this.meterRegistry.counter("booking.availability.checks");
        
        this.availabilityCheckTimer = this.meterRegistry.timer("booking.availability.check.time");
    }

    public List<BookingEntity> getAllBookings() {
        meterRegistry.counter("booking.fetch.all").increment();
        return bookingRepository.findAll();
    }

    public Optional<BookingEntity> getBookingById(String id) {
        meterRegistry.counter("booking.fetch.by.id").increment();
        return bookingRepository.findById(id);
    }

    public BookingEntity createBooking(BookingEntity booking) {
        bookingCreationAttempts.increment();
        
        try {
            if (!apartmentRepository.existsById(booking.getApartmentId())) {
                bookingCreationFailures.increment();
                throw new IllegalArgumentException("Apartment with ID " + booking.getApartmentId() + " does not exist.");
            }

            if (!userRepository.existsById(booking.getUserId())) {
                bookingCreationFailures.increment();
                throw new IllegalArgumentException("User with ID " + booking.getUserId() + " does not exist.");
            }

            List<BookingEntity> allBookings = bookingRepository.findAll();
            boolean isAvailable = isApartmentAvailableJava(
                    allBookings, 
                    booking.getApartmentId(),
                    booking.getStartDate(), 
                    booking.getEndDate()
            );

            if (!isAvailable) {
                bookingCreationFailures.increment();
                throw new IllegalArgumentException("Apartment is not available for the selected dates.");
            }

            bookingCreationSuccesses.increment();
            
            long bookingDurationDays = java.time.temporal.ChronoUnit.DAYS.between(
                    booking.getStartDate(), booking.getEndDate());
            meterRegistry.summary("booking.duration.days").record(bookingDurationDays);
            
            return bookingRepository.save(booking);
        } catch (Exception e) {
            bookingCreationFailures.increment();
            throw e;
        }
    }

public void triggerBookingFailure() {
        bookingCreationFailures.increment();  // Incrementăm contorul de erori
    }

    public void deleteBooking(String id) {
        meterRegistry.counter("booking.deletions").increment();
        bookingRepository.deleteById(id);
    }
    
    public boolean isApartmentAvailable(String apartmentId, LocalDate startDate, LocalDate endDate) {
        availabilityCheckCounter.increment();
        
        return availabilityCheckTimer.record(() -> {
            if (!apartmentRepository.existsById(apartmentId)) {
                throw new IllegalArgumentException("Apartment with ID " + apartmentId + " does not exist.");
            }
            
            List<BookingEntity> allBookings = bookingRepository.findAll();
            return isApartmentAvailableJava(allBookings, apartmentId, startDate, endDate);
        });
    }
    
    public List<BookingEntity> getBookingsForApartment(String apartmentId) {
        meterRegistry.counter("booking.fetch.by.apartment").increment();
        return bookingRepository.findByApartmentId(apartmentId);
    }
    
    public List<BookingEntity> getBookingsForApartmentAndUser(String apartmentId, String userId) {
        meterRegistry.counter("booking.fetch.by.apartment.and.user").increment();
        return bookingRepository.findByApartmentIdAndUserId(apartmentId, userId);
    }
    
    public List<BookingEntity> findOverlappingBookingsJava(
            List<BookingEntity> bookings,
            String apartmentId,
            LocalDate startDate,
            LocalDate endDate) {
        
        return bookings.stream()
                .filter(booking -> booking.getApartmentId().equals(apartmentId))
                .filter(booking -> {
                    boolean condition1 = !booking.getStartDate().isAfter(endDate) && 
                                         !booking.getEndDate().isBefore(startDate);
                    
                    boolean condition2 = !booking.getStartDate().isAfter(endDate) && 
                                         !booking.getStartDate().isBefore(startDate);
                    
                    return condition1 || condition2;
                })
                .collect(Collectors.toList());
    }
    
    public boolean isApartmentAvailableJava(
            List<BookingEntity> bookings,
            String apartmentId,
            LocalDate startDate,
            LocalDate endDate) {
        
        List<BookingEntity> overlappingBookings = findOverlappingBookingsJava(
                bookings, apartmentId, startDate, endDate);
        
        return overlappingBookings.isEmpty();
    }
    
    public List<String> findBookedApartmentIdsJava(
            List<BookingEntity> bookings,
            LocalDate startDate,
            LocalDate endDate) {
        
        return bookings.stream()
                .filter(booking -> {
                    boolean condition1 = !booking.getStartDate().isAfter(endDate) && 
                                         !booking.getEndDate().isBefore(startDate);
                    
                    boolean condition2 = !booking.getStartDate().isAfter(endDate) && 
                                         !booking.getStartDate().isBefore(startDate);
                    
                    return condition1 || condition2;
                })
                .map(BookingEntity::getApartmentId)
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<String> findAvailableApartmentIds(LocalDate startDate, LocalDate endDate) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            List<BookingEntity> allBookings = bookingRepository.findAll();
            List<String> bookedApartmentIds = findBookedApartmentIdsJava(allBookings, startDate, endDate);
            
            List<String> allApartmentIds = apartmentRepository.findAll().stream()
                    .map(apartment -> apartment.getId())
                    .collect(Collectors.toList());
            
            allApartmentIds.removeAll(bookedApartmentIds);
            
            return allApartmentIds;
        } finally {
            sample.stop(meterRegistry.timer("booking.available.apartments.search.time"));
        }
    }
}