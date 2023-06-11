package de.rieckpil.courses.book.review;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReviewRepositoryNoInMemoryTest {

  @Container
  //@ServiceConnection //seit SB 3.1, damit ist @DynamicPropertySource unnoetig!
  static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15.3")
    .withDatabaseName("test")
    .withUsername("duke")
    .withPassword("s3cret")
    // withReuse(true) funktioniert nur, wenn .testcontainers.properties den noetigen Eintrag hat
    // und wenn der Lifecycle selbst verwaltet wird:
    // - @TestContainers, @Container entfernen
    // - container.start() in static Block aufrufen
    .withReuse(true)
    ;

  @DynamicPropertySource // seit SB 3.1 unnoetig, s.o.
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", container::getJdbcUrl);
    registry.add("spring.datasource.password", container::getPassword);
    registry.add("spring.datasource.username", container::getUsername);
  }

//  // wenn @TestContainers und @Container entfernt werden, kann man so
//  // den Container selbst starten. Wird fuer withReuse(true) benoetigt!
//  static {
//    container.start();
//  }

  @Autowired
  private ReviewRepository cut;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private TestEntityManager testEntityManager;

  @Test
  void notNull() {
    assertNotNull(entityManager);
    assertNotNull(testEntityManager);
    assertNotNull(dataSource);
    assertNotNull(cut);
  }
  @Test
  @Sql(scripts = "/scripts/INIT_REVIEW_EACH_BOOK.sql")
  void shouldGetTwoReviewStatisticsWhenDatabaseContainsTwoBooksWithReview() {
    assertThat(cut.count(), is(3L));
    assertThat(cut.getReviewStatistics().size(), is(2));

    cut.getReviewStatistics().forEach(reviewStatistic -> {
      System.out.println(reviewStatistic.getId());
      System.out.println(reviewStatistic.getIsbn());
      System.out.println(reviewStatistic.getAvg());
      System.out.println(reviewStatistic.getRatings());
    });
  }
  @Test
  void databaseIsEmptyBeforeEachTestWhenNoDataAreSetup() {
    assertThat(cut.count(), is(0L));
  }

}
