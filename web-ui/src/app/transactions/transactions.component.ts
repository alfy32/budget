import {Component} from '@angular/core';

import {TransactionService} from '../transaction.service';
import {Transaction} from '../transaction';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, RouterModule} from '@angular/router';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.css'
})
export class TransactionsComponent {
  query?: string | undefined = 'all';
  transactions: Transaction[] = [];

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

}
