import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, RouterModule} from '@angular/router';
import {TransactionService} from '../transaction.service';
import {BudgetInfo} from "../budgetInfo";

@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './budgets-yearly.component.html',
  styleUrl: './budgets-yearly.component.css'
})
export class BudgetsYearlyComponent implements OnInit {
  date: Date = new Date();
  previousDate: string = '';
  nextDate: string = '';
  startOfMonth: string = '';
  endOfMonth: string = '';
  budgets: BudgetInfo[] = [];

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService
  ) {
  }

  ngOnInit(): void {
    this.updateDates();
    this.route.queryParams
      .subscribe(params => {
        if (params['date'] !== undefined) {
          this.date = new Date(params['date'] + "T00:00:00");
        } else {
          this.date = new Date();
        }
        this.updateDates();
        this.updateBudgets();
      });
  }

  updateDates(): void {
    this.previousDate = new Date(this.date.getFullYear(), this.date.getMonth() - 1, 1).toISOString().split('T')[0];
    this.nextDate = new Date(this.date.getFullYear(), this.date.getMonth() + 1, 1).toISOString().split('T')[0];
    this.startOfMonth = new Date(this.date.getFullYear(), this.date.getMonth(), 1).toISOString().split('T')[0];
    this.endOfMonth = new Date(this.date.getFullYear(), this.date.getMonth() + 1, 0).toISOString().split('T')[0];
  }

  updateBudgets(): void {
    this.transactionService.getBudgets(this.date).subscribe(budgets => {
      this.budgets = budgets;
    });
  }

  createQuery(id: string) {
    return "category," + id + ',' + this.startOfMonth + ',' + this.endOfMonth;
  }


}
