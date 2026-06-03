export interface Genre {
  id: string;
  name: string;
  books?: any[]; 
}
export interface CreateGenreDto {
  name: string;
}