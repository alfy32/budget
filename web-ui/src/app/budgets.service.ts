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

  getBudget(budgetId: string): Observable<Budget> {
    return this.http.get<Budget>('/rest/budgets/' + budgetId);
  }

  createBudget(name: string) {
    return this.http.post<void>(
      '/rest/budgets',
      {
        name: name
      }
    );
  }

  setName(id: string, name: string) {
    return this.http.post<void>(
      '/rest/budgets/' + id + '/name',
      name
    );
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

  delete(id: string) {
    return this.http.delete<void>('/rest/budgets/' + id);
  }
}
