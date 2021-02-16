-- apply changes
alter table rcachievements_achievements add column delayed_broadcast int default 0 not null;

