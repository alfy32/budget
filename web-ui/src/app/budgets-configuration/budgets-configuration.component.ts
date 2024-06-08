import {Component, OnInit} from '@angular/core';
import {CurrencyPipe, NgForOf} from "@angular/common";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {RouterLink} from "@angular/router";
import {Budget} from "../budget";
import {BudgetsService} from "../budgets.service";

@Component({
  selector: 'app-budgets-configuration',
  standalone: true,
  imports: [NgForOf, ReactiveFormsModule, FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './budgets-configuration.component.html',
  styleUrl: './budgets-configuration.component.css'
})
export class BudgetsConfigurationComponent implements OnInit {
  budgets: Budget[] = [];
  newBudget: string = '';

  constructor(private budgetService: BudgetsService) {
  }

  ngOnInit(): void {
    this.getBudgets();
  }

  private getBudgets() {
    this.budgetService.getBudgets().subscribe(budgets => {
      budgets.sort((budget1: Budget, budget2: Budget) => {
        if (budget1.name < budget2.name) {
          return -1;
        } else if (budget1.name > budget2.name) {
          return 1;
        } else {
          return 0;
        }
      })
      this.budgets = budgets;
    })
  }

  addNewBudget() {
    this.budgetService.createBudget(this.newBudget).subscribe(() => {
      this.newBudget = "";
      this.getBudgets();
    })
  }
}
