import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Book, CreateBookDto } from '../../models/book.model';
import { BookService } from '../../services/book.service';
import { NotificationService } from '../../services/notification.service';

@Injectable({ providedIn: 'root' })
export class BookListStore {
  private readonly bookService = inject(BookService);
  private readonly notify = inject(NotificationService);
  private readonly pendingRequests = signal(0);

  readonly books = signal<Book[]>([]);
  readonly hasError = signal<string | null>(null);
  readonly isLoading = computed(() => this.pendingRequests() > 0);

  private begin(): void { this.pendingRequests.update(c => c + 1); }
  private end(): void   { this.pendingRequests.update(c => Math.max(0, c - 1)); }

  load(): void {
    this.hasError.set(null);
    this.begin();
    this.bookService.getAll()
      .pipe(finalize(() => this.end()))
      .subscribe({
        next:  (data) => this.books.set(data),
        error: (err: HttpErrorResponse) => {
          this.hasError.set(err.error?.error ?? err.error?.message ?? 'Failed to load books.');
          this.notify.parseAndShowError(err, 'Failed to load books.');
        },
      });
  }

  create(dto: CreateBookDto): void {
    this.hasError.set(null);
    this.begin();
    this.bookService.create(dto)
      .pipe(finalize(() => this.end()))
      .subscribe({
        next:  (newBook) => {
          this.books.update(list => [...list, newBook]);
          this.notify.success(`"${newBook.title}" was added to the catalog.`);
        },
        error: (err: HttpErrorResponse) => {
          this.hasError.set(err.error?.error ?? err.error?.message ?? 'Failed to create book.');
          this.notify.parseAndShowError(err, 'Failed to create book.');
        },
      });
  }

  // id + partial updates (title, authorName, isbn, personId, genreIds, dueDate)
  update(id: string, updates: Partial<CreateBookDto> & { dueDate?: string | null; personId?: string | null }): void {
    this.hasError.set(null);
    this.begin();
    this.bookService.patch(id, updates as any)
      .pipe(finalize(() => this.end()))
      .subscribe({
        next:  (updated) => {
          this.books.update(list => list.map(b => b.id === id ? updated : b));
          this.notify.success(`"${updated.title}" was updated.`);
        },
        error: (err: HttpErrorResponse) => {
          this.hasError.set(err.error?.error ?? err.error?.message ?? 'Failed to update book.');
          this.notify.parseAndShowError(err, 'Failed to update book.');
        },
      });
  }

  borrow(bookId: string): void {
    this.hasError.set(null);
    this.begin();
    this.bookService.borrow(bookId)
      .pipe(finalize(() => this.end()))
      .subscribe({
        next:  (updated) => {
          this.books.update(list => list.map(b => b.id === bookId ? updated : b));
          this.notify.success(`You borrowed "${updated.title}". Due: ${updated.dueDate ?? 'N/A'}.`);
        },
        error: (err: HttpErrorResponse) => {
          const msg = err.error?.error ?? err.error?.message ?? 'Borrow limit reached or book already borrowed.';
          this.hasError.set(msg);
          this.notify.error(msg);
        },
      });
  }

  // Return a borrowed book — PATCH personId: null clears borrowedBy + dueDate on backend
  returnBook(bookId: string): void {
    this.hasError.set(null);
    this.begin();
    this.bookService.patch(bookId, { personId: null } as any)
      .pipe(finalize(() => this.end()))
      .subscribe({
        next:  (updated) => {
          this.books.update(list => list.map(b => b.id === bookId ? updated : b));
          this.notify.success(`"${updated.title}" has been returned.`);
        },
        error: (err: HttpErrorResponse) => {
          const msg = err.error?.error ?? err.error?.message ?? 'Failed to return book.';
          this.hasError.set(msg);
          this.notify.parseAndShowError(err, 'Failed to return book.');
        },
      });
  }

  // Admin: override the due date
  // Requires backend BookService.patch() to handle "dueDate" key:
  //   case "dueDate" -> book.setDueDate(value == null ? null : LocalDate.parse(value.toString()));
  patchDueDate(bookId: string, dueDate: string): void {
    // Optimistic update — show the new date immediately before the HTTP round-trip.
    const prevBooks = this.books();
    this.books.update(list => list.map(b => b.id === bookId ? { ...b, dueDate } : b));

    this.hasError.set(null);
    this.begin();
    this.bookService.patch(bookId, { dueDate } as any)
      .pipe(finalize(() => this.end()))
      .subscribe({
        next:  (updated) => {
          this.books.update(list => list.map(b => b.id === bookId ? updated : b));
          this.notify.success('Due date updated.');
        },
        error: (err: HttpErrorResponse) => {
          this.books.set(prevBooks); // revert optimistic update on failure
          const msg = err.error?.error ?? err.error?.message ?? 'Failed to update due date.';
          this.hasError.set(msg);
          this.notify.parseAndShowError(err, 'Failed to update due date.');
        },
      });
  }

  remove(id: string): void {
    const book = this.books().find(b => b.id === id);
    this.hasError.set(null);
    this.begin();
    this.bookService.delete(id)
      .pipe(finalize(() => this.end()))
      .subscribe({
        next:  () => {
          this.books.update(list => list.filter(b => b.id !== id));
          this.notify.success(`"${book?.title ?? 'Book'}" was deleted.`);
        },
        error: (err: HttpErrorResponse) => {
          const msg = err.error?.error ?? err.error?.message ?? 'Failed to delete book.';
          this.hasError.set(msg);
          this.notify.parseAndShowError(err, 'Failed to delete book.');
        },
      });
  }
}