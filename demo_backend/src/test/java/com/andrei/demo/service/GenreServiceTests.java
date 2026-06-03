package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Genre;
import com.andrei.demo.model.GenreCreateDTO;
import com.andrei.demo.repository.GenreRepository;
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

class GenreServiceTests {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreService genreService;

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

    private Genre genreWithId(UUID id, String name) {
        Genre g = new Genre();
        g.setId(id);
        g.setName(name);
        return g;
    }

    // ── getAll ──────────────────────────────────────────────────────────────

    @Test
    void testGetAll_ReturnsList() {
        List<Genre> genres = List.of(new Genre(), new Genre(), new Genre());
        when(genreRepository.findAll()).thenReturn(genres);

        List<Genre> result = genreService.getAll();

        assertEquals(3, result.size());
        verify(genreRepository).findAll();
    }

    @Test
    void testGetAll_EmptyList() {
        when(genreRepository.findAll()).thenReturn(List.of());

        assertTrue(genreService.getAll().isEmpty());
    }

    // ── getById ─────────────────────────────────────────────────────────────

    @Test
    void testGetById_Found() throws ValidationException {
        UUID id = UUID.randomUUID();
        Genre genre = genreWithId(id, "Fiction");
        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));

        Genre result = genreService.getById(id);

        assertEquals(id, result.getId());
        assertEquals("Fiction", result.getName());
    }

    @Test
    void testGetById_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(genreRepository.findById(id)).thenReturn(Optional.empty());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> genreService.getById(id));
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void testCreate_Success() throws ValidationException {
        GenreCreateDTO dto = new GenreCreateDTO();
        dto.setName("Science Fiction");

        when(genreRepository.findByName("Science Fiction")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> {
            Genre g = inv.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });

        Genre result = genreService.create(dto);

        assertNotNull(result.getId());
        assertEquals("Science Fiction", result.getName());
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void testCreate_DuplicateName_ThrowsValidationException() {
        GenreCreateDTO dto = new GenreCreateDTO();
        dto.setName("Fiction");

        when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(new Genre()));

        assertThrows(ValidationException.class, () -> genreService.create(dto));
        verify(genreRepository, never()).save(any());
    }

    // ── update ──────────────────────────────────────────────────────────────

    @Test
    void testUpdate_Success() throws ValidationException {
        UUID id = UUID.randomUUID();
        Genre existing = genreWithId(id, "OldName");

        GenreCreateDTO dto = new GenreCreateDTO();
        dto.setName("NewName");

        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.findByName("NewName")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        Genre result = genreService.update(id, dto);

        assertEquals("NewName", result.getName());
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void testUpdate_SameNameSameId_NoConflict() throws ValidationException {
        UUID id = UUID.randomUUID();
        Genre existing = genreWithId(id, "Fiction");

        GenreCreateDTO dto = new GenreCreateDTO();
        dto.setName("Fiction");

        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        // findByName returns the same genre (same ID) — no conflict
        when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(existing));
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> genreService.update(id, dto));
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void testUpdate_NameTakenByOther_ThrowsRuntimeException() {
        UUID id = UUID.randomUUID();
        Genre existing = genreWithId(id, "OldName");
        Genre conflicting = genreWithId(UUID.randomUUID(), "Fiction");

        GenreCreateDTO dto = new GenreCreateDTO();
        dto.setName("Fiction");

        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(conflicting));

        assertThrows(RuntimeException.class, () -> genreService.update(id, dto));
        verify(genreRepository, never()).save(any());
    }

    @Test
    void testUpdate_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(genreRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> genreService.update(id, new GenreCreateDTO()));
    }

    // ── patch ───────────────────────────────────────────────────────────────

    @Test
    void testPatch_UpdatesName() throws ValidationException {
        UUID id = UUID.randomUUID();
        Genre genre = genreWithId(id, "OldName");

        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));
        when(genreRepository.findByName("NewName")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        Genre result = genreService.patch(id, Map.of("name", "NewName"));

        assertEquals("NewName", result.getName());
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void testPatch_SameNameSameId_NoConflict() throws ValidationException {
        UUID id = UUID.randomUUID();
        Genre genre = genreWithId(id, "Fiction");

        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));
        when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(genre));
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> genreService.patch(id, Map.of("name", "Fiction")));
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void testPatch_NameConflict_ThrowsRuntimeException() {
        UUID id = UUID.randomUUID();
        Genre genre = genreWithId(id, "OldName");
        Genre conflicting = genreWithId(UUID.randomUUID(), "Fiction");

        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));
        when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(conflicting));

        assertThrows(RuntimeException.class,
                () -> genreService.patch(id, Map.of("name", "Fiction")));
        verify(genreRepository, never()).save(any());
    }

    @Test
    void testPatch_NoNameKey_SavesUnchanged() throws ValidationException {
        UUID id = UUID.randomUUID();
        Genre genre = genreWithId(id, "Unchanged");

        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        // Patch with an unrecognised key — name should be untouched
        Genre result = genreService.patch(id, Map.of());

        assertEquals("Unchanged", result.getName());
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void testPatch_NotFound_ThrowsValidationException() {
        UUID id = UUID.randomUUID();
        when(genreRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> genreService.patch(id, Map.of("name", "X")));
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    void testDelete_CallsRepository() {
        UUID id = UUID.randomUUID();
        doNothing().when(genreRepository).deleteById(id);

        genreService.delete(id);

        verify(genreRepository).deleteById(id);
    }
}