-- apply changes
create table rcachievements_achievements (
  id                            varchar(40) not null,
  alias                         varchar(255),
  type                          varchar(255),
  name                          varchar(255),
  description                   varchar(255),
  enabled                       int default 0 not null,
  secret                        int default 0 not null,
  hidden                        int default 0 not null,
  broadcast                     int default 0 not null,
  restricted                    int default 0 not null,
  config                        clob,
  data_id                       varchar(40),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint uq_rcachievements_achievements_data_id unique (data_id),
  constraint uq_rcachievements_achievements_alias unique (alias),
  constraint pk_rcachievements_achievements primary key (id),
  foreign key (data_id) references rcs_datastore (id) on delete restrict on update restrict
);

create table rcachievements_players (
  id                            varchar(40) not null,
  name                          varchar(255),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_rcachievements_players primary key (id)
);

create table rcs_datastore (
  id                            varchar(40) not null,
  data                          clob,
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_rcs_datastore primary key (id)
);

create table rcachievements_player_achievements (
  id                            varchar(40) not null,
  achievement_id                varchar(40) not null,
  player_id                     varchar(40) not null,
  unlocked                      timestamp,
  data_id                       varchar(40),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint uq_rcachievements_player_achievements_data_id unique (data_id),
  constraint pk_rcachievements_player_achievements primary key (id),
  foreign key (achievement_id) references rcachievements_achievements (id) on delete restrict on update restrict,
  foreign key (player_id) references rcachievements_players (id) on delete restrict on update restrict,
  foreign key (data_id) references rcs_datastore (id) on delete restrict on update restrict
);

