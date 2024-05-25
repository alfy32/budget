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

  setAmount(id: string, amount: number) {
    return this.http.post<void>(
      '/rest/budgets/' + id + '/amount',
      amount
    );
  }

  setMonthly(id: string, monthly: boolean): Observable<void> {
    return this.http.post<void>(
      '/rest/budgets/' + id + '/monthly',
      monthly
    );
  }
}
