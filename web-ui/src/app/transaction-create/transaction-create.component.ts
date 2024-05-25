import {Component} from '@angular/core';
import {Router} from "@angular/router";
import {TransactionService} from "../transaction.service";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {BankTransaction} from "../bankTransaction";
import {CurrencyPipe, DatePipe, NgIf} from "@angular/common";

@Component({
  selector: 'app-transaction-create',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, NgIf, DatePipe, CurrencyPipe],
  templateUrl: './transaction-create.component.html',
  styleUrl: './transaction-create.component.css'
})
export class TransactionCreateComponent {
  bankTransaction: BankTransaction;

  constructor(
    private router: Router,
    private transactionService: TransactionService,
  ) {
    this.bankTransaction = {
      id: "00000000-0000-0000-0000-000000000000",
      account: "Cash",
      transactionType: "debit",
      transactionDate: new Date().toISOString().split("T")[0],
      description: "",
      amount: 0
    };
  }

  createTransaction(): void {
    this.transactionService.createTransaction(this.bankTransaction).subscribe(transactionId => {
      this.router.navigate(['/transactions/' + transactionId]);
    });
  }
}
