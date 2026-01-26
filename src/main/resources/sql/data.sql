insert into dish (id, name, dish_type)
values (1, 'Salaide fraîche', 'STARTER'),
       (2, 'Poulet grillé', 'MAIN'),
       (3, 'Riz aux légumes', 'MAIN'),
       (4, 'Gâteau au chocolat ', 'DESSERT'),
       (5, 'Salade de fruits', 'DESSERT');


insert into ingredient (id, name, category, price, id_dish)
values (1, 'Laitue', 'VEGETABLE', 800.0, 1),
       (2, 'Tomate', 'VEGETABLE', 600.0, 1),
       (3, 'Poulet', 'ANIMAL', 4500.0, 2),
       (4, 'Chocolat ', 'OTHER', 3000.0, 4),
       (5, 'Beurre', 'DAIRY', 2500.0, 4);

insert into dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
values (1, 1, 1, 0.20, 'KG'),
       (2, 1, 2, 0.15, 'KG'),
       (3, 2, 3, 1.00, 'KG'),
       (4, 4, 4, 0.30, 'KG'),
       (5, 4, 5, 0.20, 'KG');

insert into stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime)
values (1, 1, 5.0, 'IN', 'KG', '2024-01-05 08:00'),
        (2, 1, 0.2, 'IN', 'KG', '2024-01-05 08:00'),
        (3, 2, 4.0, 'IN', 'KG', '2024-01-05 08:00'),
        (4, 2, 0.15, 'IN', 'KG', '2024-01-05 08:00'),
        (5, 3, 10.0, 'IN', 'KG', '2024-01-05 08:00'),
        (6, 3, 1.0, 'IN', 'KG', '2024-01-05 08:00'),
        (7, 4, 3.0, 'IN', 'KG', '2024-01-05 08:00'),
        (8, 4, 0.3, 'IN', 'KG', '2024-01-05 08:00'),
        (9, 5, 2.5, 'IN', 'KG', '2024-01-05 08:00'),
        (10, 5, 0.2, 'IN', 'KG', '2024-01-05 08:00');


update dish set price = 2000.0
where id = 1;

update dish set price = 6000.0
where id = 2;

