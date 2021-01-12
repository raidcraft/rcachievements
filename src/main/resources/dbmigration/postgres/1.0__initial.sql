-- apply changes
create table rcachievements_achievements (
  id                            uuid not null,
  alias                         varchar(255),
  type                          varchar(255),
  name                          varchar(255),
  description                   varchar(255),
  enabled                       boolean default false not null,
  secret                        boolean default false not null,
  hidden                        boolean default false not null,
  broadcast                     boolean default false not null,
  restricted                    boolean default false not null,
  config                        json,
  data_id                       uuid,
  version                       bigint not null,
  when_created                  timestamptz not null,
  when_modified                 timestamptz not null,
  constraint uq_rcachievements_achievements_data_id unique (data_id),
  constraint uq_rcachievements_achievements_alias unique (alias),
  constraint pk_rcachievements_achievements primary key (id)
);

create table rcachievements_players (
  id                            uuid not null,
  name                          varchar(255),
  version                       bigint not null,
  when_created                  timestamptz not null,
  when_modified                 timestamptz not null,
  constraint pk_rcachievements_players primary key (id)
);

create table rcachievements_datastore (
  id                            uuid not null,
  data                          json,
  version                       bigint not null,
  when_created                  timestamptz not null,
  when_modified                 timestamptz not null,
  constraint pk_rcachievements_datastore primary key (id)
);

create table rcachievements_player_achievements (
  id                            uuid not null,
  achievement_id                uuid not null,
  player_id                     uuid not null,
  unlocked                      timestamptz,
  data_id                       uuid,
  version                       bigint not null,
  when_created                  timestamptz not null,
  when_modified                 timestamptz not null,
  constraint uq_rcachievements_player_achievements_data_id unique (data_id),
  constraint pk_rcachievements_player_achievements primary key (id)
);

alter table rcachievements_achievements add constraint fk_rcachievements_achievements_data_id foreign key (data_id) references rcachievements_datastore (id) on delete restrict on update restrict;

create index ix_rcachievements_player_achievements_achievement_id on rcachievements_player_achievements (achievement_id);
alter table rcachievements_player_achievements add constraint fk_rcachievements_player_achievements_achievement_id foreign key (achievement_id) references rcachievements_achievements (id) on delete restrict on update restrict;

create index ix_rcachievements_player_achievements_player_id on rcachievements_player_achievements (player_id);
alter table rcachievements_player_achievements add constraint fk_rcachievements_player_achievements_player_id foreign key (player_id) references rcachievements_players (id) on delete restrict on update restrict;

alter table rcachievements_player_achievements add constraint fk_rcachievements_player_achievements_data_id foreign key (data_id) references rcachievements_datastore (id) on delete restrict on update restrict;

