import {Routes} from '@angular/router';

import {HomeComponent} from './home/home.component';
import {TransactionsComponent} from './transactions/transactions.component';
import {TransactionComponent} from './transaction/transaction.component';
import {BudgetsYearlyComponent} from './budgets-yearly/budgets-yearly.component';
import {CategoriesComponent} from './categories/categories.component';

import {TransactionsUploadComponent} from './transactions-upload/transactions-upload.component';
import {TransactionCategoryComponent} from './transaction-category/transaction-category.component';
import {TransactionTagsComponent} from './transaction-tags/transaction-tags.component';
import {TransactionSplitComponent} from './transaction-split/transaction-split.component';
import {CategoryComponent} from "./category/category.component";
import {TransactionCreateComponent} from "./transaction-create/transaction-create.component";
import {BudgetsMonthlyComponent} from "./budgets-monthly/budgets-monthly.component";
import {BudgetConfigurationComponent} from "./budget-configuration/budget-configuration.component";
import {BudgetsConfigurationComponent} from "./budgets-configuration/budgets-configuration.component";
import {BudgetsNeedsTransferredComponent} from "./budgets-needs-transferred/budgets-needs-transferred.component";

export const routes: Routes = [
    { path: '', component: HomeComponent },

    { path: 'create-transaction', component: TransactionCreateComponent },

    { path: 'transactions', component: TransactionsComponent },
    { path: 'transactions/:id', component: TransactionComponent },
    { path: 'transactions/:id/category', component: TransactionCategoryComponent },
    { path: 'transactions/:id/tags', component: TransactionTagsComponent },
    { path: 'split/:id', component: TransactionSplitComponent },

    { path: 'monthly', component: BudgetsMonthlyComponent },
    { path: 'yearly', component: BudgetsYearlyComponent },
    { path: 'needs-transferred', component: BudgetsNeedsTransferredComponent },

    { path: 'budgets', component: BudgetsConfigurationComponent },
    { path: 'budgets/:id', component: BudgetConfigurationComponent },

    { path: 'categories', component: CategoriesComponent },
    { path: 'categories/:id', component: CategoryComponent },

    { path: 'transactions-upload', component: TransactionsUploadComponent },
];
