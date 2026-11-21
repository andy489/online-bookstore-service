-- Replace local /covers/ paths with Cloudinary CDN URLs.
-- Images live at: https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/{slug}.jpg

-- Classic Literature
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/brothers-karamazov.jpg'   WHERE title = 'The Brothers Karamazov';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/crime-and-punishment.jpg'  WHERE title = 'Crime and Punishment';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-idiot.jpg'             WHERE title = 'The Idiot';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/master-and-margarita.jpg'  WHERE title = 'The Master and Margarita';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/brave-new-world.jpg'       WHERE title = 'Brave New World';

-- War & Historical Fiction
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/catch-22.jpg'                WHERE title = 'Catch-22';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/shogun.jpg'                  WHERE title = 'Shogun';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/king-rat.jpg'                WHERE title = 'King Rat';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/tai-pan.jpg'                 WHERE title = 'Tai-Pan';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/for-whom-the-bell-tolls.jpg' WHERE title = 'For Whom the Bell Tolls';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/all-quiet-western-front.jpg' WHERE title = 'All Quiet on the Western Front';

-- Crime & Mystery
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/girl-with-dragon-tattoo.jpg'  WHERE title = 'The Girl with the Dragon Tattoo';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/and-then-there-were-none.jpg' WHERE title = 'And Then There Were None';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/name-of-the-rose.jpg'         WHERE title = 'The Name of the Rose';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/mystic-river.jpg'             WHERE title = 'Mystic River';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/gone-baby-gone.jpg'           WHERE title = 'Gone Baby Gone';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/shutter-island.jpg'           WHERE title = 'Shutter Island';

-- Philosophy & Drama
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-stranger.jpg'           WHERE title = 'The Stranger';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/thus-spoke-zarathustra.jpg' WHERE title = 'Thus Spoke Zarathustra';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/nausea.jpg'                 WHERE title = 'Nausea';

-- Epic & Adventure
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/dune.jpg'                  WHERE title = 'Dune';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/count-of-monte-cristo.jpg' WHERE title = 'The Count of Monte Cristo';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/don-quixote.jpg'           WHERE title = 'Don Quixote';
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/one-hundred-years.jpg'     WHERE title = 'One Hundred Years of Solitude';

-- Science & Non-Fiction
UPDATE books SET cover_image_url = 'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-selfish-gene.jpg' WHERE title = 'The Selfish Gene';
