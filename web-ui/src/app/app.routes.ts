import {Routes} from '@angular/router';

import {HomeComponent} from './home/home.component';
import {TransactionsComponent} from './transactions/transactions.component';
import {TransactionComponent} from './transaction/transaction.component';
import {BudgetsComponent} from './budgets/budgets.component';
import {CategoriesComponent} from './categories/categories.component';

import {TransactionsUploadComponent} from './transactions-upload/transactions-upload.component';
import {TransactionCategoryComponent} from './transaction-category/transaction-category.component';
import {TransactionTagsComponent} from './transaction-tags/transaction-tags.component';
import {TransactionSplitComponent} from './transaction-split/transaction-split.component';
import {CategoryComponent} from "./category/category.component";

export const routes: Routes = [
    { path: '', component: HomeComponent },

    { path: 'transactions', component: TransactionsComponent },
    { path: 'transactions/:id', component: TransactionComponent },
    { path: 'transactions/:id/category', component: TransactionCategoryComponent },
    { path: 'transactions/:id/tags', component: TransactionTagsComponent },
    { path: 'split/:id', component: TransactionSplitComponent },

    { path: 'budgets', component: BudgetsComponent },
    { path: 'categories', component: CategoriesComponent },
    { path: 'categories/:id', component: CategoryComponent },

    { path: 'transactions-upload', component: TransactionsUploadComponent },
];
