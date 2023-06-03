package de.rieckpil.courses.book.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookSynchronizationListenerTest {

  private final static String VALID_ISBN = "1234567891234";

  @Mock
  private BookRepository bookRepository;

  @Mock
  private OpenLibraryApiClient openLibraryApiClient;

  @InjectMocks
  private BookSynchronizationListener cut;

  @Captor
  private ArgumentCaptor<Book> bookArgumentCaptor;

  @Test
  void shouldRejectBookWhenIsbnIsMalformed() {
    BookSynchronization bookSynchronization = new BookSynchronization("42");
    cut.consumeBookUpdates(bookSynchronization);

    verifyNoInteractions(openLibraryApiClient, bookRepository);
  }

  @Test
  void shouldNotOverrideWhenBookAlreadyExists() {
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    //Buch ist bereits gespeichert => bookRepository soll ein Buch zurueckgeben
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(new Book());

    cut.consumeBookUpdates(bookSynchronization);

    // openLibraryApiClient wurde nicht verwendet
    verifyNoInteractions(openLibraryApiClient);
    // bookRepository.save wurde nicht aufgerufen
    verify(bookRepository, never()).save(ArgumentMatchers.any());
  }

  @Test
  void shouldThrowExceptionWhenProcessingFails() {
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    //Buch soll noch nicht gespeichert sein => bookRepository soll null zurueckgeben
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);
    //openLibraryApiClient soll Exception schmeissen
    when(openLibraryApiClient.fetchMetadataForBook(VALID_ISBN)).thenThrow(new RuntimeException("network timeout"));

    assertThrows(RuntimeException.class, () -> cut.consumeBookUpdates(bookSynchronization));
  }

  @Test
  void shouldStoreBookWhenNewAndCorrectIsbn() {
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    //Buch soll noch nicht gespeichert sein => bookRepository soll null zurueckgeben
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);
    //when(bookRepository.count()).thenReturn(42L); //failed mit: Unnecessary stubbing detected
    //openLibraryApiClient soll Buch zurueckgeben
    Book requestedBook = new Book();
    String title = "Java Book";
    requestedBook.setTitle(title);
    requestedBook.setIsbn(VALID_ISBN);
    when(openLibraryApiClient.fetchMetadataForBook(VALID_ISBN)).thenReturn(requestedBook);
    //bookRepository soll Buch speichern
    when(bookRepository.save(requestedBook)).then(invocation -> {
      Book methodArg = invocation.getArgument(0);
      methodArg.setId(1L);
      return methodArg;
    });

    cut.consumeBookUpdates(bookSynchronization);

    verify(bookRepository).save(bookArgumentCaptor.capture());

    assertEquals(title, bookArgumentCaptor.getValue().getTitle());
    assertEquals(VALID_ISBN, bookArgumentCaptor.getValue().getIsbn());
  }

}
