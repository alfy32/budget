import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {Transaction} from "./transaction";
import {Split} from "./split";
import {BudgetInfo} from "./budgetInfo";
import {BankTransaction} from "./bankTransaction";
import {TransferInfo} from "./transferInfo";

@Injectable({providedIn: 'root'})
export class TransactionService {

  constructor(
    private http: HttpClient
  ) {
  }

  getTransactions(query: string | undefined): Observable<Transaction[]> {
    return this.http.get<Transaction[]>('/rest/transactions', {
      params: {
        query: query ? query : 'all'
      }
    });
  }

  createTransaction(bankTransaction: BankTransaction): Observable<String> {
    return this.http.post<String>(
      '/rest/transactions/create',
      bankTransaction,
      {}
    );
  }

  deleteTransaction(transactionId: string): Observable<void> {
    return this.http.delete<void>('/rest/transactions/' + transactionId);
  }

  getTransaction(transactionId: string): Observable<Transaction> {
    return this.http.get<Transaction>('/rest/transactions/' + transactionId);
  }

  setTransactionAmount(transactionId: string, amount: number): Observable<void> {
    return this.http.post<void>(
      '/rest/transactions/' + transactionId + '/amount',
      amount
    )
  }

  setTransactionDescription(transactionId: string, description: string): Observable<Object> {
    const formData = new URLSearchParams();
    formData.set('description', description);
    return this.http.post(
      '/rest/transactions/' + transactionId + '/description',
      formData.toString(),
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    )
  }

  setTransactionCategory(transactionId: string, categoryId: string): Observable<void> {
    const formData = new URLSearchParams();
    formData.set('categoryId', categoryId);
    return this.http.post<void>(
      '/rest/transactions/' + transactionId + '/category',
      formData.toString(),
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    )
  }

  setTransactionTags(transactionId: string, tags: string[]): Observable<void> {
    return this.http.post<void>(
      '/rest/transactions/' + transactionId + '/tags',
      tags
    )
  }

  setTransactionNotes(transactionId: string, notes: any): Observable<void> {
    return this.http.post<void>(
      '/rest/transactions/' + transactionId + '/notes',
      notes
    )
  }

  setTransactionDate(transactionId: string, transactionDate: string): Observable<void> {
    return this.http.post<void>(
      '/rest/transactions/' + transactionId + '/date',
      new String(transactionDate)
    )
  }

  setTransactionType(transactionId: string, transactionType: string): Observable<void> {
    return this.http.post<void>(
      '/rest/transactions/' + transactionId + '/type',
      transactionType
    )
  }

  setNeedsTransferred(transactionId: string, needsTransferred: boolean): Observable<void> {
    if (needsTransferred) {
      return this.http.put<void>(
        '/rest/transactions/' + transactionId + '/transferComplete',
        null
      );
    } else {
      return this.http.put<void>(
        '/rest/transactions/' + transactionId + '/needsTransferred',
        null
      );
    }
  }

  getSplit(id: string): Observable<Split> {
    return this.http.get<Split>('/rest/split/' + id);
  }

  saveSplit(split?: Split): Observable<Split> {
    return this.http.post<Split>(
      '/rest/split/' + split?.bankTransaction?.id,
      split
    );
  }

  getMonthlyBudgets(date: Date): Observable<BudgetInfo[]> {
    const dateString = date.toISOString().split('T')[0];
    return this.http.get<BudgetInfo[]>('/rest/budgets/query-monthly?date=' + dateString);
  }

  getYearlyBudgets(date: Date): Observable<BudgetInfo[]> {
    const dateString = date.toISOString().split('T')[0];
    return this.http.get<BudgetInfo[]>('/rest/budgets/query-yearly?date=' + dateString);
  }

  getNeedsTransferredBudgets(): Observable<TransferInfo[]> {
    return this.http.get<TransferInfo[]>('/rest/budgets/query-needs-transferred');
  }

  markTransferred(account: String) {
    return this.http.post<void>(
      '/rest/budgets/transfer-completed',
      account
    )
  }
}
