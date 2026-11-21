-- Wire cover_image_url for all seeded books.
-- Images are served statically by Spring Boot from /covers/{slug}.jpg

-- Classic Literature
UPDATE books SET cover_image_url = '/covers/brothers-karamazov.jpg'   WHERE title = 'The Brothers Karamazov';
UPDATE books SET cover_image_url = '/covers/crime-and-punishment.jpg'  WHERE title = 'Crime and Punishment';
UPDATE books SET cover_image_url = '/covers/the-idiot.jpg'             WHERE title = 'The Idiot';
UPDATE books SET cover_image_url = '/covers/master-and-margarita.jpg'  WHERE title = 'The Master and Margarita';
UPDATE books SET cover_image_url = '/covers/brave-new-world.jpg'       WHERE title = 'Brave New World';

-- War & Historical Fiction
UPDATE books SET cover_image_url = '/covers/catch-22.jpg'              WHERE title = 'Catch-22';
UPDATE books SET cover_image_url = '/covers/shogun.jpg'                WHERE title = 'Shogun';
UPDATE books SET cover_image_url = '/covers/king-rat.jpg'              WHERE title = 'King Rat';
UPDATE books SET cover_image_url = '/covers/tai-pan.jpg'               WHERE title = 'Tai-Pan';
UPDATE books SET cover_image_url = '/covers/for-whom-the-bell-tolls.jpg' WHERE title = 'For Whom the Bell Tolls';
UPDATE books SET cover_image_url = '/covers/all-quiet-western-front.jpg' WHERE title = 'All Quiet on the Western Front';

-- Crime & Mystery
UPDATE books SET cover_image_url = '/covers/girl-with-dragon-tattoo.jpg' WHERE title = 'The Girl with the Dragon Tattoo';
UPDATE books SET cover_image_url = '/covers/and-then-there-were-none.jpg' WHERE title = 'And Then There Were None';
UPDATE books SET cover_image_url = '/covers/name-of-the-rose.jpg'      WHERE title = 'The Name of the Rose';
UPDATE books SET cover_image_url = '/covers/mystic-river.jpg'          WHERE title = 'Mystic River';
UPDATE books SET cover_image_url = '/covers/gone-baby-gone.jpg'        WHERE title = 'Gone Baby Gone';
UPDATE books SET cover_image_url = '/covers/shutter-island.jpg'        WHERE title = 'Shutter Island';

-- Philosophy & Drama
UPDATE books SET cover_image_url = '/covers/the-stranger.jpg'          WHERE title = 'The Stranger';
UPDATE books SET cover_image_url = '/covers/thus-spoke-zarathustra.jpg' WHERE title = 'Thus Spoke Zarathustra';
UPDATE books SET cover_image_url = '/covers/nausea.jpg'                WHERE title = 'Nausea';

-- Epic & Adventure
UPDATE books SET cover_image_url = '/covers/dune.jpg'                  WHERE title = 'Dune';
UPDATE books SET cover_image_url = '/covers/count-of-monte-cristo.jpg' WHERE title = 'The Count of Monte Cristo';
UPDATE books SET cover_image_url = '/covers/don-quixote.jpg'           WHERE title = 'Don Quixote';
UPDATE books SET cover_image_url = '/covers/one-hundred-years.jpg'     WHERE title = 'One Hundred Years of Solitude';

-- Science & Non-Fiction
UPDATE books SET cover_image_url = '/covers/the-selfish-gene.jpg'      WHERE title = 'The Selfish Gene';
