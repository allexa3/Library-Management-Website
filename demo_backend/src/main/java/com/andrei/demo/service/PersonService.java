package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.PersonCreateDTO;
import com.andrei.demo.model.PersonUpdateDTO;
import com.andrei.demo.model.UserRole;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.PasswordUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;
    private final PasswordUtil passwordUtil;

    public List<Person> getPeople() {
        return personRepository.findAll();
    }

    public Person addPerson(PersonCreateDTO personDTO) throws ValidationException {
        if (personRepository.findByEmail(personDTO.getEmail()).isPresent()) {
            throw new ValidationException("Email " + personDTO.getEmail() + " is already in use.");
        }
        Person person = new Person();
        person.setName(personDTO.getName());
        person.setAge(personDTO.getAge());
        person.setEmail(personDTO.getEmail());
        person.setRole(personDTO.getRole());
        person.setPassword(passwordUtil.hashPassword(personDTO.getPassword()));
        return personRepository.save(person);
    }

    public Person updatePerson(UUID uuid, PersonUpdateDTO dto) throws ValidationException {
        Person existingPerson = personRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException("Person with id " + uuid + " not found"));

        personRepository.findByEmail(dto.getEmail()).ifPresent(p -> {
            if (!p.getId().equals(uuid)) {
                throw new RuntimeException("Email " + dto.getEmail() + " is already in use by another account.");
            }
        });

        existingPerson.setName(dto.getName());
        existingPerson.setAge(dto.getAge());
        existingPerson.setEmail(dto.getEmail());
        existingPerson.setRole(dto.getRole());
        return personRepository.save(existingPerson);
    }

    public void deletePerson(UUID uuid) {
        personRepository.deleteById(uuid);
    }

    public Person getPersonByEmail(String email) {
        return personRepository.findByEmail(email).orElseThrow(
                () -> new IllegalStateException("Person with email " + email + " not found"));
    }

    public Person getPersonById(UUID uuid) {
        return personRepository.findById(uuid).orElseThrow(
                () -> new IllegalStateException("Person with id " + uuid + " not found"));
    }

    public Person patchPerson(UUID uuid, Map<String, Object> updates) throws ValidationException {
        Person existingPerson = personRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException("Person with id " + uuid + " not found"));

        updates.forEach((key, value) -> {
            switch (key) {
                case "name" -> existingPerson.setName((String) value);
                case "age" -> existingPerson.setAge((Integer) value);
                case "email" -> {
                    String newEmail = (String) value;
                    if (!newEmail.equals(existingPerson.getEmail()) &&
                            personRepository.findByEmail(newEmail).isPresent()) {
                        throw new RuntimeException("Email " + newEmail + " is already in use.");
                    }
                    existingPerson.setEmail(newEmail);
                }
                case "role" -> existingPerson.setRole(UserRole.valueOf((String) value));
            }
        });

        return personRepository.save(existingPerson);
    }
}