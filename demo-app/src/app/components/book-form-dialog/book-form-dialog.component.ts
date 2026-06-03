import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectorRef,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { PersonService } from '../../services/person.service';
import { Person } from '../../models/person.model';
import { GenreService } from '../../services/genre.service';
import { Genre } from '../../models/genre.model';

export interface BookFormValue {
  title: string;
  authorName: string;
  isbn: string;
  personId: string | null;
  genreIds: string[];
  dueDate: string | null; // <-- ADDED
}

export interface BookFormDialogData {
  title: string;
  submitLabel?: string;
  initialValue?: any | null;
}

export type BookFormDialogResult = BookFormValue | undefined;

@Component({
  selector: 'app-book-form-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule],
  templateUrl: './book-form-dialog.component.html',
  styleUrl: './book-form-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<BookFormDialogComponent>);
  protected readonly data = inject<BookFormDialogData>(MAT_DIALOG_DATA);
  private readonly personService = inject(PersonService);
  private readonly genreService = inject(GenreService);
  private readonly cdr = inject(ChangeDetectorRef);

  protected people: Person[] = [];
  protected genres: Genre[] = [];

  // Added dueDate control to the form group
  protected readonly form = this.fb.nonNullable.group({
    title:      ['', [Validators.required, Validators.minLength(2)]],
    authorName: ['', [Validators.required]],
    isbn:       ['', [Validators.required, Validators.pattern(/^(978|979)[0-9]{10}$/)]],
    personId:   [''],
    genreIds:   [[] as string[], [Validators.required]],
    dueDate:    [''], // <-- ADDED
  });

  protected readonly selectedGenreIds = signal<string[]>([]);
  protected readonly genreSearch = signal('');

  protected readonly filteredGenres = computed(() => {
    const q = this.genreSearch().toLowerCase().trim();
    if (!q) return this.genres;
    return this.genres.filter(g => g.name.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.personService.getAll().subscribe((data) => {
      this.people = data;
      this.cdr.markForCheck();
    });

    this.genreService.getAll().subscribe((data) => {
      this.genres = data;
      this.cdr.markForCheck();
    });

    if (this.data?.initialValue) {
      const ids: string[] = this.data.initialValue.genres?.map((g: any) => g.id) ?? [];
      this.form.patchValue({
        title:      this.data.initialValue.title,
        authorName: this.data.initialValue.authorName,
        isbn:       this.data.initialValue.isbn,
        personId:   this.data.initialValue.borrowedBy?.id ?? '',
        genreIds:   ids,
        dueDate:    this.data.initialValue.dueDate ?? '', // <-- ADDED
      });
      this.selectedGenreIds.set(ids);
    }
  }

  protected onGenreSearch(event: Event): void {
    this.genreSearch.set((event.target as HTMLInputElement).value);
  }

  protected isGenreSelected(genreId: string): boolean {
    return this.selectedGenreIds().includes(genreId);
  }

  protected toggleGenre(genreId: string): void {
    const current = this.selectedGenreIds();
    const updated = current.includes(genreId)
      ? current.filter(id => id !== genreId)
      : [...current, genreId];

    this.selectedGenreIds.set(updated);
    this.form.controls.genreIds.setValue(updated);
    this.form.controls.genreIds.markAsTouched();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const result: BookFormValue = {
      title:      raw.title,
      authorName: raw.authorName,
      isbn:       raw.isbn,
      personId:   raw.personId || null,
      genreIds:   raw.genreIds,
      dueDate:    raw.dueDate || null, // <-- ADDED
    };
    this.dialogRef.close(result);
  }

  protected cancel(): void {
    this.dialogRef.close(undefined);
  }
}