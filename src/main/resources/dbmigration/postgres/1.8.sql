-- apply changes
alter table rcachievements_achievements add column delayed_broadcast boolean default false not null;

