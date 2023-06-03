package de.rieckpil.courses.book.management;

import de.rieckpil.courses.config.WebSecurityConfig;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import(WebSecurityConfig.class)
class BookControllerTest {

  @MockBean
  private BookManagementService bookManagementService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldGetEmptyArrayWhenNoBooksExists() throws Exception {
    MvcResult mvcResult = this.mockMvc
      .perform(
        get("/api/books")
          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
      )
      .andExpect( status().is(200))
      .andExpect( jsonPath("$.size()", Matchers.is(0)))
      .andDo(MockMvcResultHandlers.print())
      .andReturn();
  }

  @Test
  void shouldNotReturnXML() throws Exception {
    this.mockMvc
      .perform(
        get("/api/books")
          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
      )
      .andExpect( status().isNotAcceptable())
    ;
  }

  @Test
  void shouldGetBooksWhenServiceReturnsBooks() throws Exception {
    when(bookManagementService.getAllBooks())
      .thenReturn(List.of(
        createBook(1L, "42", "Java 17", "BUndy", "Good Book", "IT", 200L, "packt", "ftp://localhost:furzi"),
        createBook(2L, "42", "Java 23", "BUndy", "Good Book", "IT", 200L, "packt", "ftp://localhost:furzi")
      ));

    this.mockMvc
      .perform(get("/api/books"))
      .andExpect(status().is(200))
      .andExpect(jsonPath("$.size()", is(2)))
      .andExpect(jsonPath("$[0].id").doesNotExist())
      .andExpect(jsonPath("$[0].title", is("Java 17")))
      .andExpect(jsonPath("$[1].title", is("Java 23")))
      ;
  }

  private Book createBook(Long id, String isbn, String title, String author, String description, String genre, Long pages, String publisher, String thumbnailUrl) {
    Book result = new Book();
    result.setId(id);
    result.setIsbn(isbn);
    result.setTitle(title);
    result.setAuthor(author);
    result.setDescription(description);
    result.setGenre(genre);
    result.setPages(pages);
    result.setPublisher(publisher);
    result.setThumbnailUrl(thumbnailUrl);
    return result;
  }

}
