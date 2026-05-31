create table if not exists inventory_items (
    id varchar(64) primary key,
    name varchar(255) not null,
    sku varchar(128) not null unique,
    category varchar(128) not null,
    unit varchar(32) not null,
    quantity numeric(14, 3) not null check (quantity >= 0),
    min_quantity numeric(14, 3) not null check (min_quantity >= 0),
    department varchar(128) not null,
    shelf varchar(64) not null,
    cell varchar(64) not null,
    expected_delivery_date varchar(10),
    expected_delivery_quantity numeric(14, 3) check (
        expected_delivery_quantity is null or expected_delivery_quantity > 0
    ),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index if not exists idx_inventory_items_name on inventory_items (name);
create index if not exists idx_inventory_items_sku on inventory_items (sku);
create index if not exists idx_inventory_items_category on inventory_items (category);

create table if not exists employees (
    id varchar(64) primary key,
    full_name varchar(255) not null,
    login varchar(128) not null unique,
    role varchar(32) not null check (role in ('admin', 'worker')),
    status varchar(32) not null check (status in ('pending_activation', 'active')),
    temporary_password varchar(64),
    password_hash varchar(512),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    check (
        (status = 'pending_activation' and temporary_password is not null)
        or
        (status = 'active' and temporary_password is null and password_hash is not null)
    )
);

create index if not exists idx_employees_full_name on employees (full_name);
create index if not exists idx_employees_login on employees (login);
