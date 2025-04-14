package ro.unibuc.hello.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ro.unibuc.hello.data.UserEntity;
import ro.unibuc.hello.repository.UserRepository;
import ro.unibuc.hello.service.ApartmentService;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class ApartmentControllerIntegrationTest {

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
        final String MONGO_URL = "mongodb://host.docker.internal:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));
        registry.add("spring.data.mongodb.uri", () -> MONGO_URL + PORT + "/testdb");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApartmentService apartmentService;

    @Autowired
    private UserRepository userRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ApartmentEntity luxuryApartment;
    private ApartmentEntity modernFlat;
    private UserEntity user;

    @BeforeEach
    public void cleanUpAndAddTestData() {
        apartmentService.deleteAllApartments();
        userRepository.deleteAll();

        // Adaugă utilizator
        user = new UserEntity();
        user.setId("user1");
        user.setName("Test User");
        user.setEmail("test@example.com");
        userRepository.save(user);

        // Adaugă apartamente
        luxuryApartment = new ApartmentEntity(
                "Luxury Apartment", "București", 250.0, "user1",
                3, 2, true, Arrays.asList("Wi-Fi", "TV", "balcon", "aer condiționat"), 90.0, false
        );
        modernFlat = new ApartmentEntity(
                "Modern Flat", "Cluj-Napoca", 180.0, "user1",
                2, 1, false, Arrays.asList("Wi-Fi", "TV", "mașină de spălat"), 65.5, true
        );

        luxuryApartment = apartmentService.createApartment(luxuryApartment);
        modernFlat = apartmentService.createApartment(modernFlat);
    }

    @Test
    public void getAllApartments() throws Exception {
        mockMvc.perform(get("/apartments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].title").value("Luxury Apartment"))
                .andExpect(jsonPath("$[1].title").value("Modern Flat"));
    }

    @Test
    public void getApartmentById_Success() throws Exception {
        mockMvc.perform(get("/apartments/" + luxuryApartment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(luxuryApartment.getId()))
                .andExpect(jsonPath("$.title").value("Luxury Apartment"));
    }

    @Test
    public void getApartmentById_NotFound() throws Exception {
        mockMvc.perform(get("/apartments/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    public void createApartment_Success() throws Exception {
        ApartmentEntity newApartment = new ApartmentEntity(
                "New Apartment", "Timișoara", 150.0, "user1",
                1, 1, false, Arrays.asList("Wi-Fi"), 50.0, true
        );

        mockMvc.perform(post("/apartments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newApartment)))
                .andExpect(status().isOk())
                .andExpect(content().string("Apartment successfully created!"));
    }

    @Test
    public void createApartment_UserNotFound() throws Exception {
        ApartmentEntity invalidApartment = new ApartmentEntity(
                "Invalid Apartment", "Iași", 100.0, "nonexistent_user",
                1, 1, false, Arrays.asList("TV"), 40.0, false
        );

        mockMvc.perform(post("/apartments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidApartment)))
                .andExpect(status().isOk())
                .andExpect(content().string("User with ID nonexistent_user does not exist."));
    }

    @Test
    public void deleteApartment() throws Exception {
        mockMvc.perform(delete("/apartments/" + luxuryApartment.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void getAvailableApartments() throws Exception {
        mockMvc.perform(get("/apartments/available")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Ambele sunt disponibile fără rezervări
                .andExpect(jsonPath("$[0].title").value("Luxury Apartment"))
                .andExpect(jsonPath("$[1].title").value("Modern Flat"));
    }

    @Test
    public void isApartmentAvailable_Available() throws Exception {
        mockMvc.perform(get("/apartments/" + luxuryApartment.getId() + "/available")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-05"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void getPetFriendlyApartments() throws Exception {
        mockMvc.perform(get("/apartments/pet-friendly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Luxury Apartment"))
                .andExpect(jsonPath("$[0].petFriendly").value(true));
    }

    @Test
    public void getApartmentsByNumberOfRooms_Success() throws Exception {
        mockMvc.perform(get("/apartments/rooms/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Modern Flat"))
                .andExpect(jsonPath("$[0].numberOfRooms").value(2));
    }

    @Test
    public void getApartmentsByNumberOfRooms_InvalidInput() throws Exception {
        mockMvc.perform(get("/apartments/rooms/0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Number of rooms must be greater than zero"));
    }

    @Test
    public void getApartmentsByNumberOfBathrooms_Success() throws Exception {
        mockMvc.perform(get("/apartments/bathrooms/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Luxury Apartment"))
                .andExpect(jsonPath("$[0].numberOfBathrooms").value(2));
    }

    @Test
    public void getApartmentsByMinimumSquareMeters_Success() throws Exception {
        mockMvc.perform(get("/apartments/square-meters")
                        .param("minSquareMeters", "80.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Luxury Apartment"))
                .andExpect(jsonPath("$[0].squareMeters").value(90.0));
    }

    @Test
    public void getApartmentsByMaxPrice_Success() throws Exception {
        mockMvc.perform(get("/apartments/price")
                        .param("maxPrice", "200.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Modern Flat"))
                .andExpect(jsonPath("$[0].pricePerNight").value(180.0));
    }

    @Test
    public void getApartmentsByAmenity_Success() throws Exception {
        mockMvc.perform(get("/apartments/amenities")
                        .param("amenity", "Wi-Fi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Luxury Apartment"))
                .andExpect(jsonPath("$[1].title").value("Modern Flat"));
    }

    @Test
    public void getApartmentsByAmenity_InvalidInput() throws Exception {
        mockMvc.perform(get("/apartments/amenities")
                        .param("amenity", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Amenity must not be empty"));
    }

    @Test
    public void getApartmentsBySmokingAllowed_Success() throws Exception {
        mockMvc.perform(get("/apartments/smoking")
                        .param("smokingAllowed", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Modern Flat"))
                .andExpect(jsonPath("$[0].smokingAllowed").value(true));
    }
    /*
    @Test
    public void getApartmentsByLocation_Success() throws Exception {
        mockMvc.perform(get("/apartments/location")
                        .param("location", "București"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Luxury Apartment"))
                .andExpect(jsonPath("$[0].location").value("București"));
    }
    */
    @Test
    public void getApartmentsByLocation_InvalidInput() throws Exception {
        mockMvc.perform(get("/apartments/location")
                        .param("location", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Location must not be empty"));
    }

    @Test
    public void getApartmentsByPriceAndSquareMeters_Success() throws Exception {
        mockMvc.perform(get("/apartments/filter")
                        .param("minPrice", "150.0")
                        .param("maxPrice", "200.0")
                        .param("minSquareMeters", "60.0")
                        .param("maxSquareMeters", "70.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Modern Flat"))
                .andExpect(jsonPath("$[0].pricePerNight").value(180.0))
                .andExpect(jsonPath("$[0].squareMeters").value(65.5));
    }

    @Test
    public void getApartmentsByPriceAndSquareMeters_InvalidInput() throws Exception {
        mockMvc.perform(get("/apartments/filter")
                        .param("minPrice", "200.0")
                        .param("maxPrice", "100.0")
                        .param("minSquareMeters", "50.0")
                        .param("maxSquareMeters", "80.0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Minimum price cannot be greater than maximum price"));
    }

    @Test
    public void searchApartments_Success() throws Exception {
        mockMvc.perform(get("/apartments/search")
                        .param("maxPrice", "200.0")
                        .param("numberOfRooms", "2")
                        .param("smokingAllowed", "true")
                        .param("amenity", "Wi-Fi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Modern Flat"))
                .andExpect(jsonPath("$[0].pricePerNight").value(180.0))
                .andExpect(jsonPath("$[0].numberOfRooms").value(2));
    }

    @Test
    public void searchApartments_InvalidRating() throws Exception {
        mockMvc.perform(get("/apartments/search")
                        .param("minAverageRating", "6.0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Minimum average rating must be between 1 and 5"));
    }

    @Test
    public void getApartmentsByMinAverageRating_Success() throws Exception {
        // Notă: Fără review-uri, logica rating-ului nu va funcționa complet; presupunem că toate au rating 0
        mockMvc.perform(get("/apartments/by-rating")
                        .param("minAverageRating", "4.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0)); // Ajustăm dacă adaugi review-uri
    }

    @Test
    public void getApartmentsByMinAverageRating_InvalidInput() throws Exception {
        mockMvc.perform(get("/apartments/by-rating")
                        .param("minAverageRating", "6.0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Minimum average rating must be between 1 and 5"));
    }
}