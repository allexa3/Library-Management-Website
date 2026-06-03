import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Book, CreateBookDto } from '../models/book.model';

@Injectable({ providedIn: 'root' })
export class BookService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/books';

  getAll(): Observable<Book[]> {
    return this.http.get<Book[]>(this.url);
  }

  create(dto: CreateBookDto): Observable<Book> {
    return this.http.post<Book>(this.url, dto);
  }

  patch(id: string, updates: Partial<CreateBookDto>): Observable<Book> {
    return this.http.patch<Book>(`${this.url}/${id}`, updates);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  borrow(bookId: string): Observable<Book> {
    return this.http.post<Book>(`${this.url}/${bookId}/borrow`, {});
  }
}