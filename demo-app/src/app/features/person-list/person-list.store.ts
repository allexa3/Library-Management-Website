import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { CreatePersonDto, Person, UpdatePersonDto } from '../../models/person.model';
import { PersonService } from '../../services/person.service';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from '../../services/notification.service';

@Injectable({ providedIn: 'root' })
export class PersonListStore {
  private readonly personService = inject(PersonService);
  private readonly notify = inject(NotificationService);
  private readonly pendingRequests = signal(0);

  readonly persons = signal<Person[]>([]);
  readonly hasError = signal(false);
  readonly isLoading = computed(() => this.pendingRequests() > 0);

  private beginRequest(): void {
    this.pendingRequests.update((count) => count + 1);
  }

  private endRequest(): void {
    this.pendingRequests.update((count) => Math.max(0, count - 1));
  }

  load(): void {
    this.hasError.set(false);
    this.beginRequest();
    this.personService
      .getAll()
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (data) => this.persons.set(data),
        error: (err: HttpErrorResponse) => {
          this.hasError.set(true);
          this.notify.parseAndShowError(err, 'Failed to load people. Please try again.');
        },
      });
  }

  create(dto: CreatePersonDto): void {
    this.hasError.set(false);
    this.beginRequest();
    this.personService
      .create(dto)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (created) => {
          this.persons.update((list) => [...list, created]);
          this.notify.success(`Person "${created.name}" was created successfully.`);
        },
        error: (err: HttpErrorResponse) => {
          this.hasError.set(true);
          this.notify.parseAndShowError(
            err,
            'Failed to create person. Please check your input and try again.',
          );
        },
      });
  }

  update(id: string, dto: UpdatePersonDto): void {
    const existing = this.persons().find((p) => p.id === id);
    if (!existing) return;

    const payload: CreatePersonDto = { ...dto, password: existing.password };

    this.hasError.set(false);
    this.beginRequest();
    this.personService
      .update(id, payload)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (updated) => {
          this.persons.update((list) =>
            list.map((person) => (person.id === updated.id ? updated : person)),
          );
          this.notify.success(`Person "${updated.name}" was updated successfully.`);
        },
        error: (err: HttpErrorResponse) => {
          this.hasError.set(true);
          this.notify.parseAndShowError(
            err,
            'Failed to update person. The email may already be in use.',
          );
        },
      });
  }

  remove(id: string): void {
    const person = this.persons().find((p) => p.id === id);
    this.hasError.set(false);
    this.beginRequest();
    this.personService
      .delete(id)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: () => {
          this.persons.update((list) => list.filter((p) => p.id !== id));
          this.notify.success(`Person "${person?.name ?? ''}" was deleted successfully.`);
        },
        error: (err: HttpErrorResponse) => {
          this.hasError.set(true);
          if (err.status === 409 || err.status === 500) {
            this.notify.error(
              `Cannot delete "${person?.name ?? 'this person'}" because they still have books assigned to them. ` +
              `Please reassign or delete those books first.`,
            );
          } else {
            this.notify.parseAndShowError(err, 'Failed to delete person. Please try again.');
          }
        },
      });
  }
}
