import {BankTransaction} from "./bankTransaction";
import {Category} from "./category";

export interface Transaction {
  id: string,
  bankTransaction?: BankTransaction,
  splitIndex: number,
  account: string,
  transactionType: string,
  transactionDate: string,
  description: string,
  amount: number,
  category?: Category,
  tags?: string,
  notes?: string,
  needsTransferred: boolean,
}
