package de.rieckpil.courses.book.review;

import de.rieckpil.courses.book.management.Book;
import de.rieckpil.courses.book.management.BookRepository;
import de.rieckpil.courses.book.management.User;
import de.rieckpil.courses.book.management.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewVerifier mockedReviewVerifier;

  @Mock
  private UserService userService;

  @Mock
  private BookRepository bookRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @InjectMocks
  private ReviewService cut;

  private static final String EMAIL = "duke@spring.io";
  private static final String USERNAME = "duke";
  private static final String ISBN = "42";

  @Test
  void shouldNotBeNull() {
    assertNotNull(reviewRepository);
    assertNotNull(mockedReviewVerifier);
    assertNotNull(userService);
    assertNotNull(bookRepository);
    assertNotNull(cut);
  }

  @Test
  @DisplayName("when reviewed book is not existing then throw exception")
  void shouldThrowExceptionWhenReviewedBookIsNotExisting() {
    when(bookRepository.findByIsbn(ISBN))
      .thenReturn(null)
    ;
    assertThrows(IllegalArgumentException.class, () -> {
      cut.createBookReview(
        ISBN,
        new BookReviewRequest("Java", "asdf", 5),
        USERNAME,
        EMAIL);
    });
  }

  @Test
  void shouldRejectReviewWhenReviewQualityIsBad() {
    var bookReviewRequest = new BookReviewRequest("Java", "badcontent", 5);
    when(bookRepository.findByIsbn(ISBN))
      .thenReturn(new Book())
    ;
    when(mockedReviewVerifier.doesMeetQualityStandards(anyString()))
      .thenReturn(false)
    ;
    assertThrows(BadReviewQualityException.class, () -> {
      cut.createBookReview(
        ISBN,
        bookReviewRequest,
        USERNAME,
        EMAIL);
    });

    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  void shouldStoreReviewWhenReviewQualityIsGoodAndBookIsPresent() {
    var bookReviewRequest = new BookReviewRequest("Java", "good content", 5);
    when(bookRepository.findByIsbn(ISBN))
      .thenReturn(new Book());
    when(mockedReviewVerifier.doesMeetQualityStandards(anyString()))
      .thenReturn(true);
    when(reviewRepository.save(any(Review.class)))
      .thenAnswer(invocation -> {
        Review reviewToSave = invocation.getArgument(0);
        reviewToSave.setId(1L);
        return reviewToSave;
      });

    Long id = cut.createBookReview( ISBN, bookReviewRequest, USERNAME, EMAIL);
    assertEquals(1L, id);
  }
}
