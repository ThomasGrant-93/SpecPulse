-- V7__remove_group_members.sql

-- services.group_id is the single source of truth for group membership.
-- group_members becomes redundant.

DROP TABLE IF EXISTS group_members CASCADE;
