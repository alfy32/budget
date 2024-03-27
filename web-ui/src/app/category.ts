import {Budget} from "./budget";

export interface Category {
  id: string,
  name: string,
  budget?: Budget
}
