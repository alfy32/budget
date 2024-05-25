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
  needsCategorized: boolean = false;
  needsTransferred: boolean = false;
  transactions: Transaction[] = [];

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService
  ) {
    this.route.queryParams.subscribe(params => {
      let needsCategorized = params['needsCategorized'];
      let needsTransferred = params['needsTransferred'];
      if (needsCategorized != this.needsCategorized || needsTransferred != this.needsTransferred) {
        this.needsCategorized = !!needsCategorized;
        this.needsTransferred = !!needsTransferred;
        this.transactions = [];
        this.getTransactions();
      }
    })
  }

  getTransactions(): void {
    this.transactionService.getTransactions(this.needsCategorized, this.needsTransferred).subscribe(transactions => this.transactions = transactions)
  }

}
