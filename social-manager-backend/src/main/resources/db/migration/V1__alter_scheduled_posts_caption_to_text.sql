-- Flyway migration: alter scheduled_posts.caption to TEXT to allow longer captions
ALTER TABLE scheduled_posts ALTER COLUMN caption TYPE text;
