import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, RouterModule} from '@angular/router';
import {TransactionService} from '../transaction.service';
import {Budget} from '../budget';

@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './budgets.component.html',
  styleUrl: './budgets.component.css'
})
export class BudgetsComponent implements OnInit {
  date: Date = new Date();
  previousDate: string = '';
  nextDate: string = '';
  budgets: Budget[] = [];

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService
  ) { }

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
    this.transactionService.getBudgets(this.date).subscribe(budgets => {
      this.budgets = budgets;
    });
  }

}
