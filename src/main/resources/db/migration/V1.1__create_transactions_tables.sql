CREATE TABLE bank_transactions (
  id SERIAL PRIMARY KEY,
  account VARCHAR(255) NOT NULL,
  transaction_date date NOT NULL,
  description VARCHAR(255),
  comments VARCHAR(255),
  check_number VARCHAR(255),
  amount int NOT NULL
);

CREATE TABLE transactions (
  id SERIAL PRIMARY KEY,
  bank_transaction_id int references bank_transactions(id),
  account VARCHAR(255) NOT NULL,
  transaction_date date NOT NULL,
  description VARCHAR(255),
  comments VARCHAR(255),
  check_number VARCHAR(255),
  amount int NOT NULL,
  category VARCHAR(255),
  tags VARCHAR(255),
  notes VARCHAR(255)
);