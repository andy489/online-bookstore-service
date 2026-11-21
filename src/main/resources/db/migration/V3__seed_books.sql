-- Seed categories and famous books for local development.

INSERT INTO categories (name, description)
VALUES ('Classic Literature', 'Timeless works of literary fiction'),
       ('War & Historical Fiction', 'Stories set against the backdrop of war and history'),
       ('Crime & Mystery', 'Detective fiction, thrillers, and crime novels'),
       ('Philosophy & Drama', 'Philosophical works and dramatic literature'),
       ('Epic & Adventure', 'Large-scale adventure stories and epic sagas');

-- Classic Literature
INSERT INTO books (title, author, description, price, stock, category_id, deleted, version, created_at, updated_at)
VALUES ('The Brothers Karamazov',
        'Fyodor Dostoevsky',
        'A passionate philosophical novel set in 19th-century Russia, exploring faith, doubt, morality, and the existence of God through the story of the Karamazov family.',
        14.99, 40,
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW()),

       ('Crime and Punishment',
        'Fyodor Dostoevsky',
        'A young student murders a pawnbroker and grapples with guilt, morality, and redemption in Tsarist St. Petersburg.',
        12.99, 35,
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW()),

       ('The Idiot',
        'Fyodor Dostoevsky',
        'Prince Myshkin, a kind and guileless man, returns to Russia and is caught in a web of passion, jealousy, and tragedy.',
        13.49, 30,
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW()),

       ('The Master and Margarita',
        'Mikhail Bulgakov',
        'The Devil visits Soviet Moscow with his retinue, causing chaos, while a parallel narrative retells the story of Pontius Pilate and Jesus.',
        13.99, 25,
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW());

-- War & Historical Fiction
INSERT INTO books (title, author, description, price, stock, category_id, deleted, version, created_at, updated_at)
VALUES ('Catch-22',
        'Joseph Heller',
        'A darkly comic masterpiece set during World War II, following bombardier Yossarian as he tries to get out of flying more missions by proving he is insane — but the catch is that wanting to avoid danger proves he is sane.',
        13.99, 45,
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('Shogun',
        'James Clavell',
        'An English navigator is shipwrecked in feudal Japan and becomes entangled in the dangerous power struggles of samurai lords.',
        16.99, 30,
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('King Rat',
        'James Clavell',
        'Set in Changi prisoner-of-war camp in Singapore during World War II, a story of survival, moral compromise, and the will to live.',
        12.99, 25,
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('Tai-Pan',
        'James Clavell',
        'The founding of Hong Kong in 1841 seen through the eyes of Dirk Struan, the most powerful trader in the Far East.',
        15.99, 20,
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('For Whom the Bell Tolls',
        'Ernest Hemingway',
        'An American volunteer fights with the Republican guerrillas in the Spanish Civil War and falls in love behind enemy lines.',
        11.99, 35,
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('All Quiet on the Western Front',
        'Erich Maria Remarque',
        'A young German soldier experiences the brutal reality of World War I, stripping away all illusions of glory and heroism.',
        10.99, 50,
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW());

-- Crime & Mystery
INSERT INTO books (title, author, description, price, stock, category_id, deleted, version, created_at, updated_at)
VALUES ('The Girl with the Dragon Tattoo',
        'Stieg Larsson',
        'Journalist Mikael Blomkvist and hacker Lisbeth Salander investigate a decades-old disappearance within a wealthy Swedish family.',
        14.49, 40,
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('And Then There Were None',
        'Agatha Christie',
        'Ten strangers are lured to an island and murdered one by one, each death mirroring a nursery rhyme.',
        9.99, 60,
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('The Name of the Rose',
        'Umberto Eco',
        'A medieval monk investigates a series of mysterious deaths in an Italian abbey, uncovering a labyrinthine conspiracy.',
        13.99, 25,
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW());

-- Philosophy & Drama
INSERT INTO books (title, author, description, price, stock, category_id, deleted, version, created_at, updated_at)
VALUES ('The Stranger',
        'Albert Camus',
        'Meursault, an indifferent Frenchman in Algeria, kills an Arab man and faces trial — not for the murder, but for his emotional detachment.',
        10.49, 45,
        (SELECT id FROM categories WHERE name = 'Philosophy & Drama'),
        FALSE, 0, NOW(), NOW()),

       ('Thus Spoke Zarathustra',
        'Friedrich Nietzsche',
        'Nietzsche''s philosophical novel presents the ideas of the Übermensch, the death of God, and eternal recurrence through the prophet Zarathustra.',
        11.99, 30,
        (SELECT id FROM categories WHERE name = 'Philosophy & Drama'),
        FALSE, 0, NOW(), NOW()),

       ('Nausea',
        'Jean-Paul Sartre',
        'Antoine Roquentin, a historian in a French port city, is overwhelmed by a profound sense of existential disgust at the mere existence of things.',
        10.99, 20,
        (SELECT id FROM categories WHERE name = 'Philosophy & Drama'),
        FALSE, 0, NOW(), NOW());

-- Epic & Adventure
INSERT INTO books (title, author, description, price, stock, category_id, deleted, version, created_at, updated_at)
VALUES ('Dune',
        'Frank Herbert',
        'On the desert planet Arrakis, young Paul Atreides navigates political intrigue, religion, and ecology to fulfil a messianic destiny.',
        15.99, 50,
        (SELECT id FROM categories WHERE name = 'Epic & Adventure'),
        FALSE, 0, NOW(), NOW()),

       ('The Count of Monte Cristo',
        'Alexandre Dumas',
        'Edmond Dantès, wrongfully imprisoned, escapes and reinvents himself as the mysterious Count of Monte Cristo to exact an elaborate revenge.',
        14.49, 35,
        (SELECT id FROM categories WHERE name = 'Epic & Adventure'),
        FALSE, 0, NOW(), NOW()),

       ('Don Quixote',
        'Miguel de Cervantes',
        'A Spanish gentleman driven mad by chivalric romances sets out as a knight-errant with his squire Sancho Panza, tilting at windmills.',
        12.99, 25,
        (SELECT id FROM categories WHERE name = 'Epic & Adventure'),
        FALSE, 0, NOW(), NOW()),

       ('One Hundred Years of Solitude',
        'Gabriel García Márquez',
        'Seven generations of the Buendía family in the fictional town of Macondo, blending magical realism with the history of Latin America.',
        13.99, 30,
        (SELECT id FROM categories WHERE name = 'Epic & Adventure'),
        FALSE, 0, NOW(), NOW());
