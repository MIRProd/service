package ro.unibuc.hello.repository;

import ro.unibuc.hello.data.BookingEntity;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;               
import java.time.LocalDate;          
import org.springframework.data.mongodb.repository.Query;  

public interface BookingRepository extends MongoRepository<BookingEntity, String> {
    List<BookingEntity> findByApartmentId(String apartmentId);
    
    @Query("{'apartmentId': ?0, '$or': [{'startDate': {'$lte': ?2}, 'endDate': {'$gte': ?1}}, {'startDate': {'$lte': ?2, '$gte': ?1}}]}")
    List<BookingEntity> findOverlappingBookings(String apartmentId, LocalDate startDate, LocalDate endDate);
    
    @Query(value = "{'$or': [{'startDate': {'$lte': ?1}, 'endDate': {'$gte': ?0}}, {'startDate': {'$lte': ?1, '$gte': ?0}}]}", fields = "{'apartmentId': 1}")
    List<BookingEntity> findBookedApartmentIds(LocalDate startDate, LocalDate endDate);

    List<BookingEntity> findByApartmentIdAndUserId(String apartmentId, String userId);
}
