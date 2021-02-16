-- apply changes
alter table rcachievements_achievements add column delayed_broadcast tinyint(1) default 0 not null;

