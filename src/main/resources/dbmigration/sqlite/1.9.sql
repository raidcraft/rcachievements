-- apply changes
alter table rcachievements_player_achievements add column claimed_global_rewards int default 0 not null;
alter table rcachievements_player_achievements add column claimed_rewards int default 0 not null;

