import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Params, Router, RouterModule} from '@angular/router';
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
  queryParams: Params = {date: ''};
  startDate: string = '';
  transferInfoList: TransferInfo[] = [];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private transactionService: TransactionService
  ) {
    this.route.queryParams.subscribe(params => {
      this.startDate = params['start-date'];
      this.updateTransferInfo();
    });
  }

  ngOnInit(): void {
    this.startDateChanged();
  }

  startDateChanged(): void {
    this.router.navigate(
      [],
      {
        relativeTo: this.route,
        queryParams: {'start-date': this.startDate}
      }
    );
  }

  updateTransferInfo(): void {
    this.transactionService.getNeedsTransferredBudgets(this.startDate).subscribe(transferInfo => {
      this.transferInfoList = transferInfo;
    });
  }

  createQuery(id: string) {
    const queryEnd = new Date(new Date().getFullYear() + 1, 0, 1).toISOString().split('T')[0];
    return "category," + id + ',' + this.startDate + ',' + queryEnd;
  }

}
