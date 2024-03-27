import {Component, OnInit} from '@angular/core';
import {CategoriesService} from "../categories.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Category} from "../category";
import {BudgetsService} from "../budgets.service";
import {Budget} from "../budget";
import {JsonPipe, NgForOf} from "@angular/common";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [
    NgForOf,
    FormsModule,
    ReactiveFormsModule,
    JsonPipe
  ],
  templateUrl: './category.component.html',
  styleUrl: './category.component.css'
})
export class CategoryComponent implements OnInit {
  categoryId: string = '';
  categoryName: string = '';

  category?: Category;

  selectedBudgetId?: string;
  budgets: Budget[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private budgetService: BudgetsService,
    private categoriesService: CategoriesService,
  ) {
    this.route.params.subscribe(params => this.categoryId = params['id']);
  }

  ngOnInit() {
    this.refreshCategory();

    this.budgetService.getBudgets().subscribe(budgets => {
      this.budgets = budgets;
    });
  }

  refreshCategory(): void {
    this.categoriesService.getCategory(this.categoryId).subscribe(category => {
      this.categoryName = category.name;

      this.category = category;

      this.selectedBudgetId = category.budget?.id;
    })
  }

  sendUpdatedCategory() {
    if (this.category) {
      this.categoriesService.updateCategory(this.category).subscribe(value => {
        this.refreshCategory();
      });
    }
  }

  updateCategoryName(): void {
    if (this.category) {
      this.category.name = this.categoryName;
      this.sendUpdatedCategory();
    }
  }

  budgetSelected(): void {
    if (this.category) {
      let selectedBudget;
      for (let budget of this.budgets) {
        if (budget.id == this.selectedBudgetId) {
          selectedBudget = budget;
        }
      }

      this.category.budget = selectedBudget;
      this.sendUpdatedCategory();
    }
  }

  done(): void {
    this.router.navigate(['categories']);
  }

  deleteCategory(): void {
    this.categoriesService.deleteCategory(this.categoryId).subscribe(() => {
      this.router.navigate(['categories']);
    });
  }

}
