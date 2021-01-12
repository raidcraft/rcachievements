-- apply changes
create table rcachievements_achievements (
  id                            varchar(40) not null,
  alias                         varchar(255),
  type                          varchar(255),
  name                          varchar(255),
  description                   varchar(255),
  enabled                       tinyint(1) default 0 not null,
  secret                        tinyint(1) default 0 not null,
  hidden                        tinyint(1) default 0 not null,
  broadcast                     tinyint(1) default 0 not null,
  restricted                    tinyint(1) default 0 not null,
  config                        json,
  data_id                       varchar(40),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint uq_rcachievements_achievements_data_id unique (data_id),
  constraint uq_rcachievements_achievements_alias unique (alias),
  constraint pk_rcachievements_achievements primary key (id)
);

create table rcachievements_players (
  id                            varchar(40) not null,
  name                          varchar(255),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint pk_rcachievements_players primary key (id)
);

create table rcs_datastore (
  id                            varchar(40) not null,
  data                          json,
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint pk_rcs_datastore primary key (id)
);

create table rcachievements_player_achievements (
  id                            varchar(40) not null,
  achievement_id                varchar(40) not null,
  player_id                     varchar(40) not null,
  unlocked                      datetime(6),
  data_id                       varchar(40),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint uq_rcachievements_player_achievements_data_id unique (data_id),
  constraint pk_rcachievements_player_achievements primary key (id)
);

alter table rcachievements_achievements add constraint fk_rcachievements_achievements_data_id foreign key (data_id) references rcs_datastore (id) on delete restrict on update restrict;

create index ix_rcachievements_player_achievements_achievement_id on rcachievements_player_achievements (achievement_id);
alter table rcachievements_player_achievements add constraint fk_rcachievements_player_achievements_achievement_id foreign key (achievement_id) references rcachievements_achievements (id) on delete restrict on update restrict;

create index ix_rcachievements_player_achievements_player_id on rcachievements_player_achievements (player_id);
alter table rcachievements_player_achievements add constraint fk_rcachievements_player_achievements_player_id foreign key (player_id) references rcachievements_players (id) on delete restrict on update restrict;

alter table rcachievements_player_achievements add constraint fk_rcachievements_player_achievements_data_id foreign key (data_id) references rcs_datastore (id) on delete restrict on update restrict;

