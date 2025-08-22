create table if not exists fiat_rates (
                                      id bigserial primary key,
                                      currency varchar(16) not null,
                                      rate double precision not null,
                                      updated_at timestamptz not null
);
create index if not exists idx_fiat_currency on fiat_rates(currency);
create index if not exists idx_fiat_currency_updated_at_desc
    on fiat_rates (currency, updated_at desc);

create table if not exists crypto_rates (
                                        id bigserial primary key,
                                        name varchar(64) not null,
                                        value double precision not null,
                                        updated_at timestamptz not null
);
create index if not exists idx_crypto_name on crypto_rates(name);
create index if not exists idx_crypto_name_updated_at_desc
    on crypto_rates (name, updated_at desc);