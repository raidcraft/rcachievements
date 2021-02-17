-- apply changes
alter table rcachievements_player_achievements add column claimed_global_rewards boolean default false not null;
alter table rcachievements_player_achievements add column claimed_rewards boolean default false not null;

