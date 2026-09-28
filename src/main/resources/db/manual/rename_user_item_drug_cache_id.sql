-- Run this once in MySQL if user_item already has the old sfda_drug_id column.
-- Run it before restarting Jura so Hibernate does not add a second column.
ALTER TABLE user_item CHANGE COLUMN sfda_drug_id drug_cache_id INT NULL;
