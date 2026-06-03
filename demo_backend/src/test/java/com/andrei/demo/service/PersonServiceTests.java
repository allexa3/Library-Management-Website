package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.PersonCreateDTO;
import com.andrei.demo.model.PersonUpdateDTO;
import com.andrei.demo.model.UserRole;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.PasswordUtil;
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

class PersonServiceTests {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private PersonService personService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // ── getPeople ───────────────────────────────────────────────────────────

    @Test
    void testGetPeople_ReturnsList() {
        List<Person> people = List.of(new Person(), new Person());
        when(personRepository.findAll()).thenReturn(people);

        List<Person> result = personService.getPeople();

        assertEquals(2, result.size());
        assertEquals(people, result);
        verify(personRepository).findAll();
    }

    @Test
    void testGetPeople_EmptyList() {
        when(personRepository.findAll()).thenReturn(List.of());

        List<Person> result = personService.getPeople();

        assertTrue(result.isEmpty());
        verify(personRepository).findAll();
    }

    // ── getPersonById ───────────────────────────────────────────────────────

    @Test
    void testGetPersonById_Found() {
        UUID id = UUID.randomUUID();
        Person person = new Person();
        person.setId(id);
        when(personRepository.findById(id)).thenReturn(Optional.of(person));

        Person result = personService.getPersonById(id);

        assertEquals(id, result.getId());
        verify(personRepository).findById(id);
    }

