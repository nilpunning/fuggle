#!/bin/bash

set -e

psql -v ON_ERROR_STOP=1 --username "docker" --dbname "fuggle" <<-'EOSQL'
    CREATE TABLE "user" (
        id bigserial PRIMARY KEY,
        email VARCHAR(256) UNIQUE NOT NULL,
        password VARCHAR(128) NOT NULL,
        last_name VARCHAR(256) NOT NULL,
        first_name VARCHAR(256) NOT NULL);

    CREATE TABLE recipe (
        id bigserial PRIMARY KEY,
        user_id bigint REFERENCES "user" NOT NULL,
        last_modified TIMESTAMP NOT NULL DEFAULT now(),
        title TEXT,
        photo_name TEXT,
        photo bytea,
        source TEXT,
        yield TEXT,
        prep_time TEXT,
        cooking_time TEXT,
        ingredients TEXT,
        tools TEXT,
        notes TEXT,
        directions TEXT,
        tsv tsvector);

    CREATE INDEX tsv_idx ON recipe USING gin(tsv);
    CREATE INDEX recipe_index ON recipe (user_id);

    CREATE TABLE category (
        id bigserial PRIMARY KEY,
        user_id bigint REFERENCES "user" NOT NULL,
        category TEXT);

    CREATE INDEX category_index ON category (user_id);

    CREATE TABLE category_to_recipe (
        category_id bigint REFERENCES category ON DELETE CASCADE NOT NULL,
        recipe_id bigint REFERENCES recipe ON DELETE CASCADE NOT NULL,
        primary key (category_id, recipe_id));
EOSQL