import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import {
  BookFormDialogComponent,
  BookFormDialogData,
  BookFormDialogResult,
} from '../../components/book-form-dialog/book-form-dialog.component';
import { ConfirmDeleteDialogComponent } from '../../components/confirm-delete-dialog/confirm-delete-dialog.component';
import { Book } from '../../models/book.model';
import { Genre } from '../../models/genre.model';
import { BookListStore } from './book-list.store';
import { LoginStore } from '../login/login.store';
import { GenreService } from '../../services/genre.service';

type SortField = 'title' | 'title_desc' | 'author' | 'isbn';
type AvailabilityFilter = 'all' | 'available' | 'borrowed';

@Component({
  selector: 'app-book-list-page',
  standalone: true,
  imports: [CommonModule, MatDialogModule],
  templateUrl: './book-list-page.component.html',
  styleUrl: './book-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookListPageComponent {
  private readonly dialog      = inject(MatDialog);
  private readonly loginStore  = inject(LoginStore);
  private readonly router      = inject(Router);
  private readonly destroyRef  = inject(DestroyRef);
  private readonly genreService = inject(GenreService);
  protected readonly store     = inject(BookListStore);

  // Store state — typed explicitly so the template doesn't see 'unknown'
  protected readonly books     = this.store.books;
  protected readonly isLoading = this.store.isLoading;
  protected readonly hasError  = this.store.hasError;

  protected readonly isAdmin       = computed(() => this.loginStore.role() === 'ADMIN');
  protected readonly currentUserId = this.loginStore.currentUserId;

  // ── Filter / sort signals ──
  protected readonly searchQuery       = signal('');
  protected readonly availabilityFilter = signal<AvailabilityFilter>('all');
  protected readonly genreFilter        = signal<string>('all');
  protected readonly sortField          = signal<SortField>('title');

  // ALL genres from the API — ensures every genre appears in the filter even
  // if no book currently carries it
  protected readonly allGenres = signal<Genre[]>([]);

  // ── Due date inline edit (admin only) ──
  protected readonly editingDueDateFor   = signal<string | null>(null);
  protected readonly editingDueDateValue = signal<string>('');

  // How many books this customer currently has borrowed (max 3 per backend rule)
  protected readonly myBorrowedCount = computed(() =>
    this.books().filter(b => b.borrowedBy !== null && b.borrowedBy.id === this.currentUserId()).length
  );

  protected readonly hasActiveFilters = computed(
    () =>
      this.searchQuery().trim() !== '' ||
      this.availabilityFilter() !== 'all' ||
      this.genreFilter() !== 'all'
  );

  protected readonly filteredBooks = computed(() => {
    const query        = this.searchQuery().trim().toLowerCase();
    const availability = this.availabilityFilter();
    const genre        = this.genreFilter();
    const sort         = this.sortField();
    const admin        = this.isAdmin();
    const myId         = this.currentUserId();

    let result = this.books().filter((book: Book) => {
      // Customers see: available books + their own borrowed books
      if (!admin && book.borrowedBy !== null && book.borrowedBy.id !== myId) {
        return false;
      }

      const matchesSearch =
        !query ||
        book.title.toLowerCase().includes(query) ||
        (book.authorName ?? '').toLowerCase().includes(query);

      const matchesAvailability =
        !admin ||
        availability === 'all' ||
        (availability === 'available' && book.borrowedBy === null) ||
        (availability === 'borrowed'  && book.borrowedBy !== null);

      const matchesGenre =
        genre === 'all' || (book.genres ?? []).some(g => g.name === genre);

      return matchesSearch && matchesAvailability && matchesGenre;
    });

    return [...result].sort((a: Book, b: Book) => {
      switch (sort) {
        case 'title':      return a.title.localeCompare(b.title);
        case 'title_desc': return b.title.localeCompare(a.title);
        case 'author':     return (a.authorName ?? '').localeCompare(b.authorName ?? '');
        case 'isbn':       return a.isbn.localeCompare(b.isbn);
        default:           return 0;
      }
    });
  });

  constructor() {
    this.store.load();
    this.genreService.getAll().subscribe(genres => this.allGenres.set(genres));
  }

  // ── Auth ──
  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  // ── Filter handlers ──
  protected onSearchChange(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }
  protected clearSearch(): void { this.searchQuery.set(''); }
  protected onAvailabilityChange(value: AvailabilityFilter): void { this.availabilityFilter.set(value); }
  protected onGenreFilterChange(value: string): void { this.genreFilter.set(value); }
  protected onSortChange(value: SortField): void { this.sortField.set(value); }
  protected clearAllFilters(): void {
    this.searchQuery.set('');
    this.availabilityFilter.set('all');
    this.genreFilter.set('all');
  }

  // ── Borrow / Return ──
  protected borrowBook(book: Book): void  { this.store.borrow(book.id); }
  protected returnBook(book: Book): void  { this.store.returnBook(book.id); }

  // Helper used in template to check if a book is borrowed by the current user
  protected isBorrowedByMe(book: Book): boolean {
    return book.borrowedBy !== null && book.borrowedBy.id === this.currentUserId();
  }

  // ── Due date inline edit (admin only) ──
  protected startEditDueDate(book: Book): void {
    this.editingDueDateFor.set(book.id);
    // Use the date string directly — it's already YYYY-MM-DD from the backend.
    // Routing through new Date() can shift the date by one day depending on timezone.
    const existing = book.dueDate ?? new Date().toISOString().split('T')[0];
    this.editingDueDateValue.set(existing);
  }

  protected cancelEditDueDate(): void {
    this.editingDueDateFor.set(null);
    this.editingDueDateValue.set('');
  }

  protected saveDueDate(book: Book, dateValue: string): void {
    if (!dateValue) return;
    this.store.patchDueDate(book.id, dateValue);
    this.editingDueDateFor.set(null);
    this.editingDueDateValue.set('');
  }

  // ── CRUD dialogs ──
  protected openCreateDialog(): void {
    this.dialog
      .open<BookFormDialogComponent, BookFormDialogData, BookFormDialogResult>(
        BookFormDialogComponent,
        { data: { title: 'Add Book', submitLabel: 'Add Book' }, width: '560px' },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => { if (result) this.store.create(result); });
  }

  protected openEditDialog(book: Book): void {
    this.dialog
      .open<BookFormDialogComponent, BookFormDialogData, BookFormDialogResult>(
        BookFormDialogComponent,
        { data: { title: 'Edit Book', submitLabel: 'Save changes', initialValue: book }, width: '560px' },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        // Store.update now takes (id, updates) — two separate args
        if (result) this.store.update(book.id, result);
      });
  }

  protected openDeleteDialog(book: Book): void {
    this.dialog
      .open<ConfirmDeleteDialogComponent, { name: string }, boolean>(
        ConfirmDeleteDialogComponent,
        { data: { name: book.title } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(confirmed => { if (confirmed) this.store.remove(book.id); });
  }

  // ── Utilities ──
  protected getDueDateClassification(dueDateStr: string | null): string {
    if (!dueDateStr) return 'status-clear';
    const due   = new Date(dueDateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const days = Math.ceil((due.getTime() - today.getTime()) / (1000 * 3600 * 24));
    if (days < 0)  return 'status-overdue';
    if (days <= 3) return 'status-critical';
    return 'status-extended';
  }

  protected getDaysUntilDue(dueDateStr: string | null): number | null {
    if (!dueDateStr) return null;
    const due   = new Date(dueDateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.ceil((due.getTime() - today.getTime()) / (1000 * 3600 * 24));
  }
}