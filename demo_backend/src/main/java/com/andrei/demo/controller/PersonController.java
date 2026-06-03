package com.andrei.demo.controller;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.PersonCreateDTO;
import com.andrei.demo.model.PersonUpdateDTO;
import com.andrei.demo.service.PersonService;
import com.andrei.demo.model.Person;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
public class PersonController {
    private final PersonService personService;

    @GetMapping("/person")
    public List<Person> getPeople() {
        return personService.getPeople();
    }

    @GetMapping("/person/{uuid}")
    public Person getPersonById(@PathVariable UUID uuid) {
        return personService.getPersonById(uuid);
    }

    @GetMapping("/person/email/{email}")
    public Person getPersonByEmail(@PathVariable String email) {
        return personService.getPersonByEmail(email);
    }

    @PostMapping("/person")
    public Person addPerson(@Valid @RequestBody PersonCreateDTO personDTO)
            throws ValidationException {
        return personService.addPerson(personDTO);
    }

    @PutMapping("/person/{uuid}")
    public Person updatePerson(@PathVariable UUID uuid,
                               @Valid @RequestBody PersonUpdateDTO dto)
            throws ValidationException {
        return personService.updatePerson(uuid, dto);
    }

    @DeleteMapping("/person/{uuid}")
    public void deletePerson(@PathVariable UUID uuid) {
        personService.deletePerson(uuid);
    }

    @PatchMapping("/person/{uuid}")
    public Person patchPerson(@PathVariable UUID uuid, @RequestBody Map<String, Object> updates)
            throws ValidationException {
        return personService.patchPerson(uuid, updates);
    }
}