    @Test
    void testGetPersonById_NotFound_ThrowsIllegalStateException() {
        UUID id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> personService.getPersonById(id));
    }

    // ── getPersonByEmail ────────────────────────────────────────────────────

    @Test
    void testGetPersonByEmail_Found() {
        String email = "john@example.com";
        Person person = new Person();
        person.setEmail(email);
        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));

        Person result = personService.getPersonByEmail(email);

        assertEquals(email, result.getEmail());
        verify(personRepository).findByEmail(email);
    }

    @Test
    void testGetPersonByEmail_NotFound_ThrowsIllegalStateException() {
        String email = "ghost@example.com";
        when(personRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> personService.getPersonByEmail(email));
    }

    // ── addPerson ───────────────────────────────────────────────────────────

    @Test
    void testAddPerson_Success() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("John");
        dto.setPassword("Password@1");
        dto.setAge(30);
        dto.setEmail("john@example.com");
        dto.setRole(UserRole.CUSTOMER);

        when(personRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(passwordUtil.hashPassword(dto.getPassword())).thenReturn("hashed-password");
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        Person result = personService.addPerson(dto);

        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("hashed-password", result.getPassword());
        assertEquals(UserRole.CUSTOMER, result.getRole());
        verify(passwordUtil).hashPassword(dto.getPassword());
        verify(personRepository).save(any(Person.class));
    }

    @Test
    void testAddPerson_DuplicateEmail_ThrowsValidationException() {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setEmail("taken@example.com");
        dto.setPassword("Password@1");
        dto.setName("Bob");
        dto.setAge(25);

        when(personRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new Person()));

        assertThrows(ValidationException.class, () -> personService.addPerson(dto));
        verify(personRepository, never()).save(any());
        verifyNoInteractions(passwordUtil);
    }

    // ── updatePerson ────────────────────────────────────────────────────────

    @Test
    void testUpdatePerson_Success() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(uuid);
        existing.setName("John");
        existing.setAge(30);
        existing.setEmail("john@example.com");
        existing.setRole(UserRole.CUSTOMER);

        PersonUpdateDTO dto = new PersonUpdateDTO();
        dto.setName("Jane");
        dto.setAge(25);
        dto.setEmail("jane@example.com");
        dto.setRole(UserRole.ADMIN);

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        Person result = personService.updatePerson(uuid, dto);

        assertEquals("Jane", result.getName());
        assertEquals(25, result.getAge());
        assertEquals("jane@example.com", result.getEmail());
        assertEquals(UserRole.ADMIN, result.getRole());
        verify(personRepository).save(any(Person.class));
    }

    @Test
    void testUpdatePerson_NotFound_ThrowsValidationException() {
        UUID uuid = UUID.randomUUID();
        when(personRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> personService.updatePerson(uuid, new PersonUpdateDTO()));
        verify(personRepository, never()).save(any());
    }

    @Test
    void testUpdatePerson_EmailTakenByOther_ThrowsRuntimeException() {
        UUID uuid = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(uuid);
        existing.setEmail("john@example.com");

        Person other = new Person();
        other.setId(UUID.randomUUID()); // different person owns the email

        PersonUpdateDTO dto = new PersonUpdateDTO();
        dto.setName("John");
        dto.setAge(30);
        dto.setEmail("taken@example.com");
        dto.setRole(UserRole.CUSTOMER);

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        assertThrows(RuntimeException.class, () -> personService.updatePerson(uuid, dto));
        verify(personRepository, never()).save(any());
    }

    @Test
    void testUpdatePerson_SameEmailSameId_NoConflict() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(uuid);
        existing.setEmail("john@example.com");
        existing.setName("John");
        existing.setAge(30);
        existing.setRole(UserRole.CUSTOMER);

        PersonUpdateDTO dto = new PersonUpdateDTO();
        dto.setName("John Updated");
        dto.setAge(31);
        dto.setEmail("john@example.com"); // same email, same person
        dto.setRole(UserRole.CUSTOMER);

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        // findByEmail returns the same person (same ID) — no conflict
        when(personRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existing));
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> personService.updatePerson(uuid, dto));
        verify(personRepository).save(any(Person.class));
    }

    // ── deletePerson ────────────────────────────────────────────────────────

    @Test
    void testDeletePerson_CallsRepository() {
        UUID uuid = UUID.randomUUID();
        doNothing().when(personRepository).deleteById(uuid);

        personService.deletePerson(uuid);

        verify(personRepository).deleteById(uuid);
    }

    // ── patchPerson ─────────────────────────────────────────────────────────

    @Test
    void testPatchPerson_UpdatesName() throws ValidationException {
        UUID uuid = UUID.randomUUID();
        Person person = new Person();
        person.setId(uuid);
        person.setName("Old Name");

        when(personRepository.findById(uuid)).thenReturn(Optional.of(person));
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        Person result = personService.patchPerson(uuid, Map.of("name", "New Name"));

        assertEquals("New Name", result.getName());
        verify(personRepository).save(any(Person.class));
    }

    @Test
    void testPatchPerson_UpdatesAge() throws ValidationException {
        UUID uuid = UUID.randomUUID();
        Person person = new Person();
        person.setId(uuid);
        person.setAge(25);

        when(personRepository.findById(uuid)).thenReturn(Optional.of(person));
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        Person result = personService.patchPerson(uuid, Map.of("age", 35));

        assertEquals(35, result.getAge());
    }

    @Test
    void testPatchPerson_UpdatesEmail_NoConflict() throws ValidationException {
        UUID uuid = UUID.randomUUID();
        Person person = new Person();
        person.setId(uuid);
        person.setEmail("old@example.com");

        when(personRepository.findById(uuid)).thenReturn(Optional.of(person));
        when(personRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        Person result = personService.patchPerson(uuid, Map.of("email", "new@example.com"));

        assertEquals("new@example.com", result.getEmail());
    }

    @Test
    void testPatchPerson_EmailConflict_ThrowsRuntimeException() {
        UUID uuid = UUID.randomUUID();
        Person person = new Person();
        person.setId(uuid);
        person.setEmail("old@example.com");

        Person other = new Person();
        other.setId(UUID.randomUUID());

        when(personRepository.findById(uuid)).thenReturn(Optional.of(person));
        when(personRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        assertThrows(RuntimeException.class,
                () -> personService.patchPerson(uuid, Map.of("email", "taken@example.com")));
        verify(personRepository, never()).save(any());
    }

    @Test
    void testPatchPerson_UpdatesRole() throws ValidationException {
        UUID uuid = UUID.randomUUID();
        Person person = new Person();
        person.setId(uuid);
        person.setRole(UserRole.CUSTOMER);

        when(personRepository.findById(uuid)).thenReturn(Optional.of(person));
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        Person result = personService.patchPerson(uuid, Map.of("role", "ADMIN"));

        assertEquals(UserRole.ADMIN, result.getRole());
    }

    @Test
    void testPatchPerson_NotFound_ThrowsValidationException() {
        UUID uuid = UUID.randomUUID();
        when(personRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> personService.patchPerson(uuid, Map.of("name", "X")));
    }
}