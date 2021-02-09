-- apply changes
alter table rcachievements_achievements add column parent_id varchar(40);

create index ix_rcachievements_achievements_parent_id on rcachievements_achievements (parent_id);

