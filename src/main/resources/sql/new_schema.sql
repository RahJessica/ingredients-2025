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

CREATE TABLE "order" (
     id SERIAL PRIMARY KEY,
     reference VARCHAR(20) UNIQUE NOT NULL,
     creation_datetime TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE dish_order (
     id SERIAL PRIMARY KEY,
     id_order INTEGER NOT NULL,
     id_dish INTEGER NOT NULL,
     quantity INTEGER NOT NULL CHECK (quantity > 0),

     CONSTRAINT fk_dish_order_order
          FOREIGN KEY (id_order)
          REFERENCES "order"(id)
          ON DELETE CASCADE,

     CONSTRAINT fk_dish_order_dish
        FOREIGN KEY (id_dish)
        REFERENCES dish(id)
        ON DELETE CASCADE,

        CONSTRAINT uq_order_dish UNIQUE (id_order, id_dish)
);

-- Normalisation des données : relation many to many
ALTER TABLE ingredient DROP COLUMN id_dish;