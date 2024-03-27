import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {CategoriesService} from '../categories.service';
import {RouterModule} from '@angular/router';
import {Category} from "../category";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.css'
})
export class CategoriesComponent implements OnInit {
  categories: Category[] = [];
  newCategory: string = '';

  constructor(private categoriesService: CategoriesService) { }

  ngOnInit(): void {
    this.getCategories();
  }

  getCategories(): void {
    this.categoriesService.getCategories().subscribe(categories => {
      this.categories = categories;
    });
  }

  addNewCategory(): void {
    this.categoriesService.addCategory(this.newCategory).subscribe(() => {
      this.newCategory = '';
      this.getCategories();
    });
  }

}
