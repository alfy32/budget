CREATE TABLE bank_transactions (
  id UUID PRIMARY KEY,
  csv VARCHAR(255),
  account VARCHAR(255) NOT NULL,
  transactionDate DATE NOT NULL,
  description VARCHAR(255),
  comments VARCHAR(255),
  checkNumber VARCHAR(255),
  amount INTEGER NOT NULL
);

CREATE TABLE transactions (
  id UUID PRIMARY KEY,
  bankTransactionId UUID NOT NULL,
  account VARCHAR(255) NOT NULL,
  transactionDate DATE NOT NULL,
  description VARCHAR(255),
  amount INTEGER NOT NULL,
  categoryId UUID,
  tags VARCHAR(255),
  notes VARCHAR(255)
);
