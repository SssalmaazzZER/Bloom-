-- =====================================================================
-- Bloomé — DEVELOPMENT / DEMO catalog seed
-- Loaded only when FLYWAY_LOCATIONS includes classpath:db/seed
-- (default for local development; exclude it in production).
-- Photos: Unsplash (https://unsplash.com/license), hotlinked via images.unsplash.com
-- =====================================================================

CREATE FUNCTION pg_temp.img(id text) RETURNS text AS $$
    SELECT 'https://images.unsplash.com/photo-' || id || '?auto=format&fit=crop&w=1200&q=80'
$$ LANGUAGE sql IMMUTABLE;

-- ---------------------------------------------------------------------
-- Categories
-- ---------------------------------------------------------------------
INSERT INTO categories (slug, name, description, image_url, sort_order) VALUES
 ('bouquets',  'Bouquets',          'Des compositions généreuses, assemblées à la main chaque matin par nos fleuristes.', pg_temp.img('1563241527-3004b7be0ffd'), 1),
 ('roses',     'Roses',             'La reine des fleurs, en brassées veloutées ou en tiges élégantes.',                  pg_temp.img('1494972308805-463bc619d34e'), 2),
 ('saison',    'Fleurs de saison',  'Le meilleur de chaque saison, cueilli au bon moment chez nos producteurs.',         pg_temp.img('1444021465936-c6ca81d39b84'), 3),
 ('plantes',   'Plantes d''intérieur','Des plantes faciles à vivre pour verdir votre intérieur durablement.',            pg_temp.img('1614594975525-e45190c55d0b'), 4),
 ('coffrets',  'Coffrets cadeaux',  'Fleurs et douceurs réunies dans un coffret prêt à offrir.',                          pg_temp.img('1549465220-1a8b9238cd48'), 5);

-- ---------------------------------------------------------------------
-- Occasions
-- ---------------------------------------------------------------------
INSERT INTO occasions (slug, name, description, image_url, sort_order) VALUES
 ('anniversaire', 'Anniversaire',          'Des couleurs joyeuses pour souffler les bougies.',          pg_temp.img('1572454591674-2739f30d8c40'), 1),
 ('couple',       'Anniversaire de couple','Célébrer chaque année passée à deux.',                      pg_temp.img('1520342868574-5fa3804e551c'), 2),
 ('amour',        'Amour & romance',       'Dire « je t''aime » sans un mot.',                          pg_temp.img('1526047932273-341f2a7631f9'), 3),
 ('celebration',  'Célébrations',          'Diplômes, naissances, réussites : marquer le moment.',      pg_temp.img('1484100356142-db6ab6244067'), 4),
 ('condoleances', 'Condoléances',          'Des compositions sobres pour accompagner avec délicatesse.', pg_temp.img('1474112704314-8162b7749a90'), 5),
 ('merci',        'Merci',                 'Une attention fleurie pour exprimer sa gratitude.',          pg_temp.img('1471899236350-e3016bf1e69e'), 6);

-- ---------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------
INSERT INTO products (slug, name, short_description, description, composition, care_instructions, category_id,
                      price, compare_at_price, stock_quantity, low_stock_threshold, max_per_order,
                      featured, bestseller, sales_count, created_at)
VALUES
 ('rosee-du-matin', 'Rosée du Matin',
  'Roses roses poudrées et feuillage tendre, comme un matin de printemps.',
  'Rosée du Matin réunit des roses aux nuances rose poudré, cueillies à peine écloses pour s''ouvrir lentement chez vous. Un bouquet tendre et lumineux, idéal pour une naissance, un merci sincère ou simplement le plaisir d''offrir.',
  '15 roses « Sweet Avalanche », 5 roses branchues, eucalyptus cinerea, pittosporum',
  'Recoupez les tiges en biseau tous les deux jours et changez l''eau. Éloignez le bouquet des fruits et des sources de chaleur.',
  (SELECT id FROM categories WHERE slug = 'bouquets'), 420.00, NULL, 24, 5, 10, TRUE, TRUE, 138, now() - interval '40 days'),

 ('velvet-roses', 'Velvet Roses',
  'Une brassée de roses rouges au toucher de velours.',
  'Velvet Roses est notre déclaration la plus intense : des roses rouge profond aux pétales veloutés, sélectionnées pour leur tenue exceptionnelle. Présentées serrées, en dôme, pour un effet spectaculaire.',
  '24 roses rouges « Red Naomi » longues tiges',
  'Retirez les feuilles immergées, recoupez les tiges et ajoutez le sachet nutritif fourni.',
  (SELECT id FROM categories WHERE slug = 'roses'), 590.00, 650.00, 18, 5, 6, TRUE, TRUE, 212, now() - interval '60 days'),

 ('jardin-de-pivoines', 'Jardin de Pivoines',
  'Des pivoines généreuses aux pétales froissés, parfum délicat.',
  'Jardin de Pivoines célèbre la fleur la plus attendue de l''année. Leurs boutons ronds s''ouvrent en corolles spectaculaires, du rose tendre au fuchsia. Un bouquet d''exception, en quantité limitée.',
  '10 pivoines « Sarah Bernhardt », 3 roses de jardin, feuillage de saison',
  'Placez les boutons dans une eau tiède pour accélérer l''ouverture. Changez l''eau tous les deux jours.',
  (SELECT id FROM categories WHERE slug = 'bouquets'), 650.00, NULL, 4, 5, 4, TRUE, TRUE, 97, now() - interval '12 days'),

 ('soleil-d-ete', 'Soleil d''Été',
  'Tournesols éclatants pour faire entrer la lumière.',
  'Soleil d''Été rassemble de grands tournesols et des touches de vert frais. Un bouquet franc et joyeux qui illumine une pièce et fait sourire à coup sûr.',
  '7 tournesols, solidago, feuillage d''eucalyptus',
  'Les tournesols boivent beaucoup : vérifiez le niveau d''eau chaque jour.',
  (SELECT id FROM categories WHERE slug = 'saison'), 320.00, NULL, 30, 5, 10, FALSE, TRUE, 154, now() - interval '35 days'),

 ('douceur-pastel', 'Douceur Pastel',
  'Une palette crème et rose pâle, champêtre et délicate.',
  'Douceur Pastel mêle roses, œillets et petites fleurs des champs dans un camaïeu crème et rose pâle. Présenté dans un bocal en verre, il se pose tel quel, sans vase.',
  'Roses crème, œillets pastel, gypsophile, statice, bocal en verre recyclé',
  'Complétez l''eau du bocal tous les deux jours.',
  (SELECT id FROM categories WHERE slug = 'bouquets'), 360.00, NULL, 20, 5, 10, TRUE, FALSE, 76, now() - interval '20 days'),

 ('eclat-de-tulipes', 'Éclat de Tulipes',
  'Tulipes roses et fuchsia, fraîches et pétillantes.',
  'Éclat de Tulipes, c''est la gaieté du printemps : des tulipes roses et fuchsia qui continuent de grandir et de se courber gracieusement dans le vase.',
  '20 tulipes roses et fuchsia',
  'Utilisez un vase haut et une eau froide. Les tulipes continuent de pousser après la coupe.',
  (SELECT id FROM categories WHERE slug = 'saison'), 290.00, NULL, 26, 5, 10, FALSE, FALSE, 64, now() - interval '8 days'),

 ('jardin-sauvage', 'Jardin Sauvage',
  'Une composition libre et foisonnante, façon jardin anglais.',
  'Jardin Sauvage est notre signature : un bouquet ample, asymétrique et texturé, où se mêlent roses de jardin, fleurs des champs et feuillages graphiques. Chaque pièce est unique.',
  'Roses de jardin, renoncules, chardons, astilbes, feuillages variés selon arrivage',
  'Recoupez les tiges et retirez au fur et à mesure les fleurs fanées.',
  (SELECT id FROM categories WHERE slug = 'bouquets'), 520.00, NULL, 15, 5, 8, TRUE, FALSE, 58, now() - interval '5 days'),

 ('orchidee-elegance', 'Orchidée Élégance',
  'Phalaenopsis rose à deux hampes, en cache-pot céramique.',
  'Orchidée Élégance est une phalaenopsis à deux hampes florales, rose orchidée, présentée dans un cache-pot en céramique blanche. Elle fleurit plusieurs semaines et refleurit avec un minimum de soins.',
  '1 phalaenopsis 2 hampes, cache-pot céramique Ø 13 cm',
  'Arrosez une fois par semaine en trempant le pot 10 minutes. Lumière vive, sans soleil direct.',
  (SELECT id FROM categories WHERE slug = 'plantes'), 390.00, NULL, 12, 4, 5, TRUE, TRUE, 121, now() - interval '50 days'),

 ('fleurs-de-coton', 'Fleurs de Coton',
  'Un nuage de fleurs blanches, pur et apaisant.',
  'Fleurs de Coton réunit chrysanthèmes, roses et fleurs blanches en un bouquet doux et lumineux. Une composition tout en sobriété, appréciée pour les mariages comme pour les moments de recueillement.',
  'Chrysanthèmes blancs, roses blanches, lisianthus, gypsophile',
  'Changez l''eau tous les deux jours et retirez les fleurs fanées.',
  (SELECT id FROM categories WHERE slug = 'bouquets'), 380.00, NULL, 16, 5, 10, FALSE, FALSE, 45, now() - interval '25 days'),

 ('petit-bonheur', 'Petit Bonheur',
  'Un petit bouquet de gerberas pour un sourire immédiat.',
  'Petit Bonheur est l''attention parfaite : quelques gerberas roses et marguerites, réunis dans un petit bouquet rond. Le format idéal pour dire merci ou « je pense à toi ».',
  '5 gerberas roses, marguerites, feuillage',
  'Gardez peu d''eau dans le vase : les tiges de gerbera pourrissent vite si elles trempent trop.',
  (SELECT id FROM categories WHERE slug = 'bouquets'), 190.00, NULL, 40, 8, 10, FALSE, TRUE, 189, now() - interval '70 days'),

 ('hortensia-bleu-nuit', 'Hortensia Bleu Nuit',
  'Des têtes d''hortensia bleu intense, majestueuses.',
  'Hortensia Bleu Nuit rassemble de larges têtes d''hortensias au bleu profond. Une composition volumineuse et graphique, qui sèche joliment après sa floraison.',
  '5 têtes d''hortensia bleu, feuillage',
  'Les hortensias boivent par la tête : vaporisez-les chaque jour.',
  (SELECT id FROM categories WHERE slug = 'saison'), 440.00, NULL, 3, 4, 4, FALSE, FALSE, 22, now() - interval '3 days'),

 ('dahlia-d-automne', 'Dahlia d''Automne',
  'Dahlias rose poudré, la douceur de l''arrière-saison.',
  'Dahlia d''Automne met à l''honneur le dahlia « Café au lait » et ses pétales géométriques. Une composition saisonnière, disponible tant que dure la récolte.',
  '9 dahlias, cosmos, feuillage de saison',
  'Recoupez les tiges chaque jour pour prolonger la tenue.',
  (SELECT id FROM categories WHERE slug = 'saison'), 410.00, NULL, 10, 4, 6, TRUE, FALSE, 31, now() - interval '1 day'),

 ('monstera-deliciosa', 'Monstera Deliciosa',
  'La plante tropicale iconique, facile à vivre.',
  'Le Monstera Deliciosa et ses grandes feuilles découpées apportent une touche tropicale à toute pièce. Livré en pot blanc, hauteur 60 à 70 cm.',
  'Monstera deliciosa, pot céramique blanc Ø 19 cm',
  'Arrosez quand la terre est sèche sur 3 cm. Lumière indirecte.',
  (SELECT id FROM categories WHERE slug = 'plantes'), 450.00, NULL, 9, 3, 3, FALSE, TRUE, 88, now() - interval '45 days'),

 ('calathea-orbifolia', 'Calathea Orbifolia',
  'De larges feuilles rayées argent, d''un vert apaisant.',
  'La Calathea Orbifolia déploie de grandes feuilles rondes zébrées d''argent. Une plante d''ambiance qui aime l''humidité et la lumière douce.',
  'Calathea orbifolia, pot Ø 17 cm',
  'Gardez la terre légèrement humide et vaporisez les feuilles avec une eau non calcaire.',
  (SELECT id FROM categories WHERE slug = 'plantes'), 280.00, NULL, 0, 3, 3, FALSE, FALSE, 19, now() - interval '30 days'),

 ('trio-de-succulentes', 'Trio de Succulentes',
  'Trois succulentes en mini-pots, presque sans entretien.',
  'Le Trio de Succulentes réunit trois petites plantes graphiques, idéales pour un bureau ou une étagère. Le cadeau parfait pour les jardiniers débutants.',
  '3 succulentes assorties, mini-pots en terre cuite',
  'Un arrosage léger toutes les deux semaines suffit. Beaucoup de lumière.',
  (SELECT id FROM categories WHERE slug = 'plantes'), 180.00, NULL, 35, 6, 10, FALSE, FALSE, 73, now() - interval '28 days'),

 ('bonsai-ficus', 'Bonsaï Ficus',
  'Un ficus ginseng sculptural, symbole de longévité.',
  'Ce bonsaï Ficus Ginseng, aux racines aériennes sculpturales, est livré dans une coupe en céramique. Une plante d''intérieur robuste et pleine de caractère.',
  'Ficus microcarpa « Ginseng », coupe céramique, soucoupe',
  'Arrosez dès que la surface sèche. Évitez les courants d''air.',
  (SELECT id FROM categories WHERE slug = 'plantes'), 340.00, NULL, 7, 3, 4, FALSE, FALSE, 27, now() - interval '15 days'),

 ('coffret-douceur', 'Coffret Douceur',
  'Bouquet de roses, bougie parfumée et chocolats artisanaux.',
  'Le Coffret Douceur réunit un petit bouquet de roses roses, une bougie parfumée à la fleur d''oranger et un assortiment de chocolats artisanaux de Rabat. Emballé dans notre boîte rose signature.',
  'Bouquet de 9 roses, bougie 180 g fleur d''oranger, 12 chocolats',
  'Placez le bouquet dans l''eau dès réception. Bougie : première combustion de 2 heures.',
  (SELECT id FROM categories WHERE slug = 'coffrets'), 590.00, NULL, 12, 4, 5, TRUE, FALSE, 54, now() - interval '18 days'),

 ('coffret-fetes', 'Coffret Fêtes',
  'Fleurs rouges et coffret gourmand pour célébrer en grand.',
  'Le Coffret Fêtes associe une composition de fleurs rouges, un thé à la menthe de nos partenaires et des pâtisseries fines. Le cadeau idéal pour les fêtes et les grandes occasions.',
  'Composition rouge, thé vert à la menthe 100 g, assortiment de pâtisseries',
  'Conservez les pâtisseries au frais et consommez-les sous 5 jours.',
  (SELECT id FROM categories WHERE slug = 'coffrets'), 690.00, 750.00, 6, 3, 3, FALSE, FALSE, 23, now() - interval '9 days'),

 ('lys-etoile', 'Lys Étoilé',
  'Lys stargazer au parfum enivrant.',
  'Lys Étoilé met en scène des lys stargazer aux pétales roses tachetés, au parfum puissant. Livrés en boutons pour une floraison prolongée.',
  '7 tiges de lys « Stargazer », feuillage',
  'Retirez délicatement les étamines à l''ouverture pour éviter les taches de pollen.',
  (SELECT id FROM categories WHERE slug = 'saison'), 350.00, NULL, 14, 4, 8, FALSE, FALSE, 39, now() - interval '22 days'),

 ('douce-memoire', 'Douce Mémoire',
  'Fleurs blanches et feuillage, pour accompagner avec délicatesse.',
  'Douce Mémoire est une composition sobre de fleurs blanches, pensée pour exprimer votre soutien et votre affection dans les moments difficiles. Une carte manuscrite peut l''accompagner.',
  'Roses blanches, magnolia, lisianthus, feuillage vert',
  'Changez l''eau régulièrement et retirez les fleurs fanées.',
  (SELECT id FROM categories WHERE slug = 'bouquets'), 480.00, NULL, 10, 4, 5, FALSE, FALSE, 17, now() - interval '33 days');

-- ---------------------------------------------------------------------
-- Product images
-- ---------------------------------------------------------------------
INSERT INTO product_images (product_id, url, alt_text, sort_order)
SELECT p.id, pg_temp.img(v.photo), v.alt, v.ord
FROM (VALUES
  ('rosee-du-matin',      '1582794543139-8ac9cb0f7b11', 'Bouquet de roses roses poudrées', 0),
  ('rosee-du-matin',      '1518895949257-7621c3c786d7', 'Rose rose dans un vase en verre', 1),
  ('velvet-roses',        '1494972308805-463bc619d34e', 'Brassée de roses rouges', 0),
  ('velvet-roses',        '1496062031456-07b8f162a322', 'Rose rouge veloutée en gros plan', 1),
  ('velvet-roses',        '1519378058457-4c29a0a2efac', 'Roses rouges serrées en dôme', 2),
  ('jardin-de-pivoines',  '1687201171025-06712d5d4dc9', 'Pivoines roses dans un vase blanc', 0),
  ('jardin-de-pivoines',  '1563770660941-20978e870e26', 'Pivoine rose en fleur', 1),
  ('jardin-de-pivoines',  '1591886960571-74d43a9d4166', 'Roses de jardin et pivoines', 2),
  ('soleil-d-ete',        '1455659817273-f96807779a8a', 'Tournesols lumineux', 0),
  ('soleil-d-ete',        '1470509037663-253afd7f0f51', 'Tournesol en plein soleil', 1),
  ('soleil-d-ete',        '1597848212624-a19eb35e2651', 'Champ de tournesols', 2),
  ('douceur-pastel',      '1563241527-3004b7be0ffd', 'Bouquet pastel dans un bocal en verre', 0),
  ('douceur-pastel',      '1525310072745-f49212b5ac6d', 'Fleur rose pâle sur fond clair', 1),
  ('eclat-de-tulipes',    '1561181286-d3fee7d55364', 'Tulipes roses dans un vase', 0),
  ('eclat-de-tulipes',    '1520763185298-1b434c919102', 'Tulipe rose sur fond rose', 1),
  ('eclat-de-tulipes',    '1468327768560-75b778cbb551', 'Tulipes blanches et rouges', 2),
  ('jardin-sauvage',      '1487530811176-3780de880c2d', 'Bouquet foisonnant façon jardin anglais', 0),
  ('jardin-sauvage',      '1457089328109-e5d9bd499191', 'Composition aux tons profonds', 1),
  ('orchidee-elegance',   '1610397648930-477b8c7f0943', 'Orchidée phalaenopsis rose', 0),
  ('fleurs-de-coton',     '1460039230329-eb070fc6c77c', 'Fleurs blanches en bouquet', 0),
  ('fleurs-de-coton',     '1495231916356-a86217efff12', 'Rose blanche en gros plan', 1),
  ('petit-bonheur',       '1471899236350-e3016bf1e69e', 'Gerbera rose sur fond bleu', 0),
  ('petit-bonheur',       '1508784411316-02b8cd4d3a3a', 'Marguerite blanche', 1),
  ('hortensia-bleu-nuit', '1471696035578-3d8c78d99684', 'Hortensias bleus', 0),
  ('dahlia-d-automne',    '1444021465936-c6ca81d39b84', 'Dahlia rose poudré', 0),
  ('dahlia-d-automne',    '1530092285049-1c42085fd395', 'Cosmos blancs dans le ciel', 1),
  ('monstera-deliciosa',  '1614594975525-e45190c55d0b', 'Monstera en pot blanc', 0),
  ('monstera-deliciosa',  '1545241047-6083a3684587', 'Monstera dans un intérieur lumineux', 1),
  ('calathea-orbifolia',  '1491147334573-44cbb4602074', 'Feuilles de calathea rayées', 0),
  ('trio-de-succulentes', '1459156212016-c812468e2115', 'Succulentes en pots vues du dessus', 0),
  ('trio-de-succulentes', '1485955900006-10f4d324d411', 'Succulente dans un pot menthe', 1),
  ('trio-de-succulentes', '1509423350716-97f9360b4e09', 'Haworthia en pot blanc', 2),
  ('bonsai-ficus',        '1526397751294-331021109fbd', 'Bonsaï en coupe', 0),
  ('bonsai-ficus',        '1512428813834-c702c7702b78', 'Petit bonsaï ficus', 1),
  ('coffret-douceur',     '1549465220-1a8b9238cd48', 'Coffret cadeau rose avec ruban doré', 0),
  ('coffret-douceur',     '1572454591674-2739f30d8c40', 'Bouquet de roses pêche', 1),
  ('coffret-fetes',       '1513885535751-8b9238bd345a', 'Coffrets cadeaux rouges', 0),
  ('lys-etoile',          '1502977249166-824b3a8a4d6d', 'Lys stargazer rose', 0),
  ('douce-memoire',       '1474112704314-8162b7749a90', 'Fleur de magnolia blanche', 0),
  ('douce-memoire',       '1495231916356-a86217efff12', 'Rose blanche', 1)
) AS v(slug, photo, alt, ord)
JOIN products p ON p.slug = v.slug;

-- ---------------------------------------------------------------------
-- Colors
-- ---------------------------------------------------------------------
INSERT INTO product_colors (product_id, color)
SELECT p.id, v.color FROM (VALUES
  ('rosee-du-matin','PINK'), ('rosee-du-matin','PASTEL'),
  ('velvet-roses','RED'),
  ('jardin-de-pivoines','PINK'),
  ('soleil-d-ete','YELLOW'),
  ('douceur-pastel','PASTEL'), ('douceur-pastel','WHITE'), ('douceur-pastel','PINK'),
  ('eclat-de-tulipes','PINK'),
  ('jardin-sauvage','MIXED'), ('jardin-sauvage','PINK'),
  ('orchidee-elegance','PINK'), ('orchidee-elegance','PURPLE'),
  ('fleurs-de-coton','WHITE'),
  ('petit-bonheur','PINK'),
  ('hortensia-bleu-nuit','BLUE'),
  ('dahlia-d-automne','PINK'), ('dahlia-d-automne','PASTEL'),
  ('monstera-deliciosa','GREEN'),
  ('calathea-orbifolia','GREEN'),
  ('trio-de-succulentes','GREEN'),
  ('bonsai-ficus','GREEN'),
  ('coffret-douceur','PINK'),
  ('coffret-fetes','RED'),
  ('lys-etoile','PINK'), ('lys-etoile','WHITE'),
  ('douce-memoire','WHITE')
) AS v(slug, color) JOIN products p ON p.slug = v.slug;

-- ---------------------------------------------------------------------
-- Occasions per product
-- ---------------------------------------------------------------------
INSERT INTO product_occasions (product_id, occasion_id)
SELECT p.id, o.id FROM (VALUES
  ('rosee-du-matin','anniversaire'), ('rosee-du-matin','merci'), ('rosee-du-matin','amour'),
  ('velvet-roses','amour'), ('velvet-roses','couple'),
  ('jardin-de-pivoines','couple'), ('jardin-de-pivoines','anniversaire'), ('jardin-de-pivoines','celebration'),
  ('soleil-d-ete','anniversaire'), ('soleil-d-ete','merci'), ('soleil-d-ete','celebration'),
  ('douceur-pastel','merci'), ('douceur-pastel','celebration'),
  ('eclat-de-tulipes','anniversaire'), ('eclat-de-tulipes','merci'),
  ('jardin-sauvage','celebration'), ('jardin-sauvage','anniversaire'),
  ('orchidee-elegance','merci'), ('orchidee-elegance','couple'),
  ('fleurs-de-coton','condoleances'), ('fleurs-de-coton','celebration'),
  ('petit-bonheur','merci'), ('petit-bonheur','anniversaire'),
  ('hortensia-bleu-nuit','celebration'),
  ('dahlia-d-automne','anniversaire'), ('dahlia-d-automne','merci'),
  ('monstera-deliciosa','celebration'),
  ('trio-de-succulentes','merci'),
  ('bonsai-ficus','merci'), ('bonsai-ficus','condoleances'),
  ('coffret-douceur','anniversaire'), ('coffret-douceur','amour'),
  ('coffret-fetes','celebration'), ('coffret-fetes','couple'),
  ('lys-etoile','condoleances'), ('lys-etoile','anniversaire'),
  ('douce-memoire','condoleances')
) AS v(slug, occ)
JOIN products p ON p.slug = v.slug
JOIN occasions o ON o.slug = v.occ;

-- ---------------------------------------------------------------------
-- Build-your-bouquet options
-- ---------------------------------------------------------------------
INSERT INTO bouquet_stems (name, color, description, image_url, unit_price, stock_quantity, max_per_bouquet, sort_order) VALUES
 ('Rose rouge',          'RED',    'Rose « Red Naomi », tige longue',          pg_temp.img('1496062031456-07b8f162a322'), 22.00, 300, 25, 1),
 ('Rose rose poudré',    'PINK',   'Rose « Sweet Avalanche »',                 pg_temp.img('1518895949257-7621c3c786d7'), 22.00, 250, 25, 2),
 ('Rose blanche',        'WHITE',  'Rose « Avalanche » blanc pur',             pg_temp.img('1495231916356-a86217efff12'), 22.00, 200, 25, 3),
 ('Pivoine',             'PINK',   'Pivoine « Sarah Bernhardt »',              pg_temp.img('1563770660941-20978e870e26'), 45.00, 60,  10, 4),
 ('Tulipe rose',         'PINK',   'Tulipe de Hollande',                       pg_temp.img('1520763185298-1b434c919102'), 14.00, 200, 20, 5),
 ('Tournesol',           'YELLOW', 'Tournesol à grande tête',                  pg_temp.img('1470509037663-253afd7f0f51'), 18.00, 120, 10, 6),
 ('Gerbera',             'PINK',   'Gerbera rose tendre',                      pg_temp.img('1471899236350-e3016bf1e69e'), 12.00, 150, 15, 7),
 ('Marguerite',          'WHITE',  'Marguerite des champs',                    pg_temp.img('1508784411316-02b8cd4d3a3a'),  8.00, 200, 20, 8),
 ('Lys stargazer',       'PINK',   'Lys parfumé, livré en boutons',            pg_temp.img('1502977249166-824b3a8a4d6d'), 30.00, 80,  8,  9),
 ('Dahlia',              'PINK',   'Dahlia « Café au lait »',                  pg_temp.img('1444021465936-c6ca81d39b84'), 28.00, 70,  10, 10),
 ('Hortensia bleu',      'BLUE',   'Tête d''hortensia volumineuse',            pg_temp.img('1471696035578-3d8c78d99684'), 40.00, 40,  5,  11),
 ('Fougère',             'GREEN',  'Feuillage de fougère graphique',           pg_temp.img('1599148401005-fe6d7497cb5e'),  6.00, 300, 10, 12);

INSERT INTO bouquet_wrappings (name, description, swatch_color, price, sort_order) VALUES
 ('Papier kraft',       'Naturel et champêtre, noué à la main.',            '#C8A27A', 0.00,  1),
 ('Papier crème',       'Papier de soie crème, élégant et intemporel.',     '#F4EADF', 15.00, 2),
 ('Rose blush',         'Papier mousseline rose poudré, notre signature.',  '#F2CFD2', 20.00, 3),
 ('Lin bordeaux',       'Tissu de lin réutilisable, couleur bordeaux.',     '#663B45', 45.00, 4),
 ('Boîte chapeau',      'Boîte ronde rigide avec mousse florale.',          '#302A29', 90.00, 5);

INSERT INTO bouquet_ribbons (name, swatch_color, price, sort_order) VALUES
 ('Ficelle naturelle', '#B89B72', 0.00,  1),
 ('Satin ivoire',      '#FFF6E6', 10.00, 2),
 ('Velours bordeaux',  '#663B45', 15.00, 3),
 ('Soie vieux rose',   '#C77983', 15.00, 4),
 ('Organza sauge',     '#9AA78D', 12.00, 5);
