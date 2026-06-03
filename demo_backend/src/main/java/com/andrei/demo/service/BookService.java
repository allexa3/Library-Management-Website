package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Book;
import com.andrei.demo.model.BookCreateDTO;
import com.andrei.demo.model.Person;
import com.andrei.demo.repository.BookRepository;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.repository.GenreRepository;
import com.andrei.demo.service.strategy.BorrowingStrategyResolver;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final PersonRepository personRepository;
    private final GenreRepository genreRepository;
    private final BorrowingStrategyResolver borrowingStrategyResolver;

    public List<Book> getAll() { return bookRepository.findAll(); }

    public Book getById(UUID id) throws ValidationException {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Book not found"));
    }

    private void validateBorrowLimit(UUID personId, UUID excludeBookId) throws ValidationException {
        if (personId == null) return;
        long borrowedCount = bookRepository.findAll().stream()
                .filter(b -> b.getBorrowedBy() != null
                        && b.getBorrowedBy().getId().equals(personId)
                        && (excludeBookId == null || !b.getId().equals(excludeBookId)))
                .count();

        if (borrowedCount >= 3) {
            throw new ValidationException("You have reached the maximum limit of 3 borrowed books.");
        }
    }

    public Book create(BookCreateDTO dto) throws ValidationException {
        if (bookRepository.findByIsbn(dto.getIsbn()).isPresent()) {
            throw new ValidationException("A book with this ISBN already exists.");
        }

        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthorName(dto.getAuthorName());
        book.setIsbn(dto.getIsbn());

        if (dto.getPersonId() != null) {
            validateBorrowLimit(dto.getPersonId(), null);
            Person person = personRepository.findById(dto.getPersonId())
                    .orElseThrow(() -> new ValidationException("Person not found"));
            book.setBorrowedBy(person);

            // Dynamic flow calculation
            LocalDate dueDate = borrowingStrategyResolver.getStrategy(person.getRole()).calculateDueDate(LocalDate.now());
            book.setDueDate(dueDate);
        }

        if (dto.getGenreIds() != null && !dto.getGenreIds().isEmpty()) {
            book.setGenres(genreRepository.findAllById(dto.getGenreIds()));
        }

        return bookRepository.save(book);
    }

    public Book borrowBook(UUID bookId, UUID personId) throws ValidationException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ValidationException("Book not found"));

        if (book.getBorrowedBy() != null) {
            throw new ValidationException("This book is already borrowed.");
        }

        validateBorrowLimit(personId, bookId);

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ValidationException("Person not found"));

        book.setBorrowedBy(person);

        // Strategy Design Pattern executed here dynamically
        LocalDate dueDate = borrowingStrategyResolver.getStrategy(person.getRole()).calculateDueDate(LocalDate.now());
        book.setDueDate(dueDate);

        return bookRepository.save(book);
    }

    public Book patch(UUID id, Map<String, Object> updates) throws ValidationException {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Book not found"));

        // Process other fields, but avoid overwriting dueDate if it is explicitly passed
        boolean explicitDueDateProvided = updates.containsKey("dueDate");

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case "title" -> book.setTitle((String) value);
                case "authorName" -> book.setAuthorName((String) value);
                case "isbn" -> book.setIsbn((String) value);
                case "personId" -> {
                    if (value == null) {
                        book.setBorrowedBy(null);
                        if (!explicitDueDateProvided) {
                            book.setDueDate(null);
                        }
                    } else {
                        UUID pId = UUID.fromString(value.toString());
                        validateBorrowLimit(pId, id);
                        Person person = personRepository.findById(pId).orElse(null);
                        boolean borrowerChanged = book.getBorrowedBy() == null
                                || !book.getBorrowedBy().getId().equals(pId);
                        book.setBorrowedBy(person);
                        // Only auto-calculate dynamic strategy if frontend did not supply an explicit override date
                        if (borrowerChanged && person != null && !explicitDueDateProvided) {
                            LocalDate dueDate = borrowingStrategyResolver.getStrategy(person.getRole()).calculateDueDate(LocalDate.now());
                            book.setDueDate(dueDate);
                        }
                    }
                }
                case "genreIds" -> {
                    List<UUID> genreIds = ((List<?>) value).stream()
                            .map(v -> UUID.fromString(v.toString()))
                            .collect(Collectors.toList());
                    book.setGenres(genreRepository.findAllById(genreIds));
                }
                case "dueDate" -> {
                    if (value == null) {
                        book.setDueDate(null);
                    } else {
                        book.setDueDate(java.time.LocalDate.parse(value.toString()));
                    }
                }
            }
        }
        return bookRepository.save(book);
    }

    public void delete(UUID id) { bookRepository.deleteById(id); }

    public Book update(UUID id, BookCreateDTO dto) throws ValidationException {
        Book book = getById(id);
        if (!book.getIsbn().equals(dto.getIsbn()) &&
                bookRepository.findByIsbn(dto.getIsbn()).isPresent()) {
            throw new ValidationException("A book with this ISBN already exists.");
        }
        book.setTitle(dto.getTitle());
        book.setAuthorName(dto.getAuthorName());
        book.setIsbn(dto.getIsbn());

        if (dto.getPersonId() != null) {
            validateBorrowLimit(dto.getPersonId(), id);
            Person person = personRepository.findById(dto.getPersonId())
                    .orElseThrow(() -> new ValidationException("Person not found"));
            book.setBorrowedBy(person);

            LocalDate dueDate = borrowingStrategyResolver.getStrategy(person.getRole()).calculateDueDate(LocalDate.now());
            book.setDueDate(dueDate);
        } else {
            book.setBorrowedBy(null);
            book.setDueDate(null);
        }

        if (dto.getGenreIds() != null && !dto.getGenreIds().isEmpty()) {
            book.setGenres(genreRepository.findAllById(dto.getGenreIds()));
        }
        return bookRepository.save(book);
    }
}