-- Flyway migration: add published_post_url column
ALTER TABLE scheduled_posts ADD COLUMN published_post_url TEXT;
