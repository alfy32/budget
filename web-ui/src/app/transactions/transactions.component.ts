import {Component} from '@angular/core';

import {TransactionService} from '../transaction.service';
import {Transaction} from '../transaction';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, RouterModule} from '@angular/router';
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.css'
})
export class TransactionsComponent {
  query?: string | undefined = 'all';
  transactions: Transaction[] = [];
  search: string = '';

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService
  ) {
    this.route.queryParams.subscribe(params => {
      let query = params['query'];
      if (query != this.query) {
        this.query = query;
        this.transactions = [];
        this.getTransactions();
      }
    })
  }

  getTransactions(): void {
    this.transactionService.getTransactions(this.query).subscribe(transactions => this.transactions = transactions)
  }

  matchesSearch(transaction: Transaction) {
    if (this.search === '') {
      return true;
    }

    const search = this.search.toLowerCase();

    if (transaction.description != null) {
      if (transaction.description.toLowerCase().includes(search)) {
        return true;
      }
    }

    if (transaction.notes != null) {
      if (transaction.notes.toLowerCase().includes(search)) {
        return true;
      }
    }

    return false;
  }
}
