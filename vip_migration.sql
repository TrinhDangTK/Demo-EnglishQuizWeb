-- ============================================================
-- VIP TIER MIGRATION
-- Run this AFTER restarting the app (Hibernate will add the columns).
-- This sets the Listening category (id=4) as VIP-only.
-- ============================================================

-- Mark Listening category as VIP-only
UPDATE category SET vip_only = TRUE WHERE id = 4;

-- All existing users default to Normal (tier=1), already handled by column default.
-- To manually grant VIP to a specific user:
-- UPDATE user_account SET tier = 2 WHERE username = 'your_username';
