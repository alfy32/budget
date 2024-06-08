import {Budget} from "./budget";
import {CategoryInfo} from "./categoryInfo";

export interface BudgetInfo {
  total: number,
  expectedTotal: number,
  percent: number,
  expectedPercent: number,
  budget: Budget,
  categories: CategoryInfo[],
}
