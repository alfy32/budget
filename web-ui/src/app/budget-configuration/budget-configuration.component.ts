import {Component, OnInit} from '@angular/core';
import {BudgetsService} from "../budgets.service";
import {Budget} from "../budget";
import {CommonModule, NgForOf} from "@angular/common";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-budget-configuration',
  standalone: true,
  imports: [CommonModule, FormsModule, NgForOf],
  templateUrl: './budget-configuration.component.html',
  styleUrl: './budget-configuration.component.css'
})
export class BudgetConfigurationComponent implements OnInit {
  budgets: Budget[] = [];

  constructor(
    private budgetService: BudgetsService,
  ) {
  }

  ngOnInit() {
    this.loadBudgets();
  }

  private loadBudgets() {
    this.budgetService.getBudgets().subscribe(budgets => {
      budgets.sort((budget1: Budget, budget2: Budget) => {
        if (budget1.name < budget2.name) {
          return -1;
        } else if (budget1.name > budget2.name) {
          return 1;
        } else {
          return 0;
        }
      });

      this.budgets = budgets;
    });
  }

  onAmountChange(id: string | undefined, amount: number) {
    if (id) {
      this.budgetService.setAmount(id, amount).subscribe(() => {
      });
    }
  }

  setMonthly(id: string | undefined, monthly: boolean) {
    if (id) {
      this.budgetService.setMonthly(id, monthly).subscribe(() => {
      });
    }
  }

}
