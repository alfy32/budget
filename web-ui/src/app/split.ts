import {BankTransaction} from "./bankTransaction";
import {SplitTransaction} from "./splitTransaction";

export interface Split {
    bankTransaction?: BankTransaction,
    transactions: SplitTransaction[],
}
