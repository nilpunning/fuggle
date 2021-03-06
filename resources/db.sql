-- name: time-of-day
SELECT timeofday();

-- name: user-password-where-email
SELECT id, password FROM "user" WHERE email = :email;

-- name: search-recipe
SELECT
  r.id,
  r.title,
  r.photo_name,
  ts_headline(
    rtrim(
      regexp_replace(
        concat_ws(
            ', ',
            string_agg(category.category, ', '),
            r.source,
            r.yield,
            r.prep_time,
            r.cooking_time,
            r.ingredients,
            r.tools,
            r.notes,
            r.directions
        ),
        '(, )+',
        ', ',
        'g'
      ),
      ', '
    ),
    r.q,
    'MaxFragments=1, MinWords=5, MaxWords=25'
  ) AS headline
FROM (
  SELECT recipe.id, recipe.title, recipe.photo_name, recipe.source,
  recipe.yield, recipe.prep_time, recipe.cooking_time, recipe.ingredients,
  recipe.tools, recipe.notes, recipe.directions, recipe.tsv, q
  FROM recipe, plainto_tsquery(:query) AS q
  WHERE user_id = :user_id
  AND tsv @@ q
) AS r
LEFT OUTER JOIN category_to_recipe ON r.id = category_to_recipe.recipe_id
LEFT JOIN category ON category.id = category_to_recipe.category_id
GROUP BY r.id, r.title, r.photo_name, r.source, r.yield, r.prep_time,
r.cooking_time, r.ingredients, r.tools, r.notes, r.directions, r.q, r.tsv
ORDER BY ts_rank_cd(r.tsv, r.q) DESC;

-- name: drop-table-recipe!
DROP TABLE recipe;

-- name: insert-recipe<!
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
    :user_id,
    :title,
    :source,
    :yield,
    :prep_time,
    :cooking_time,
    :ingredients,
    :tools,
    :notes,
    :directions);

-- name: update-recipe<!
UPDATE recipe SET
    last_modified = now(),
    title = :title,
    source = :source,
    yield = :yield,
    prep_time = :prep_time,
    cooking_time = :cooking_time,
    ingredients = :ingredients,
    tools = :tools,
    notes = :notes,
    directions = :directions
WHERE user_id = :user_id AND id = :id;

-- name: update-recipe-photo<!
UPDATE recipe SET
    photo_name = :photo_name,
    photo = :photo
WHERE user_id = :user_id and id = :id;

-- name: update-recipe-tsv!
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
    WHERE recipe.id = :id
    GROUP BY recipe.id
  )
WHERE recipe.id = :id;

-- name: recipe-titles
SELECT category_to_recipe.category_id, category.category, recipe.id,
recipe.title, recipe.photo_name
FROM recipe
LEFT OUTER JOIN category_to_recipe ON recipe.id = category_to_recipe.recipe_id
LEFT JOIN category ON category.id = category_to_recipe.category_id
WHERE recipe.user_id = :user_id
GROUP BY category_to_recipe.category_id, category.category, recipe.id
ORDER BY category.category, recipe.title NULLS LAST;

-- name: recipe-by-id
SELECT recipe.id, recipe.title, recipe.photo_name, recipe.source, recipe.yield,
recipe.prep_time, recipe.cooking_time, recipe.ingredients, recipe.tools,
recipe.notes, recipe.directions
FROM recipe
WHERE user_id = :user_id AND id = :id;

-- name: recipe-photo-by-id
SELECT recipe.photo
FROM recipe
WHERE user_id = :user_id
AND id = :id
AND recipe.photo_name = photo_name;

-- name: recipe-id-by-category-id
SELECT recipe_id FROM category_to_recipe
WHERE category_id = :category_id;

-- name: delete-recipe!
DELETE FROM recipe
WHERE user_id = :user_id AND id = :id;

-- name: categories
SELECT * FROM category
WHERE user_id = :user_id
ORDER BY id DESC;

-- name: insert-category!
INSERT INTO category (
    user_id,
    category)
VALUES (
    :user_id,
    :category);

-- name: update-category!
UPDATE category SET
    category = :category
WHERE user_id = :user_id AND id = :id;

-- name: delete-category!
DELETE FROM category
WHERE user_id = :user_id AND id = :id;

-- name: categories-to-recipe
SELECT category.id, category.category FROM category, category_to_recipe
WHERE category.id = category_to_recipe.category_id
AND category_to_recipe.recipe_id = :recipe_id;

-- name: category-ids-to-recipe
SELECT category.id FROM category, category_to_recipe
WHERE category.id = category_to_recipe.category_id
AND category_to_recipe.recipe_id = :recipe_id;

-- name: insert-category-to-recipe!
INSERT INTO category_to_recipe (
    category_id,
    recipe_id)
VALUES (
    :category_id,
    :recipe_id);

-- name: delete-category-to-recipe!
DELETE FROM category_to_recipe
WHERE category_id = :category_id AND recipe_id = :recipe_id;

-- name: update-category<!
UPDATE category SET
    category = :category
WHERE user_id = :user_id AND id = :id;

-- name: drop-category-index!
DROP INDEX category_index;

-- name: drop-category!
DROP TABLE category;

-- name: drop-category-to-recipe!
DROP TABLE category_to_recipe;