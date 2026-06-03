import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Genre, CreateGenreDto } from '../models/genre.model';

@Injectable({ providedIn: 'root' })
export class GenreService {
  
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/genre';

  getAll(): Observable<Genre[]> {
    return this.http.get<Genre[]>(this.url);
  }

  create(dto: CreateGenreDto): Observable<Genre> {
    return this.http.post<Genre>(this.url, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  update(id: string, dto: CreateGenreDto): Observable<Genre> {
  return this.http.put<Genre>(`${this.url}/${id}`, dto);
}
}