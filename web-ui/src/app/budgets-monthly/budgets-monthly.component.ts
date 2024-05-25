import {Component, OnInit} from '@angular/core';
import {CommonModule} from "@angular/common";
import {ActivatedRoute, RouterModule} from "@angular/router";
import {TransactionService} from "../transaction.service";
import {BudgetInfo} from "../budgetInfo";

@Component({
  selector: 'app-monthly',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './budgets-monthly.component.html',
  styleUrl: './budgets-monthly.component.css'
})
export class BudgetsMonthlyComponent implements OnInit {
  date: Date = new Date();
  previousDate: string = '';
  nextDate: string = '';
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
  }

  updateBudgets(): void {
    this.transactionService.getMonthlyBudgets(this.date).subscribe(budgets => {
      this.budgets = budgets;
    });
  }

}
