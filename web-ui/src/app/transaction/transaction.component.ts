import {Component, OnInit} from '@angular/core';
import {TransactionService} from '../transaction.service';
import {Transaction} from '../transaction';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
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
  query: String = 'all';
  transaction: Transaction = {
    id: '',
    splitIndex: -1,
    account: '',
    transactionType: '',
    transactionDate: '',
    description: '',
    amount: 0,
    needsTransferred: false,
  };
  editingAmount: boolean = false;
  editingDescription: boolean = false;
  editingNotes: boolean = false;
  editingDate: boolean = false;
  editingTransactionType: boolean = false;
  areYouSure: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService
  ) {
    this.route.params.subscribe(params => this.getTransaction(params['id']));
    this.route.queryParams.subscribe(params => this.query = params['query'])
  }

  ngOnInit(): void {
  }

  onClickAmount(): void {
    if (!this.editingAmount) {
      this.editingAmount = true;
    }
  }

  onClickUpdateAmount() {
    this.transactionService.setTransactionAmount(this.transaction.id, this.transaction.amount).subscribe(result => {
      this.editingAmount = false;
    });
  }

  onClickCancelEditAmount(): void {
    if (this.editingAmount) {
      this.transactionService.getTransaction(this.transaction.id).subscribe(transaction => {
        this.editingAmount = false;
        this.transaction = transaction;
      });
    }
  }

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

  onClickCancelEditDescription(): void {
    if (this.editingDescription) {
      this.transactionService.getTransaction(this.transaction.id).subscribe(transaction => {
        this.editingDescription = false;
        this.transaction = transaction;
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

  onClickDate(): void {
    if (!this.editingDate) {
      this.editingDate = true;
    }
  }

  onClickUpdateDate(): void {
    this.transactionService.setTransactionDate(this.transaction.id, this.transaction.transactionDate).subscribe(() => {
      this.editingDate = false;
    });
  }

  onClickCancelDate(): void {
    this.transactionService.getTransaction(this.transaction.id).subscribe(transaction => {
      this.transaction = transaction;
      this.editingDate = false;
    });
  }

  onClickTransactionType(): void {
    if (!this.editingTransactionType) {
      this.editingTransactionType = true;
    }
  }

  onClickUpdateTransactionType(): void {
    this.transactionService.setTransactionType(this.transaction.id, this.transaction.transactionType).subscribe(result => {
      this.editingTransactionType = false;
    });
  }

  onClickCancelTransactionType(): void {
    this.transactionService.getTransaction(this.transaction.id).subscribe(transaction => {
      this.transaction = transaction;
      this.editingTransactionType = false;
    });
  }

  onClickNeedsTransferred(): void {
    this.transactionService.setNeedsTransferred(this.transaction.id, this.transaction.needsTransferred).subscribe(result => {
      this.transactionService.getTransaction(this.transaction.id).subscribe(transaction => {
        this.transaction = transaction;
      });
    });
  }

  getTransaction(id: string): void {
    this.transactionService.getTransaction(id).subscribe(transaction => {
      this.transaction = transaction;
    });
  }

  deleteTransaction(): void {
    this.areYouSure = true;
  }

  imSureDeleteTransaction(): void {
    this.transactionService.deleteTransaction(this.transaction.id).subscribe(() => {
      this.router.navigate(['/transactions'], {
        queryParams: {
          query: this.query
        }
      })
    });
  }

}
