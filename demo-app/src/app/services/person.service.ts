import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreatePersonDto, Person } from '../models/person.model';

const API_URL = 'http://localhost:8080/person';

@Injectable({ providedIn: 'root' })
export class PersonService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<Person[]> {
    return this.http.get<Person[]>(API_URL);
  }

  create(dto: CreatePersonDto): Observable<Person> {
    return this.http.post<Person>(API_URL, dto);
  }

  update(id: string, dto: CreatePersonDto): Observable<Person> {
    return this.http.put<Person>(`${API_URL}/${id}`, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${id}`);
  }

  patch(id: string, updates: Partial<Person>): Observable<Person> {
    return this.http.patch<Person>(`${API_URL}/${id}`, updates);
  }
}
