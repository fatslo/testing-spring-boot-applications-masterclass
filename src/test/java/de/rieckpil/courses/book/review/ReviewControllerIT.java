package de.rieckpil.courses.book.review;

import com.nimbusds.jose.JOSEException;
import de.rieckpil.courses.AbstractIntegrationTest;
import de.rieckpil.courses.book.management.Book;
import de.rieckpil.courses.book.management.BookRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;

class ReviewControllerIT extends AbstractIntegrationTest {

  private static final String ISBN = "9780596004651";

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @BeforeEach
  void beforeEach() {
    // es reicht, lediglich das Buch fuer den Review zu speichern, der gesamte Prozess
    // mit Import des Buchs ist bereits an anderer Stelle getestet.
    Book book = new Book();
    book.setPublisher("Duke Inc.");
    book.setIsbn(ISBN);
    book.setPages(42L);
    book.setTitle("Joyful testing with Spring Boot");
    book.setDescription("Writing unit and integration tests for Spring Boot applications");
    book.setAuthor("rieckpil");
    book.setThumbnailUrl(
      "https://rieckpil.de/wp-content/uploads/2020/08/tsbam_introduction_thumbnail-585x329.png.webp");
    book.setGenre("Software Development");

    this.bookRepository.save(book);
  }
  @AfterEach
  void afterEach() {
    this.reviewRepository.deleteAll();
    this.bookRepository.deleteAll();
  }

  @Test
  void shouldReturnCreatedReviewWhenBookExistsAndReviewHasGoodQuality() throws JOSEException {
    String reviewPayload =
      """
    {
      "reviewTitle" : "Great book with lots of tips & tricks",
      "reviewContent" : "I can really recommend reading this book. It includes up-to-date library versions and real-world examples",
      "rating": 4
    }
    """;

    String validJWT = getSignedJWT();

    HttpHeaders responseHeaders =
      this.webTestClient
        .post()
        .uri("/api/books/{isbn}/reviews", ISBN)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWT)
        .contentType(MediaType.APPLICATION_JSON) //TODO Was passiert ohne?
        .bodyValue(reviewPayload)
        .exchange()
        .expectStatus()
        .isCreated()
        .returnResult(ResponseEntity.class)
        .getResponseHeaders();

    System.out.println("responseHeaders.getLocation() = " + responseHeaders.getLocation());

    this.webTestClient
      .get()
      .uri(responseHeaders.getLocation())
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJWT)
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .jsonPath("$.reviewTitle")
      .isEqualTo("Great book with lots of tips & tricks")
      .jsonPath("$.rating")
      .isEqualTo(4)
      .jsonPath("$.bookIsbn")
      .isEqualTo(ISBN);

  }

  @Test
  void shouldReturnReviewStatisticWhenMultipleReviewsForBookFromDifferentUsersExist() throws JOSEException {
    String reviewPayload =
      """
    {
      "reviewTitle" : "Great book with lots of tips & tricks",
      "reviewContent" : "I can really recommend reading this book. It includes up-to-date library versions and real-world examples",
      "rating": %d
    }
    """;
    this.webTestClient
      .post()
      .uri("/api/books/{isbn}/reviews", ISBN)
      .header(HttpHeaders.AUTHORIZATION,
        "Bearer " + getSignedJWT("mike", "mike@spring.io"))
      .contentType(MediaType.APPLICATION_JSON) //TODO Was passiert ohne?
      .bodyValue(reviewPayload.formatted(5))
      .exchange()
      .expectStatus()
      ;
    this.webTestClient
      .post()
      .uri("/api/books/{isbn}/reviews", ISBN)
      .header(HttpHeaders.AUTHORIZATION,
        "Bearer " + getSignedJWT("duke", "duke@spring.io"))
      .contentType(MediaType.APPLICATION_JSON) //TODO Was passiert ohne?
      .bodyValue(reviewPayload.formatted(3))
      .exchange()
      .expectStatus()
    ;
    this.webTestClient
      .post()
      .uri("/api/books/{isbn}/reviews", ISBN)
      .header(HttpHeaders.AUTHORIZATION,
        "Bearer " + getSignedJWT("sandy", "sandy@spring.io"))
      .contentType(MediaType.APPLICATION_JSON) //TODO Was passiert ohne?
      .bodyValue(reviewPayload.formatted(4))
      .exchange()
      .expectStatus()
    ;

    this.webTestClient
      .get()
      .uri("/api/books/reviews/statistics")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + getSignedJWT())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$[0].isbn").isEqualTo(ISBN)
      .jsonPath("$[0].ratings").isEqualTo(3)
      .jsonPath("$[0].avg").isEqualTo(4)
      ;
  }
}
