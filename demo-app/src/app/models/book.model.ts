import { Person } from './person.model';
import { Genre } from './genre.model';

export interface Book {
  id: string;
  title: string;
  authorName: string;
  isbn: string;
  borrowedBy: Person | null;
  dueDate: string | null; // ISO Date String structure from Strategy pattern
  genres: Genre[];
}

export interface CreateBookDto {
  title: string;
  authorName: string;
  isbn: string;
  personId: string | null;
  genreIds: string[];
}