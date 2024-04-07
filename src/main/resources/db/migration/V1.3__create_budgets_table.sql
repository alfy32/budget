CREATE TABLE budgets (
  id uuid PRIMARY KEY,
  name varchar(255) NOT NULL,
  amount integer DEFAULT 0 NOT NULL
);