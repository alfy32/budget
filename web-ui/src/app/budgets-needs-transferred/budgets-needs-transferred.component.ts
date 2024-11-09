import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {RouterModule} from '@angular/router';
import {TransactionService} from '../transaction.service';
import {TransferInfo} from "../transferInfo";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './budgets-needs-transferred.component.html',
  styleUrl: './budgets-needs-transferred.component.css'
})
export class BudgetsNeedsTransferredComponent implements OnInit {
  transferInfoList: TransferInfo[] = [];

  constructor(
    private transactionService: TransactionService
  ) {
  }

  ngOnInit(): void {
    this.updateTransferInfo();
  }

  updateTransferInfo(): void {
    this.transactionService.getNeedsTransferredBudgets().subscribe(transferInfo => {
      this.transferInfoList = transferInfo;
    });
  }

  createQuery(id: string) {
    return "category," + id + ",needsTransferred";
  }

  markTransferred(account: String) {
    this.transactionService.markTransferred(account).subscribe(() => {
      this.updateTransferInfo();
    });
  }

}
