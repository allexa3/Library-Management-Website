import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import {
  ConfirmDeleteDialogComponent,
  ConfirmDeleteDialogData,
} from '../../components/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  PersonFormDialogComponent,
  PersonFormDialogData,
  PersonFormDialogResult,
} from '../../components/person-form-dialog/person-form-dialog.component';
import { CreatePersonDto, Person, UpdatePersonDto } from '../../models/person.model';
import { PersonListStore } from './person-list.store';
import { Router } from '@angular/router';
import { LoginStore } from '../login/login.store';

@Component({
  selector: 'app-person-list-page',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
  ],
  templateUrl: './person-list-page.component.html',
  styleUrl: './person-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonListPageComponent {
  private readonly dialog = inject(MatDialog);
  private readonly store = inject(PersonListStore);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly persons = this.store.persons;
  protected readonly hasError = this.store.hasError;
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
      .open<PersonFormDialogComponent, PersonFormDialogData, PersonFormDialogResult>(
        PersonFormDialogComponent,
        { data: { title: 'Add member', submitLabel: 'Create', showPasswordField: true }, width: '480px' },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.store.create(result as CreatePersonDto);
      });
  }

  protected openEditDialog(person: Person): void {
    if (this.isLoading()) return;

    this.dialog
      .open<PersonFormDialogComponent, PersonFormDialogData, PersonFormDialogResult>(
        PersonFormDialogComponent,
        { data: { title: 'Edit member', submitLabel: 'Save changes', initialValue: person }, width: '480px' },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.store.update(person.id, result as UpdatePersonDto);
      });
  }

  protected openDeleteDialog(person: Person): void {
    if (this.isLoading()) return;

    this.dialog
      .open<ConfirmDeleteDialogComponent, ConfirmDeleteDialogData, boolean>(
        ConfirmDeleteDialogComponent,
        { data: { name: person.name } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.store.remove(person.id);
      });
  }
}