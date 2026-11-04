-- How a rule's text is compared with the bank description (ignoring upper/lower case):
--   equals       the whole description is the text
--   starts_with  the description begins with the text
--   contains     the text appears anywhere in the description
-- Rules made before this column existed keep matching exactly.
ALTER TABLE categorize_rules ADD COLUMN match_type TEXT NOT NULL DEFAULT 'equals'
    CHECK (match_type IN ('equals', 'starts_with', 'contains'));
