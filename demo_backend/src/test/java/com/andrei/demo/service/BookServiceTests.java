package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Book;
import com.andrei.demo.model.BookCreateDTO;
import com.andrei.demo.model.Genre;
import com.andrei.demo.model.Person;
import com.andrei.demo.repository.BookRepository;
import com.andrei.demo.repository.GenreRepository;
import com.andrei.demo.repository.PersonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookServiceTests {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private BookService bookService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Person personWithId(UUID id) {
        Person p = new Person();
        p.setId(id);
        return p;
    }

    private Book borrowedBy(Person person) {
        Book b = new Book();
        b.setId(UUID.randomUUID());
        b.setBorrowedBy(person);
        return b;
    }

    // ── getAll ──────────────────────────────────────────────────────────────

    @Test
    void testGetAll_ReturnsList() {
        List<Book> books = List.of(new Book(), new Book());
        when(bookRepository.findAll()).thenReturn(books);

        List<Book> result = bookService.getAll();

        assertEquals(2, result.size());
        verify(bookRepository).findAll();
    }

    @Test
    void testGetAll_EmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());

        assertTrue(bookService.getAll().isEmpty());
    }

    // ── getById ─────────────────────────────────────────────────────────────

    @Test
    void testGetById_Found() throws ValidationException {
        UUID id = UUID.randomUUID();
        Book book = new Book();
        book.setId(id);
        when(bookRepository.findById(id)).thenReturn(Optional.of(book));

        Book result = bookService.getById(id);

        assertEquals(id, result.getId());
    }

    @Test
    void testGetById_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> bookService.getById(id));
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void testCreate_Success_WithBorrowerAndGenres() throws ValidationException {
        UUID personId = UUID.randomUUID();
        UUID genreId = UUID.randomUUID();

        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle("Clean Code");
        dto.setAuthorName("Robert Martin");
        dto.setIsbn("9780132350884");
        dto.setPersonId(personId);
        dto.setGenreIds(List.of(genreId));

        Person person = personWithId(personId);
        Genre genre = new Genre();
        genre.setId(genreId);
        genre.setName("Programming");

        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.findAll()).thenReturn(List.of()); // no existing borrow limit issue
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(genreRepository.findAllById(List.of(genreId))).thenReturn(List.of(genre));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.create(dto);

        assertEquals("Clean Code", result.getTitle());
        assertEquals("Robert Martin", result.getAuthorName());
        assertEquals("9780132350884", result.getIsbn());
        assertEquals(person, result.getBorrowedBy());
        assertEquals(1, result.getGenres().size());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testCreate_Success_NoBorrower() throws ValidationException {
        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle("Clean Code");
        dto.setAuthorName("Robert Martin");
        dto.setIsbn("9780132350884");
        // no personId set

        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.create(dto);

        assertNull(result.getBorrowedBy());
        verify(personRepository, never()).findById(any());
    }

    @Test
    void testCreate_DuplicateIsbn_ThrowsValidationException() {
        BookCreateDTO dto = new BookCreateDTO();
        dto.setIsbn("9780132350884");

        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.of(new Book()));

        assertThrows(ValidationException.class, () -> bookService.create(dto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testCreate_BorrowLimitExceeded_ThrowsValidationException() {
        UUID personId = UUID.randomUUID();
        Person person = personWithId(personId);

        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle("The Fourth Book");
        dto.setAuthorName("Author");
        dto.setIsbn("9780132350889");
        dto.setPersonId(personId);

        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
        // Person already has 3 books borrowed
        when(bookRepository.findAll()).thenReturn(List.of(
                borrowedBy(person), borrowedBy(person), borrowedBy(person)
        ));

        assertThrows(ValidationException.class, () -> bookService.create(dto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testCreate_PersonNotFound_ThrowsValidationException() {
        UUID personId = UUID.randomUUID();

        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle("Test Book");
        dto.setAuthorName("Author");
        dto.setIsbn("9780132350884");
        dto.setPersonId(personId);

        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.findAll()).thenReturn(List.of()); // borrow limit not exceeded
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> bookService.create(dto));
        verify(bookRepository, never()).save(any());
    }

    // ── borrowBook ──────────────────────────────────────────────────────────

    @Test
    void testBorrowBook_Success() throws ValidationException {
        UUID bookId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        Book book = new Book();
        book.setId(bookId);
        book.setBorrowedBy(null); // available

        Person person = personWithId(personId);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.findAll()).thenReturn(List.of()); // no existing borrows
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.borrowBook(bookId, personId);

        assertEquals(person, result.getBorrowedBy());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testBorrowBook_AlreadyBorrowed_ThrowsValidationException() {
        UUID bookId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        Person otherPerson = personWithId(UUID.randomUUID());
        Book book = new Book();
        book.setId(bookId);
        book.setBorrowedBy(otherPerson); // already borrowed

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThrows(ValidationException.class, () -> bookService.borrowBook(bookId, personId));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testBorrowBook_LimitExceeded_ThrowsValidationException() {
        UUID bookId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        Person person = personWithId(personId);

        Book target = new Book();
        target.setId(bookId);
        target.setBorrowedBy(null);

        // 3 other books already borrowed by this person (different IDs)
        List<Book> existing = List.of(
                borrowedBy(person), borrowedBy(person), borrowedBy(person)
        );

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(target));
        when(bookRepository.findAll()).thenReturn(existing);

        assertThrows(ValidationException.class, () -> bookService.borrowBook(bookId, personId));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testBorrowBook_NotFound_ThrowsValidationException() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> bookService.borrowBook(bookId, UUID.randomUUID()));
    }

    @Test
    void testBorrowBook_PersonNotFound_ThrowsValidationException() {
        UUID bookId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        Book book = new Book();
        book.setId(bookId);
        book.setBorrowedBy(null);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.findAll()).thenReturn(List.of());
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> bookService.borrowBook(bookId, personId));
        verify(bookRepository, never()).save(any());
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    void testDelete_CallsRepository() {
        UUID id = UUID.randomUUID();
        doNothing().when(bookRepository).deleteById(id);

        bookService.delete(id);

        verify(bookRepository).deleteById(id);
    }

    // ── patch ───────────────────────────────────────────────────────────────

    @Test
    void testPatch_UpdatesTitle() throws ValidationException {
        UUID id = UUID.randomUUID();
        Book book = new Book();
        book.setId(id);
        book.setTitle("Old Title");

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.patch(id, Map.of("title", "New Title"));

        assertEquals("New Title", result.getTitle());
    }

    @Test
    void testPatch_UpdatesAuthorName() throws ValidationException {
        UUID id = UUID.randomUUID();
        Book book = new Book();
        book.setId(id);
        book.setAuthorName("Old Author");

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.patch(id, Map.of("authorName", "New Author"));

        assertEquals("New Author", result.getAuthorName());
    }

    @Test
    void testPatch_UpdatesGenres() throws ValidationException {
        UUID id = UUID.randomUUID();
        UUID genreId = UUID.randomUUID();
        Book book = new Book();
        book.setId(id);

        Genre genre = new Genre();
        genre.setId(genreId);
        genre.setName("Fiction");

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));
        when(genreRepository.findAllById(List.of(genreId))).thenReturn(List.of(genre));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.patch(id, Map.of("genreIds", List.of(genreId.toString())));

        assertEquals(1, result.getGenres().size());
        assertEquals("Fiction", result.getGenres().get(0).getName());
    }

    @Test
    void testPatch_ClearsBorrower_WhenPersonIdIsNull() throws ValidationException {
        UUID id = UUID.randomUUID();
        Book book = new Book();
        book.setId(id);
        book.setBorrowedBy(personWithId(UUID.randomUUID()));

        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("personId", null);

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.patch(id, updates);

        assertNull(result.getBorrowedBy());
    }

    @Test
    void testPatch_SetsBorrower_WhenPersonIdProvided() throws ValidationException {
        UUID id = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        Person person = personWithId(personId);

        Book book = new Book();
        book.setId(id);

        when(bookRepository.findById(id)).thenReturn(Optional.of(book));
        when(bookRepository.findAll()).thenReturn(List.of());
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.patch(id, Map.of("personId", personId.toString()));

        assertEquals(person, result.getBorrowedBy());
    }

    @Test
    void testPatch_BookNotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> bookService.patch(id, Map.of("title", "X")));
    }

    // ── update ──────────────────────────────────────────────────────────────

    @Test
    void testUpdate_Success() throws ValidationException {
        UUID id = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        Person person = personWithId(personId);

        Book existing = new Book();
        existing.setId(id);
        existing.setIsbn("9780132350884");

        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle("Updated Title");
        dto.setAuthorName("New Author");
        dto.setIsbn("9780132350884"); // same ISBN — no conflict
        dto.setPersonId(personId);

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.findAll()).thenReturn(List.of());
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.update(id, dto);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("New Author", result.getAuthorName());
        assertEquals(person, result.getBorrowedBy());
    }

    @Test
    void testUpdate_ClearsBorrower_WhenNoBorrowerInDto() throws ValidationException {
        UUID id = UUID.randomUUID();

        Book existing = new Book();
        existing.setId(id);
        existing.setIsbn("9780132350884");
        existing.setBorrowedBy(personWithId(UUID.randomUUID()));

        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle("Title");
        dto.setAuthorName("Author");
        dto.setIsbn("9780132350884");
        // personId is null

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.update(id, dto);

        assertNull(result.getBorrowedBy());
    }

    @Test
    void testUpdate_NewIsbnAlreadyExists_ThrowsValidationException() {
        UUID id = UUID.randomUUID();

        Book existing = new Book();
        existing.setId(id);
        existing.setIsbn("9780000000001");

        Book conflict = new Book();
        conflict.setId(UUID.randomUUID()); // different book owns the new ISBN

        BookCreateDTO dto = new BookCreateDTO();
        dto.setIsbn("9780000000002");

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.of(conflict));

        assertThrows(ValidationException.class, () -> bookService.update(id, dto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testUpdate_BorrowLimitExceeded_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        Person person = personWithId(personId);

        Book existing = new Book();
        existing.setId(id);
        existing.setIsbn("9780132350884");

        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle("Updated");
        dto.setAuthorName("Author");
        dto.setIsbn("9780132350884");
        dto.setPersonId(personId);

        // 3 other books (different IDs) already borrowed by this person
        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.findAll()).thenReturn(List.of(
                borrowedBy(person), borrowedBy(person), borrowedBy(person)
        ));

        assertThrows(ValidationException.class, () -> bookService.update(id, dto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testUpdate_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> bookService.update(id, new BookCreateDTO()));
    }
}