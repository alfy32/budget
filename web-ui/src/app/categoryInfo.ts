import {Category} from "./category";
import {Transaction} from "./transaction";

export interface CategoryInfo {
  category: Category,
  total: number,
  transactions: Transaction[],
}
