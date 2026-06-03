import { ChangeDetectionStrategy, Component, DestroyRef, inject, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { ConfirmDeleteDialogComponent } from '../../components/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  GenreFormDialogComponent,
  GenreFormDialogData,
  GenreFormDialogResult,
} from '../../components/genre-form-dialog/genre-form-dialog.component';
import { Genre } from '../../models/genre.model';
import { GenreListStore } from './genre-list.store';
import { LoginStore } from '../login/login.store';

@Component({
  selector: 'app-genre-list-page',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
  ],
  templateUrl: './genre-list-page.component.html',
  styleUrl: './genre-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenreListPageComponent {
  private readonly dialog = inject(MatDialog);
  private readonly store = inject(GenreListStore);
  private readonly destroyRef = inject(DestroyRef);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);

  protected readonly genres = this.store.genres;
  protected readonly isLoading = this.store.isLoading;

  constructor() {
    this.store.load();
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  protected openCreateDialog(): void {
    if (this.isLoading()) return;

    this.dialog
      .open<GenreFormDialogComponent, GenreFormDialogData, GenreFormDialogResult>(
        GenreFormDialogComponent,
        { data: { title: 'Add genre', submitLabel: 'Create' }, width: '400px' },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (result) this.store.create(result);
      });
  }

  protected deleteGenre(genre: Genre): void {
    if (this.isLoading()) return;

    this.dialog
      .open<ConfirmDeleteDialogComponent, { name: string }, boolean>(
        ConfirmDeleteDialogComponent,
        { data: { name: genre.name } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (confirmed) this.store.remove(genre.id);
      });
  }

  protected editGenre(genre: Genre): void {
    this.dialog
      .open<GenreFormDialogComponent, GenreFormDialogData, GenreFormDialogResult>(
        GenreFormDialogComponent,
        {
          data: {
            title: 'Edit genre',
            submitLabel: 'Save changes',
            initialValue: { name: genre.name },
          },
          width: '400px',
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (result) this.store.update(genre.id, result);
      });
  }
}