package de.rieckpil.courses.book.management;

import com.nimbusds.jose.JOSEException;
import de.rieckpil.courses.initializer.RSAKeyGenerator;
import de.rieckpil.courses.initializer.WireMockInitializer;
import de.rieckpil.courses.stubs.OAuth2Stubs;
import de.rieckpil.courses.stubs.OpenLibraryStubs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("integration-test")
class BookSynchronizationListenerIT {

  @Container
  static PostgreSQLContainer database =
    (PostgreSQLContainer) new PostgreSQLContainer("postgres:12")
      .withDatabaseName("test")
      .withUsername("duke")
      .withPassword("s3cret")
      .withReuse(true);
  ;

  @Container
  static LocalStackContainer localStack =
    (LocalStackContainer) new LocalStackContainer("0.10.0")
      .withServices(LocalStackContainer.Service.SQS)
      .withEnv("DEFAULT_REGION", "eu-central-1")
      .withReuse(true)
    ;

  private static final String QUEUE_NAME = UUID.randomUUID().toString();
  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", database::getJdbcUrl);
    registry.add("spring.datasource.password", database::getPassword);
    registry.add("spring.datasource.username", database::getUsername);
    registry.add("sqs.book-synchronization-queue", () -> QUEUE_NAME);
  }
  @TestConfiguration
  static class TestConfig {
    @Bean
    public AmazonSQS amazonSQSAsync() {
      return Amazon
    }
  }

  @BeforeAll
  static void beforeAll() throws IOException, InterruptedException {
    localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
  }
  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private RSAKeyGenerator rsaKeyGenerator;

  @Autowired
  private OAuth2Stubs oAuth2Stubs;

  @Autowired
  private OpenLibraryStubs openLibraryStubs;

  @Autowired
  private BookRepository bookRepository;

  @Test
  void shouldGetSuccessWhenClientIsAuthenticated() throws JOSEException {
  }

  @Test
  void shouldReturnBookFromAPIWhenApplicationConsumesNewSyncRequest() {
  }
}
