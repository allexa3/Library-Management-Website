import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface PersonFormDialogData {
  title: string;
  submitLabel?: string;
  showPasswordField?: boolean;
  initialValue?: PersonFormInitialValue | null;
}

export interface PersonFormValue {
  name: string;
  age: number;
  email: string;
  role: string;
  password?: string;
}

export interface PersonFormInitialValue {
  name: string;
  age: number;
  email: string;
  role: string;
}

export type PersonFormDialogResult = PersonFormValue | undefined;

@Component({
  selector: 'app-person-form-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule],
  templateUrl: './person-form-dialog.component.html',
  styleUrl: './person-form-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PersonFormDialogComponent>);
  protected readonly data = inject<PersonFormDialogData>(MAT_DIALOG_DATA);

  protected readonly isPasswordVisible = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name:     ['', [Validators.required, Validators.minLength(2)]],
    age:      [18, [Validators.required, Validators.min(18), Validators.max(200)]],
    email:    ['', [Validators.required, Validators.email]],
    role:     ['CUSTOMER', [Validators.required]],
    password: ['', []],
  });

  ngOnInit(): void {
    if (this.data.initialValue) {
      this.form.patchValue(this.data.initialValue);
    }

    if (this.data.showPasswordField) {
      this.form.controls.password.setValidators([Validators.required]);
      this.form.controls.password.updateValueAndValidity();
    }
  }

  protected togglePasswordVisibility(): void {
    this.isPasswordVisible.update((v) => !v);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, age, email, role, password } = this.form.getRawValue();
    const result: PersonFormValue = this.data.showPasswordField
      ? { name, age, email, role, password }
      : { name, age, email, role };

    this.dialogRef.close(result);
  }

  protected cancel(): void {
    this.dialogRef.close(undefined);
  }
}