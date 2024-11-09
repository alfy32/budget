import {BudgetInfo} from "./budgetInfo";

export interface TransferInfo {
  account: String,
  amount: number,
  budgets: BudgetInfo[],
}
