-- Seed Science & Non-Fiction category and two Gattaca-adjacent books.

INSERT INTO categories (name, description)
VALUES ('Science & Non-Fiction', 'Popular science, evolutionary biology, and factual writing');

-- Classic Literature
INSERT INTO books (title, author, description, price, stock, category_id, deleted, version, created_at, updated_at)
VALUES ('Brave New World',
        'Aldous Huxley',
        'In a future World State, human beings are genetically engineered and socially conditioned from birth. Bernard Marx begins to question the shallow happiness of a society built on pleasure, conformity, and the elimination of free will.',
        11.99, 45,
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW());

-- Science & Non-Fiction
INSERT INTO books (title, author, description, price, stock, category_id, deleted, version, created_at, updated_at)
VALUES ('The Selfish Gene',
        'Richard Dawkins',
        'Dawkins presents the gene-centred view of evolution, arguing that genes — not organisms or species — are the fundamental units of natural selection. A landmark work of popular science that introduced the concept of the meme.',
        13.99, 30,
        (SELECT id FROM categories WHERE name = 'Science & Non-Fiction'),
        FALSE, 0, NOW(), NOW());
