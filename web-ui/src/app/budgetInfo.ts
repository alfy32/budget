import {Budget} from "./budget";
import {CategoryInfo} from "./categoryInfo";

export interface BudgetInfo {
  total: number,
  percent: number,
  budget: Budget,
  categories: CategoryInfo[],
}
