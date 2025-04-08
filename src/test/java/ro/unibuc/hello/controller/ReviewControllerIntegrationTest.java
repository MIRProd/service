package ro.unibuc.hello.controller;

import java.time.LocalDate;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import ro.unibuc.hello.data.UserEntity;
import ro.unibuc.hello.data.BookingEntity;
import org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ro.unibuc.hello.data.ReviewEntity;
import ro.unibuc.hello.repository.ReviewRepository;
import ro.unibuc.hello.service.ReviewService;
import ro.unibuc.hello.repository.UserRepository;
import ro.unibuc.hello.repository.BookingRepository;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class ReviewControllerIntegrationTest {

    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            .withExposedPorts(27017);

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
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void cleanUp() {
        reviewRepository.deleteAll();
        userRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    private void createTestData() {
        // Create and save user
        UserEntity user = new UserEntity();
        user.setId("user123");
        user.setName("Test User");
        user.setEmail("test@example.com");
        userRepository.save(user);
        
        // Create and save booking
        BookingEntity booking = new BookingEntity();
        booking.setApartmentId("apartment123");
        booking.setUserId("user123");
        booking.setStartDate(LocalDate.of(2025, 1, 1));
        booking.setEndDate(LocalDate.of(2025, 1, 5));
        bookingRepository.save(booking);
    }

    @Test
    public void testCreateReview_Success() throws Exception {
        createTestData();
        ReviewEntity review = new ReviewEntity("1", "Great apartment!", 4, "apartment123", "user123");

        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(content().string("Review successfully created!"));
    }

    @Test
    public void testAddLikeToReview_Success() throws Exception {
        createTestData();
        ReviewEntity review = new ReviewEntity("1", "Great apartment!", 4, "apartment123", "user123");
        reviewRepository.save(review);

        mockMvc.perform(post("/reviews/1/like")
                .param("userId", "user123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Like added successfully!"));

        // Verificare prin endpoint-ul de get
        mockMvc.perform(get("/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").isArray())
                .andExpect(jsonPath("$.likes.length()").value(1))
                .andExpect(jsonPath("$.likes[0]").value("user123"));
    }

    @Test
    public void testAddLikeAndDislike_RemoveLike_Success() throws Exception {
        createTestData();
        ReviewEntity review = new ReviewEntity("1", "Great apartment!", 4, "apartment123", "user123");
        reviewRepository.save(review);

        // Adaugă like
        mockMvc.perform(post("/reviews/1/like")
                .param("userId", "user123"))
                .andExpect(status().isOk());

        // Adaugă dislike (ar trebui să elimine like-ul)
        mockMvc.perform(post("/reviews/1/dislike")
                .param("userId", "user123"))
                .andExpect(status().isOk());

        // Verifică starea finală
        mockMvc.perform(get("/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").isArray())
                .andExpect(jsonPath("$.likes.length()").value(0))
                .andExpect(jsonPath("$.dislikes").isArray())
                .andExpect(jsonPath("$.dislikes.length()").value(1))
                .andExpect(jsonPath("$.dislikes[0]").value("user123"));
    }
}