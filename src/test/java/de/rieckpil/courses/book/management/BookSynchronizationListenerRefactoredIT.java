package de.rieckpil.courses.book.management;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.rieckpil.courses.AbstractIntegrationTest;
import de.rieckpil.courses.initializer.RSAKeyGenerator;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.given;

class BookSynchronizationListenerRefactoredIT extends AbstractIntegrationTest {

  private static final String ISBN = "9780596004651";
  private static String VALID_RESPONSE;

  static {
    try {
      VALID_RESPONSE =
        new String(
          BookSynchronizationListenerRefactoredIT.class
            .getClassLoader()
            .getResourceAsStream("stubs/openlibrary/success-" + ISBN + ".json")
            .readAllBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Autowired private SqsTemplate sqsTemplate;

  @Autowired private WebTestClient webTestClient;

  @Autowired private BookRepository bookRepository;

  @Test
  void shouldGetSuccessWhenClientIsAuthenticated() throws JOSEException {
    // Mache Request zu geschuetztem Endpoint und vergewissere dass mit authentifiziertem
    // Benutzer, also gueltigem authorization-token im Header, eine 200er Antwort
    // vom Server kommt
    webTestClient
      .get()
      .uri("/api/books/reviews/statistics")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + getSignedJWT())
      .exchange()
      .expectStatus().is2xxSuccessful()
    ;
  }

  @Test
  void shouldReturnBookFromAPIWhenApplicationConsumesNewSyncRequest() {

    webTestClient
      .get()
      .uri("/api/books")
      .exchange()
      .expectStatus().isOk()
      .expectBody().jsonPath("$.size()").isEqualTo(0)
    ;

    this.openLibraryStubs.stubForSuccessfulBookResponse(ISBN, VALID_RESPONSE);

    this.sqsTemplate.send(
      QUEUE_NAME,
      """
          {
            "isbn": "%s"
          }
        """.formatted(ISBN));

    given()
      .atMost(Duration.ofSeconds(5))
      .await()
      .untilAsserted(() -> {
        webTestClient
          .get()
          .uri("/api/books")
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].isbn").isEqualTo(ISBN)
        ;
      });
  }
}
