import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface GenreFormDialogData {
  title: string;
  submitLabel?: string;
  initialValue?: GenreFormValue | null;
}

export interface GenreFormValue {
  name: string;
}

export type GenreFormDialogResult = GenreFormValue | undefined;

@Component({
  selector: 'app-genre-form-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule],
  templateUrl: './genre-form-dialog.component.html',
  styleUrl: './genre-form-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenreFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<GenreFormDialogComponent>);
  protected readonly data = inject<GenreFormDialogData>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
  });

  ngOnInit(): void {
    if (this.data.initialValue) {
      this.form.patchValue(this.data.initialValue);
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.getRawValue() as GenreFormValue);
  }

  protected cancel(): void {
    this.dialogRef.close(undefined);
  }
}