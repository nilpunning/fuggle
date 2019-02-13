#!/bin/bash

set -e

psql -v ON_ERROR_STOP=1 --username "docker" --dbname "fuggle" <<-'EOSQL'
    INSERT INTO "user" (email, password, first_name, last_name) VALUES (
        'fake@email.com',
        '$2a$10$zU3MbqTY1PQ1Gsba3lLGe.UtottwLAxXgtqbzl2ozPYvU3tI74c3m',
        'Big',
        'Fake');
    
    INSERT INTO recipe (
        user_id,
        title,
        source,
        yield,
        prep_time,
        cooking_time,
        ingredients,
        tools,
        notes,
        directions)
    VALUES (
        1,
        'Pancakes',
        '',
        '8 pancakes',
        '5 minutes',
        '15 minutes',
        '1 1/2 cups flour
3 1/2 teaspoons baking powder
1 teaspoon salt
1 teaspoon sugar
1 1/4 cups milk
1 egg
3 tablespoons butter',
        'griddle
spatula',
        '',
        'Mix ingredients together.
On lightly oiled griddle over medium heat scoop 1/4 cup for each pancake.
Cook both sides until brown.');

UPDATE recipe SET
  tsv = (
    SELECT
        setweight(to_tsvector(COALESCE(recipe.title, '')), 'A') ||
        setweight(to_tsvector(COALESCE(string_agg(category.category, ' '), '')), 'B') ||
        setweight(to_tsvector(COALESCE(recipe.source, '')), 'C') ||
        setweight(to_tsvector(COALESCE(recipe.yield, '')), 'C') ||
        setweight(to_tsvector(COALESCE(recipe.prep_time, '')), 'C') ||
        setweight(to_tsvector(COALESCE(recipe.cooking_time, '')), 'C') ||
        setweight(to_tsvector(COALESCE(recipe.ingredients, '')), 'B') ||
        setweight(to_tsvector(COALESCE(recipe.tools, '')), 'C') ||
        setweight(to_tsvector(COALESCE(recipe.notes, '')), 'C') ||
        setweight(to_tsvector(COALESCE(recipe.directions, '')), 'C')
    FROM recipe
    LEFT OUTER JOIN category_to_recipe ON recipe.id = category_to_recipe.recipe_id
    LEFT JOIN category ON category.id = category_to_recipe.category_id
    WHERE recipe.id = 1
    GROUP BY recipe.id
  )
WHERE recipe.id = 1;
EOSQL