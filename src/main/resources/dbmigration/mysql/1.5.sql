-- apply changes
create table rcachievements_categories (
  id                            varchar(40) not null,
  alias                         varchar(255),
  name                          varchar(255),
  description                   json,
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint uq_rcachievements_categories_alias unique (alias),
  constraint pk_rcachievements_categories primary key (id)
);

alter table rcachievements_achievements add column category_id varchar(40);

create index ix_rcachievements_achievements_category_id on rcachievements_achievements (category_id);
alter table rcachievements_achievements add constraint fk_rcachievements_achievements_category_id foreign key (category_id) references rcachievements_categories (id) on delete restrict on update restrict;

