-- V16: keep material subtype attributes (component lead time, finished-product line/warranty) so the twin reloads without data loss
ALTER TABLE materials ADD COLUMN IF NOT EXISTS details_json TEXT;
