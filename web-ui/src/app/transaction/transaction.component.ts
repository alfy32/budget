import {Component, OnInit} from '@angular/core';
import {TransactionService} from '../transaction.service';
import {Transaction} from '../transaction';
import {ActivatedRoute, RouterModule} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

@Component({
  selector: 'app-transaction',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ReactiveFormsModule],
  templateUrl: './transaction.component.html',
  styleUrl: './transaction.component.css'
})
export class TransactionComponent implements OnInit {
  needsCategorized: boolean = false;
  transaction: Transaction = {
    id: '',
    splitIndex: -1,
    account: '',
    transactionDate: '',
    description: '',
    amount: 0,
  };
  editingDescription: boolean = false;
  editingNotes: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService
  ) {
    this.route.params.subscribe(params => this.getTransaction(params['id']));
    this.route.queryParams.subscribe(params => this.needsCategorized = params['needsCategorized'])
  }

  ngOnInit(): void { }

  onClickDescription(): void {
    if (!this.editingDescription) {
      this.editingDescription = true;
    }
  }

  onClickUpdateDescription(): void {
    this.transactionService.setTransactionDescription(this.transaction.id, this.transaction.description).subscribe(result => {
      this.editingDescription = false;
    });
  }

  onClickResetDescription(): void {
    if (this.transaction.bankTransaction) {
      this.transaction.description = this.transaction.bankTransaction?.description;
      this.transactionService.setTransactionDescription(this.transaction.id, this.transaction.description).subscribe(result => {
        this.editingDescription = false;
      });
    }
  }

  onClickEditNotes(): void {
    if (!this.editingNotes) {
      this.editingNotes = true;
    }
  }

  onClickUpdateNotes(): void {
    this.transactionService.setTransactionNotes(this.transaction.id, this.transaction.notes).subscribe(result => {
      this.editingNotes = false;
    });
  }

  onClickCancelNotes(): void {
    this.transactionService.getTransaction(this.transaction.id).subscribe(transaction => {
      this.transaction = transaction;
      this.editingNotes = false;
    });
  }

  getTransaction(id: string): void {
    this.transactionService.getTransaction(id).subscribe(transaction => {
      this.transaction = transaction;
    });
  }

}
