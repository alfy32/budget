import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {Budget} from "./budget";

@Injectable({providedIn: 'root'})
export class BudgetsService {

  constructor(
    private http: HttpClient
  ) {
  }

  getBudgets(): Observable<Budget[]> {
    return this.http.get<Budget[]>('/rest/budgets');
  }

}
