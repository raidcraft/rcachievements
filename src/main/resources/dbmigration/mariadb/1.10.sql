-- apply changes
alter table rcachievements_achievements add column show_progress tinyint(1) default 1 not null;

