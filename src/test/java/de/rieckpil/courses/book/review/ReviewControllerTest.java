package de.rieckpil.courses.book.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.rieckpil.courses.config.WebSecurityConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(WebSecurityConfig.class)
class ReviewControllerTest {

  @MockBean
  private ReviewService reviewService;

  @Autowired
  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @BeforeEach
  void beforeEach() {
    this.objectMapper = new ObjectMapper();
  }
  @Test
  @DisplayName("should return twenty reviews without any order when no parameters are specified")
  void shouldReturnTwentyReviewsWithoutAnyOrderWhenNoParametersAreSpecified() throws Exception {
    ArrayNode result =objectMapper.createArrayNode();

    ObjectNode statistics = objectMapper.createObjectNode();
    statistics.put("bookId", 1);
    statistics.put("isbn", "42");
    statistics.put("avg", 89.3);
    statistics.put("ratings", 2);
    result.add(statistics);

    when(reviewService.getAllReviews(20, "none"))
      .thenReturn(result);

    this.mockMvc
      .perform(get("/api/books/reviews"))
      .andDo(MockMvcResultHandlers.print())
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.size()", Matchers.is(1)))
      ;
  }

  @Test
  @DisplayName("when user is not authenticated then no statistics are returned and status is 401")
  void shouldNotReturnReviewStatisticsWhenUserIsUnauthenticated() throws Exception {
    this.mockMvc
      .perform(get("/api/books/reviews/statistics"))
      .andDo(MockMvcResultHandlers.print())
      .andExpect(status().isUnauthorized())
      ;

    // assert getReviewStatistics wurde nicht aufgerufen
    verifyNoInteractions(reviewService);
  }

  @Test
  @DisplayName("when user is authenticated then statistics are returned")
//  @WithMockUser(username = "duke")
  void shouldReturnReviewStatisticsWhenUserIsAuthenticated() throws Exception {
    this.mockMvc
//      .perform(get("/api/books/reviews/statistics"))
//      .perform(get("/api/books/reviews/statistics")
//        .with(user("duke")))
//      .perform(get("/api/books/reviews/statistics")
//        .with(oidcLogin()))
//      .perform(get("/api/books/reviews/statistics")
//        .with(oauth2Login()))
//      .perform(get("/api/books/reviews/statistics")
//        .with(httpBasic("duke", "password")))
      .perform(get("/api/books/reviews/statistics")
        .with(jwt()))
      .andDo(MockMvcResultHandlers.print())
      .andExpect(status().is(200))
      ;

    // assert getReviewStatistics wurde aufgerufen
    verify(reviewService).getReviewStatistics();
  }

  @Test
  void shouldCreateNewBookReviewForAuthenticatedUserWithValidPayload() throws Exception {
    String requestBody = """
      {
        "reviewTitle": "Great Java Book",
        "reviewContent": "I really like this book.",
        "rating": 4
      }
      """;

    when(reviewService.createBookReview(
      eq("42"), any(BookReviewRequest.class), eq("duke"), endsWith("spring.io"))
    )
      .thenReturn(84L)
    ;
    this.mockMvc
      .perform(
        post("/api/books/{isbn}/reviews", 42)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
          .with(jwt().jwt(builder -> builder
            .claim("email", "duke@spring.io")
            .claim("preferred_username", "duke")))
      )
      .andExpect(status().isCreated())
      .andExpect(header().exists("Location"))
      .andExpect(header().string("Location", Matchers.containsString("/books/42/reviews/84")))
      ;
  }

  @Test
  void shouldRejectNewBookReviewForAuthenticatedUsersWithInvalidPayload() throws Exception {

    String requestBody = """
      {
        "reviewTitle": "Great Java Book",
        "reviewContent": "I really like this book.",
        "rating": -1
      }
      """;

    this.mockMvc
      .perform(
        post("/api/books/{isbn}/reviews", 42)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
          .with(jwt().jwt(builder -> builder
            .claim("email", "duke@spring.io")
            .claim("preferred_username", "duke")))
      )
      .andDo(MockMvcResultHandlers.print())
      .andExpect(status().isBadRequest())
    ;
  }

  @Test
  void shouldNotAllowDeletingReviewsWhenUserIsAuthenticatedWithoutModeratorRole() throws Exception {
    this.mockMvc
      .perform(delete("/api/books/{isbn}/reviews/{reviewId}", 42, 3)
        .with(jwt())
      )
      .andExpect(status().isForbidden())
      ;

    verifyNoInteractions(reviewService);
  }

  @Test
  //@WithMockUser(roles = "moderator")
  void shouldAllowDeletingReviewsWhenUserIsAuthenticatedAndHasModeratorRole() throws Exception {
    this.mockMvc
      .perform(delete("/api/books/{isbn}/reviews/{reviewId}", 42, 3)
        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_moderator")))
      )
      .andExpect(status().isOk())
    ;

    verify(reviewService).deleteReview("42", 3L);
  }
}
