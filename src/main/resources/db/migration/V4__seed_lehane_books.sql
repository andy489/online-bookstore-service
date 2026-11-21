-- Seed Dennis Lehane's top 3 books into Crime & Mystery

INSERT INTO books (title, author, description, price, stock, category_id, deleted, version, created_at, updated_at)
VALUES ('Mystic River',
        'Dennis Lehane',
        'Three childhood friends from a Boston neighbourhood are reunited by tragedy when one of their daughters is murdered. A devastating exploration of trauma, guilt, and the past that never lets go.',
        13.99, 35,
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('Gone Baby Gone',
        'Dennis Lehane',
        'Private detectives Patrick Kenzie and Angela Gennaro search for a missing four-year-old girl in a Boston working-class neighbourhood, uncovering layers of corruption and moral ambiguity.',
        12.99, 30,
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('Shutter Island',
        'Dennis Lehane',
        'U.S. Marshal Teddy Daniels investigates the disappearance of a patient from Ashecliffe Hospital on remote Shutter Island — but nothing on the island is what it seems.',
        13.49, 40,
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW());
