package ro.unibuc.hello.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.hello.dto.Greeting;

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

import ro.unibuc.hello.service.GreetingsService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// integration between two components: `GreetingsService` and `GreetingsController` (the requests are made using mockMvc)
// Annotation to specify that this class contains integration tests for Spring Boot application.
@SpringBootTest
// Annotation to automatically configure a MockMvc instance.
@AutoConfigureMockMvc
// Annotation to enable Testcontainers support.
@Testcontainers
// Tagging this test class as an integration test for categorization.
//@Tag("IntegrationTest")
public class GreetingsControllerIntegrationTest {

    // Declaring a Testcontainers MongoDB container.
    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            // Exposing port 27017 from the MongoDB container.
            .withExposedPorts(27017)
            // enable sharding on MongoDB container
            .withSharding();

    // Method executed once before all tests to start the MongoDB container.
    @BeforeAll
    public static void setUp() {
        mongoDBContainer.start();
    }

    // Method executed once after all tests to stop the MongoDB container.
    @AfterAll
    public static void tearDown() {
        mongoDBContainer.stop();
    }

    // Method to dynamically set MongoDB connection properties for the test environment.
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // Constructing MongoDB connection URL with localhost and mapped port.
        final String MONGO_URL = "mongodb://localhost:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));

        // Adding MongoDB connection URL property to the registry.
        registry.add("mongodb.connection.url", () -> MONGO_URL + PORT);
    }

    // Autowiring MockMvc instance for HTTP request simulation.
    @Autowired
    private MockMvc mockMvc;

    // Autowiring GreetingsService for interacting with the MongoDB database.
    @Autowired
    private GreetingsService greetingsService;

    // Method executed before each test to clean up and add test data to the database.
    @BeforeEach
    public void cleanUpAndAddTestData() {
        // Creating test Greeting objects.
        Greeting greeting1 = new Greeting("1", "Hello 1");
        Greeting greeting2 = new Greeting("2", "Hello 2");

        // Saving test Greetings to the database via GreetingsService.
        greetingsService.saveGreeting(greeting1);
        greetingsService.saveGreeting(greeting2);
    }

    // Integration test to verify the endpoint for retrieving all greetings.
    @Test
    public void testGetAllGreetings() throws Exception {
        mockMvc.perform(get("/greetings"))
            // Verifying HTTP response status code is 200 OK.
            .andExpect(status().isOk())
            // Verifying content type is JSON.
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            // Verifying there are 2 greetings returned.
            .andExpect(jsonPath("$.length()").value(2))
            // Verifying content of the first greeting.
            .andExpect(jsonPath("$[0].content").value("Hello 1"))
            // Verifying content of the second greeting.
            .andExpect(jsonPath("$[1].content").value("Hello 2"));
    }
}
