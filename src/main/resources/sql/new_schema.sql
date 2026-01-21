CREATE TYPE unit_type AS ENUM ('PCS', 'KG', 'L');
CREATE TYPE movement_type AS ENUM ('IN', 'OUT');

CREATE TABLE dish_ingredient
(
    id                SERIAL PRIMARY KEY,
    id_dish           INT       NOT NULL,
    id_ingredient     INT       NOT NULL,
    quantity_required NUMERIC   NOT NULL,
    unit              unit_type NOT NULL,

    CONSTRAINT fk_dish
        FOREIGN KEY (id_dish)
            REFERENCES dish (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_ingredient
        FOREIGN KEY (id_ingredient)
            REFERENCES ingredient (id)
            ON DELETE CASCADE
);

CREATE TABLE stock_movement (
    id SERIAL PRIMARY KEY,
    id_ingredient INT NOT NULL,
    quantity NUMERIC NOT NULL,
    unit unit_type NOT NULL,
    type movement_type NOT NULL,
    creation_datetime TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_stock_ingredient
    FOREIGN KEY (id_ingredient) REFERENCES ingredient(id)
    ON DELETE CASCADE
);

-- Normalisation des données : relation many to many
ALTER TABLE ingredient DROP COLUMN id_dish;