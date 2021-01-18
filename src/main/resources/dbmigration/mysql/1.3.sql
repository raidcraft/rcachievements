-- apply changes
alter table rcachievements_achievements add column global_rewards tinyint(1) default 1 not null;

