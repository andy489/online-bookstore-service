-- Seed all categories and books for local development.
-- All 6 categories and 23 books are seeded in one pass with Cloudinary cover URLs inline.

INSERT INTO categories (name, description)
VALUES ('Classic Literature',      'Timeless works of literary fiction'),
       ('War & Historical Fiction', 'Stories set against the backdrop of war and history'),
       ('Crime & Mystery',          'Detective fiction, thrillers, and crime novels'),
       ('Philosophy & Drama',       'Philosophical works and dramatic literature'),
       ('Epic & Adventure',         'Large-scale adventure stories and epic sagas'),
       ('Science & Non-Fiction',    'Popular science, evolutionary biology, and factual writing');

-- Classic Literature
INSERT INTO books (title, author, description, price, stock, cover_image_url, category_id, deleted, version, created_at, updated_at)
VALUES ('The Brothers Karamazov',
        'Fyodor Dostoevsky',
        'A passionate philosophical novel set in 19th-century Russia, exploring faith, doubt, morality, and the existence of God through the story of the Karamazov family.',
        14.99, 40,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/brothers-karamazov.jpg',
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW()),

       ('Crime and Punishment',
        'Fyodor Dostoevsky',
        'A young student murders a pawnbroker and grapples with guilt, morality, and redemption in Tsarist St. Petersburg.',
        12.99, 35,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/crime-and-punishment.jpg',
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW()),

       ('The Idiot',
        'Fyodor Dostoevsky',
        'Prince Myshkin, a kind and guileless man, returns to Russia and is caught in a web of passion, jealousy, and tragedy.',
        13.49, 30,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-idiot.jpg',
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW()),

       ('The Master and Margarita',
        'Mikhail Bulgakov',
        'The Devil visits Soviet Moscow with his retinue, causing chaos, while a parallel narrative retells the story of Pontius Pilate and Jesus.',
        13.99, 25,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/master-and-margarita.jpg',
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW()),

       ('Brave New World',
        'Aldous Huxley',
        'In a future World State, human beings are genetically engineered and socially conditioned from birth. Bernard Marx begins to question the shallow happiness of a society built on pleasure, conformity, and the elimination of free will.',
        11.99, 45,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/brave-new-world.jpg',
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW());

