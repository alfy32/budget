import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";

import {Category} from "./category";

@Injectable({ providedIn: 'root' })
export class CategoriesService {

    constructor(
        private http: HttpClient
    ) { }

    getCategories(): Observable<Category[]> {
        return this.http.get<Category[]>('/rest/categories');
    }

    addCategory(name: string): Observable<void> {
        const formData = new URLSearchParams();
        formData.set('name', name);
        return this.http.post<void>(
            '/rest/categories',
            { name: name }
        );
    }

    updateCategory(id: number, name: string): Observable<void> {
        console.log(id + " " + name);
        return this.http.post<void>(
            '/rest/categories/' + id,
            { name: name }
        )
    }

    deleteCategory(id: number): Observable<void> {
        return this.http.delete<void>('/rest/categories/' + id);
    }

}
