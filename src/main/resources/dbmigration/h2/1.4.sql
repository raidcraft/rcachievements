-- apply changes
alter table rcachievements_achievements add column parent_id uuid;

create index ix_rcachievements_achievements_parent_id on rcachievements_achievements (parent_id);
alter table rcachievements_achievements add constraint fk_rcachievements_achievements_parent_id foreign key (parent_id) references rcachievements_achievements (id) on delete restrict on update restrict;

