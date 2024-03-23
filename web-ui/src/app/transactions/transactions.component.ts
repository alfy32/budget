import {Component, OnInit} from '@angular/core';

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
export class TransactionsComponent implements OnInit {
  needsCategorized: boolean = false;
  transactions: Transaction[] = [];

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService
  ) {
    this.route.queryParams.subscribe(params => {
      let needsCategorized = params['needsCategorized'];
      if (needsCategorized != this.needsCategorized) {
        this.needsCategorized = needsCategorized;
        this.transactions = [];
        this.getTransactions();
      }
    })
  }

  ngOnInit(): void {
    this.getTransactions();
  }

  getTransactions(): void {
    this.transactionService.getTransactions(this.needsCategorized).subscribe(transactions => this.transactions = transactions)
  }

}
