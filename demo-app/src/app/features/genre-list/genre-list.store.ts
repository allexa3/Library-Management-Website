import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize, Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Genre, CreateGenreDto } from '../../models/genre.model';
import { GenreService } from '../../services/genre.service';
import { NotificationService } from '../../services/notification.service';

@Injectable({ providedIn: 'root' })
export class GenreListStore {
  private readonly genreService = inject(GenreService);
  private readonly notify = inject(NotificationService);
  private readonly pendingRequests = signal(0);

  readonly genres = signal<Genre[]>([]);
  readonly isLoading = computed(() => this.pendingRequests() > 0);

  private beginRequest() {
    this.pendingRequests.update((c) => c + 1);
  }
  private endRequest() {
    this.pendingRequests.update((c) => Math.max(0, c - 1));
  }

  load(): void {
    this.beginRequest();
    const request$ = this.genreService.getAll() as unknown as Observable<Genre[]>;
    request$.pipe(finalize(() => this.endRequest())).subscribe({
      next: (data) => this.genres.set(data),
      error: (err: HttpErrorResponse) => {
        this.notify.parseAndShowError(err, 'Failed to load genres. Please try again.');
      },
    });
  }

  create(dto: CreateGenreDto): void {
    this.beginRequest();
    const request$ = this.genreService.create(dto) as unknown as Observable<Genre>;
    request$.pipe(finalize(() => this.endRequest())).subscribe({
      next: (created: Genre) => {
        this.genres.update((list) => [...list, created]);
        this.notify.success(`Genre "${created.name}" was created successfully.`);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409) {
          this.notify.error(
            `A genre with this name already exists. Please choose a different name.`,
          );
        } else {
          this.notify.parseAndShowError(
            err,
            'Failed to create genre. Please check your input and try again.',
          );
        }
      },
    });
  }

  remove(id: string): void {
    const genre = this.genres().find((g) => g.id === id);
    this.beginRequest();
    const request$ = this.genreService.delete(id) as unknown as Observable<void>;
    request$.pipe(finalize(() => this.endRequest())).subscribe({
      next: () => {
        this.genres.update((list) => list.filter((g) => g.id !== id));
        this.notify.success(`Genre "${genre?.name ?? ''}" was deleted successfully.`);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409 || err.status === 500) {
          this.notify.error(
            `Cannot delete genre "${genre?.name ?? 'this genre'}" because it is still assigned to one or more books. ` +
            `Please remove this genre from all books before deleting it.`,
          );
        } else {
          this.notify.parseAndShowError(err, 'Failed to delete genre. Please try again.');
        }
      },
    });
  }

  update(id: string, dto: CreateGenreDto): void {
    const genre = this.genres().find((g) => g.id === id);
    this.beginRequest();
    const request$ = this.genreService.update(id, dto) as unknown as Observable<Genre>;
    request$.pipe(finalize(() => this.endRequest())).subscribe({
      next: (updated) => {
        this.genres.update((list) => list.map((g) => (g.id === id ? updated : g)));
        this.notify.success(`Genre "${updated.name}" was updated successfully.`);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409) {
          this.notify.error(
            `Cannot rename genre: a genre named "${dto.name}" already exists. Please choose a different name.`,
          );
        } else {
          this.notify.parseAndShowError(
            err,
            `Failed to update genre "${genre?.name ?? ''}". Please try again.`,
          );
        }
      },
    });
  }
}