import {Component, OnInit} from '@angular/core';
import {BudgetsService} from "../budgets.service";
import {Budget} from "../budget";
import {CommonModule, NgForOf} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {Category} from "../category";
import {CategoriesService} from "../categories.service";

@Component({
  selector: 'app-budget-configuration',
  standalone: true,
  imports: [CommonModule, FormsModule, NgForOf],
  templateUrl: './budget-configuration.component.html',
  styleUrl: './budget-configuration.component.css'
})
export class BudgetConfigurationComponent implements OnInit {
  budgetId: string = '';
  name: string = '';
  amount: number = 0;
  monthly: boolean = false;

  budget?: Budget;
  categories: Category[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private budgetService: BudgetsService,
    private categoriesService: CategoriesService,
  ) {
    this.route.params.subscribe(params => this.budgetId = params['id']);
  }

  ngOnInit() {
    this.refreshBudget();
  }

  private refreshBudget() {
    this.categoriesService.getCategories().subscribe(categories => {
      this.categories = categories;
    });

    this.budgetService.getBudget(this.budgetId).subscribe(budget => {
      this.name = budget.name;
      this.amount = budget.amount;
      this.monthly = budget.monthly;
      this.budget = budget;
    });
  }

  updateName() {
    this.budgetService.setName(this.budgetId, this.name).subscribe(() => {
      this.refreshBudget();
    });
  }

  updateAmount() {
    this.budgetService.setAmount(this.budgetId, this.amount).subscribe(() => {
      this.refreshBudget();
    });
  }

  updateMonthly() {
    this.budgetService.setMonthly(this.budgetId, this.monthly).subscribe(() => {
      this.refreshBudget();
    });
  }

  done() {
    this.router.navigate(['budgets']);
  }

  deleteBudget() {
    this.budgetService.delete(this.budgetId).subscribe(() => {
      this.router.navigate(['budgets']);
    });
  }

}
