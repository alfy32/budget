export interface BankTransaction {
  id: string,
  csv?: string,
  account: string,
  transactionType: string,
  transactionDate: string,
  description: string,
  comments?: string,
  checkNumber?: string,
  amount: number,
}
