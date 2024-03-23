export interface BankTransaction {
    id: string,
    csv?: string,
    account: string,
    transactionDate: string,
    description: string,
    comments?: string,
    checkNumber?: string,
    amount: number,
}