-- apply changes
create table rcachievements_placed_blocks (
  id                            uuid not null,
  world                         uuid,
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  type                          varchar(255),
  placed_by                     uuid,
  version                       bigint not null,
  when_created                  timestamptz not null,
  when_modified                 timestamptz not null,
  constraint uq_rcachievements_placed_blocks_world_x_y_z unique (world,x,y,z),
  constraint pk_rcachievements_placed_blocks primary key (id)
);

create index ix_rcachievements_placed_blocks_world on rcachievements_placed_blocks (world);
create index ix_rcachievements_placed_blocks_x on rcachievements_placed_blocks (x);
create index ix_rcachievements_placed_blocks_y on rcachievements_placed_blocks (y);
create index ix_rcachievements_placed_blocks_z on rcachievements_placed_blocks (z);
