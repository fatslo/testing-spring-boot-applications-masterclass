package de.rieckpil.courses.book.review;

import de.rieckpil.courses.book.review.RandomReviewParameterResolverExtension.RandomReview;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({RandomReviewParameterResolverExtension.class})
class ReviewVerifier2Test {
  @Test
  @DisplayName("Should fail when review contains swearword")
  void shouldFailWhenReviewContainsSwearword() {
    String review = "one two three four five six seven eight nine ten shit";
    ReviewVerifier reviewVerifier = new ReviewVerifier();

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result);
  }

  @Test
  @DisplayName("Should fail when review contains Lorem ipsum")
  void testLoremIpsum() {
    String review = "Lorem ipsum one two three four five six seven eight nine ten";
    ReviewVerifier reviewVerifier = new ReviewVerifier();

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "should have failed due to Lorem ipsum in review");
  }

  @ParameterizedTest
  @DisplayName("should fail when review is of bad quality")
  @CsvFileSource(resources = "/badReview.csv")
  void testBadReview(String review, boolean exp) {
    ReviewVerifier reviewVerifier = new ReviewVerifier();

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertEquals(exp, result);
  }

  @RepeatedTest(3)
  void shouldFailWhenRandomReviewQualityIsBad(@RandomReview String review) {
    ReviewVerifier reviewVerifier = new ReviewVerifier();

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "did not detect random bad review");
  }

  @Test
  void shouldPass() {
    ReviewVerifier reviewVerifier = new ReviewVerifier();
    String review = " I can totally recomment this book who is interested in learning all.";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertTrue(result, "should pass");
  }
}
