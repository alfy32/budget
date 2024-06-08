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
  startOfYear: string = '';
  endOfYear: string = '';
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
    this.previousDate = new Date(this.date.getFullYear() - 1, 0, 1).toISOString().split('T')[0];
    this.nextDate = new Date(this.date.getFullYear() + 1, 0, 1).toISOString().split('T')[0];
    this.startOfYear = new Date(this.date.getFullYear(), 0, 1).toISOString().split('T')[0];
    this.endOfYear = new Date(this.date.getFullYear() + 1, 0, 1).toISOString().split('T')[0];
  }

  updateBudgets(): void {
    this.transactionService.getYearlyBudgets(this.date).subscribe(budgets => {
      this.budgets = budgets;
    });
  }

  createQuery(id: string) {
    return "category," + id + ',' + this.startOfYear + ',' + this.endOfYear;
  }


}