-- War & Historical Fiction
INSERT INTO books (title, author, description, price, stock, cover_image_url, category_id, deleted, version, created_at, updated_at)
VALUES ('Catch-22',
        'Joseph Heller',
        'A darkly comic masterpiece set during World War II, following bombardier Yossarian as he tries to get out of flying more missions by proving he is insane — but the catch is that wanting to avoid danger proves he is sane.',
        13.99, 45,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/catch-22.jpg',
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('Shogun',
        'James Clavell',
        'An English navigator is shipwrecked in feudal Japan and becomes entangled in the dangerous power struggles of samurai lords.',
        16.99, 30,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/shogun.jpg',
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('King Rat',
        'James Clavell',
        'Set in Changi prisoner-of-war camp in Singapore during World War II, a story of survival, moral compromise, and the will to live.',
        12.99, 25,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/king-rat.jpg',
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('Tai-Pan',
        'James Clavell',
        'The founding of Hong Kong in 1841 seen through the eyes of Dirk Struan, the most powerful trader in the Far East.',
        15.99, 20,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/tai-pan.jpg',
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('For Whom the Bell Tolls',
        'Ernest Hemingway',
        'An American volunteer fights with the Republican guerrillas in the Spanish Civil War and falls in love behind enemy lines.',
        11.99, 35,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/for-whom-the-bell-tolls.jpg',
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW()),

       ('All Quiet on the Western Front',
        'Erich Maria Remarque',
        'A young German soldier experiences the brutal reality of World War I, stripping away all illusions of glory and heroism.',
        10.99, 50,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/all-quiet-western-front.jpg',
        (SELECT id FROM categories WHERE name = 'War & Historical Fiction'),
        FALSE, 0, NOW(), NOW());

-- Crime & Mystery
INSERT INTO books (title, author, description, price, stock, cover_image_url, category_id, deleted, version, created_at, updated_at)
VALUES ('The Girl with the Dragon Tattoo',
        'Stieg Larsson',
        'Journalist Mikael Blomkvist and hacker Lisbeth Salander investigate a decades-old disappearance within a wealthy Swedish family.',
        14.49, 40,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/girl-with-dragon-tattoo.jpg',
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('And Then There Were None',
        'Agatha Christie',
        'Ten strangers are lured to an island and murdered one by one, each death mirroring a nursery rhyme.',
        9.99, 60,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/and-then-there-were-none.jpg',
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('The Name of the Rose',
        'Umberto Eco',
        'A medieval monk investigates a series of mysterious deaths in an Italian abbey, uncovering a labyrinthine conspiracy.',
        13.99, 25,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/name-of-the-rose.jpg',
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('Mystic River',
        'Dennis Lehane',
        'Three childhood friends from a Boston neighbourhood are reunited by tragedy when one of their daughters is murdered. A devastating exploration of trauma, guilt, and the past that never lets go.',
        13.99, 35,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/mystic-river.jpg',
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('Gone Baby Gone',
        'Dennis Lehane',
        'Private detectives Patrick Kenzie and Angela Gennaro search for a missing four-year-old girl in a Boston working-class neighbourhood, uncovering layers of corruption and moral ambiguity.',
        12.99, 30,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/gone-baby-gone.jpg',
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW()),

       ('Shutter Island',
        'Dennis Lehane',
        'U.S. Marshal Teddy Daniels investigates the disappearance of a patient from Ashecliffe Hospital on remote Shutter Island — but nothing on the island is what it seems.',
        13.49, 40,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/shutter-island.jpg',
        (SELECT id FROM categories WHERE name = 'Crime & Mystery'),
        FALSE, 0, NOW(), NOW());

-- Philosophy & Drama
INSERT INTO books (title, author, description, price, stock, cover_image_url, category_id, deleted, version, created_at, updated_at)
VALUES ('The Stranger',
        'Albert Camus',
        'Meursault, an indifferent Frenchman in Algeria, kills an Arab man and faces trial — not for the murder, but for his emotional detachment.',
        10.49, 45,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-stranger.jpg',
        (SELECT id FROM categories WHERE name = 'Philosophy & Drama'),
        FALSE, 0, NOW(), NOW()),

       ('Thus Spoke Zarathustra',
        'Friedrich Nietzsche',
        'Nietzsche''s philosophical novel presents the ideas of the Übermensch, the death of God, and eternal recurrence through the prophet Zarathustra.',
        11.99, 30,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/thus-spoke-zarathustra.jpg',
        (SELECT id FROM categories WHERE name = 'Philosophy & Drama'),
        FALSE, 0, NOW(), NOW()),

       ('Nausea',
        'Jean-Paul Sartre',
        'Antoine Roquentin, a historian in a French port city, is overwhelmed by a profound sense of existential disgust at the mere existence of things.',
        10.99, 20,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/nausea.jpg',
        (SELECT id FROM categories WHERE name = 'Philosophy & Drama'),
        FALSE, 0, NOW(), NOW());

-- Epic & Adventure
INSERT INTO books (title, author, description, price, stock, cover_image_url, category_id, deleted, version, created_at, updated_at)
VALUES ('Dune',
        'Frank Herbert',
        'On the desert planet Arrakis, young Paul Atreides navigates political intrigue, religion, and ecology to fulfil a messianic destiny.',
        15.99, 50,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/dune.jpg',
        (SELECT id FROM categories WHERE name = 'Epic & Adventure'),
        FALSE, 0, NOW(), NOW()),

       ('The Count of Monte Cristo',
        'Alexandre Dumas',
        'Edmond Dantès, wrongfully imprisoned, escapes and reinvents himself as the mysterious Count of Monte Cristo to exact an elaborate revenge.',
        14.49, 35,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/count-of-monte-cristo.jpg',
        (SELECT id FROM categories WHERE name = 'Epic & Adventure'),
        FALSE, 0, NOW(), NOW()),

       ('Don Quixote',
        'Miguel de Cervantes',
        'A Spanish gentleman driven mad by chivalric romances sets out as a knight-errant with his squire Sancho Panza, tilting at windmills.',
        12.99, 25,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/don-quixote.jpg',
        (SELECT id FROM categories WHERE name = 'Epic & Adventure'),
        FALSE, 0, NOW(), NOW()),

       ('One Hundred Years of Solitude',
        'Gabriel García Márquez',
        'Seven generations of the Buendía family in the fictional town of Macondo, blending magical realism with the history of Latin America.',
        13.99, 30,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/one-hundred-years.jpg',
        (SELECT id FROM categories WHERE name = 'Epic & Adventure'),
        FALSE, 0, NOW(), NOW());

-- Science & Non-Fiction
INSERT INTO books (title, author, description, price, stock, cover_image_url, category_id, deleted, version, created_at, updated_at)
VALUES ('The Selfish Gene',
        'Richard Dawkins',
        'Dawkins presents the gene-centred view of evolution, arguing that genes — not organisms or species — are the fundamental units of natural selection. A landmark work of popular science that introduced the concept of the meme.',
        13.99, 30,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-selfish-gene.jpg',
        (SELECT id FROM categories WHERE name = 'Science & Non-Fiction'),
        FALSE, 0, NOW(), NOW());
