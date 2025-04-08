package ro.unibuc.hello.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ro.unibuc.hello.data.ApartmentEntity;
import ro.unibuc.hello.data.BookingEntity;
import ro.unibuc.hello.data.ReviewEntity;
import ro.unibuc.hello.data.UserEntity;
import ro.unibuc.hello.repository.ApartmentRepository;
import ro.unibuc.hello.repository.BookingRepository;
import ro.unibuc.hello.repository.UserRepository;
import ro.unibuc.hello.service.ReviewService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class ReviewControllerIntegrationTest {

    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            .withExposedPorts(27017)
            .withSharding();

    @BeforeAll
    public static void setUp() {
        mongoDBContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        mongoDBContainer.stop();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        final String MONGO_URL = "mongodb://host.docker.internal:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));

        registry.add("mongodb.connection.url", () -> MONGO_URL + PORT);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewService reviewService;

    private ApartmentEntity apartment;
    private UserEntity user;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void cleanUpAndAddTestData() {
        bookingRepository.deleteAll();
        apartmentRepository.deleteAll();
        userRepository.deleteAll();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        user = new UserEntity();
        user.setId("user123");
        user.setName("Test User");
        user.setEmail("test@example.com");
        userRepository.save(user);

        apartment = new ApartmentEntity();
        apartment.setId("apartment123");
        apartment.setTitle("Test Apartment");
        apartment.setLocation("Test Location");
        apartment.setPricePerNight(150.0);
        apartment.setUserId("user123");
        apartmentRepository.save(apartment);

        BookingEntity booking = new BookingEntity();
        booking.setApartmentId("apartment123");
        booking.setUserId("user123");
        booking.setStartDate(LocalDate.of(2025, 1, 1));
        booking.setEndDate(LocalDate.of(2025, 1, 7));
        bookingRepository.save(booking);
    }

    @Test
    public void testCreateReview_Success() throws Exception {
        ReviewEntity review = new ReviewEntity();
        review.setApartmentId("apartment123");
        review.setUserId("user123");
        review.setRating(5);
        review.setComment("Great apartment!");

        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isCreated()) // HTTP 201
                .andExpect(jsonPath("$.comment").value("Great apartment!"))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    public void testCreateReview_UserMustHaveBooked() throws Exception {
        // Mocks: No booking for the user
        when(bookingRepository.findByApartmentIdAndUserId("apartment123", "user123"))
            .thenReturn(Collections.emptyList());  // No booking found

        ReviewEntity review = new ReviewEntity();
        review.setApartmentId("apartment123");
        review.setUserId("user123");
        review.setRating(5);
        review.setComment("Great apartment!");

        // Act & Assert: Send a POST request to create the review and check the response
        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isBadRequest()) // HTTP 400
                .andExpect(content().string("User must have booked the apartment before leaving a review."));
    }

    @Test
    public void testGetAllReviews_Success() throws Exception {
        // Simulate reviews being available
        ReviewEntity review = new ReviewEntity("1", "Great apartment!", 5, "apartment123", "user123");
        when(reviewService.getAllReviewsSortedByRating()).thenReturn(Arrays.asList(review));

        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk())  // Expecting HTTP status 200 (OK)
                .andExpect(jsonPath("$[0].comment").value("Great apartment!"))
                .andExpect(jsonPath("$[0].rating").value(5));  // Check for "comment" and "rating"
    }

    @Test
    public void testAddLike_AfterAddDislike_ShouldRemoveDislike() throws Exception {
        // Add initial dislike
        Set<String> dislikes = new HashSet<>();
        dislikes.add("user123");

        ReviewEntity review = new ReviewEntity("1", "Great apartment!", 5, "apartment123", "user123");
        review.setDislikes(dislikes);

        // Mock adding dislike and like
        when(reviewService.addDislike("1", "user123")).thenReturn("Dislike added successfully!");
        when(reviewService.addLike("1", "user123")).thenReturn("Like added successfully!");

        // Act & Assert: Adding dislike
        mockMvc.perform(post("/reviews/1/dislike")
                .param("userId", "user123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Dislike added successfully!"));

        // Act & Assert: Adding like after dislike
        mockMvc.perform(post("/reviews/1/like")
                .param("userId", "user123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Like added successfully!"));

        // Check that like was added and dislike was removed
        assertTrue(review.getLikes().contains("user123"));
        assertFalse(review.getDislikes().contains("user123"));
    }
}
