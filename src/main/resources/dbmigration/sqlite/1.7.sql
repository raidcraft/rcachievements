-- apply changes
create table rcachievements_placed_blocks (
  id                            varchar(40) not null,
  world                         varchar(40),
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  type                          varchar(255),
  placed_by                     varchar(40),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint uq_rcachievements_placed_blocks_world_x_y_z unique (world,x,y,z),
  constraint pk_rcachievements_placed_blocks primary key (id)
);

create index ix_rcachievements_placed_blocks_world on rcachievements_placed_blocks (world);
create index ix_rcachievements_placed_blocks_x on rcachievements_placed_blocks (x);
create index ix_rcachievements_placed_blocks_y on rcachievements_placed_blocks (y);
create index ix_rcachievements_placed_blocks_z on rcachievements_placed_blocks (z);
