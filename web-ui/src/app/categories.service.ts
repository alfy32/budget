import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";

import {Category} from "./category";

@Injectable({providedIn: 'root'})
export class CategoriesService {

  constructor(
    private http: HttpClient
  ) {
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/rest/categories');
  }

  getCategory(id: string): Observable<Category> {
    return this.http.get<Category>('/rest/categories/' + id);
  }

  addCategory(name: string): Observable<void> {
    const formData = new URLSearchParams();
    formData.set('name', name);
    return this.http.post<void>(
      '/rest/categories',
      {name: name}
    );
  }

  updateCategory(category: Category): Observable<void> {
    return this.http.post<void>(
      '/rest/categories/' + category.id,
      category
    )
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>('/rest/categories/' + id);
  }

}
