-- Seeds a full Indian menu. Safe to run against a menu that already has items:
-- categories are matched by name, and a dish is skipped when any item with the
-- same normalized name already exists in any category. Photos are served by the
-- frontend from /menu/<slug>.jpg.

INSERT INTO menu_categories (name, description, display_order, active, created_at, updated_at)
SELECT v.name, v.description, v.display_order, TRUE, NOW(), NOW()
FROM (VALUES
    ('Soups', 'Warm shorbas and soups to start', 1),
    ('Starters', 'Snacks and appetizers', 2),
    ('Tandoor', 'Kebabs and tikkas from the clay oven', 3),
    ('Chaat & Street Food', 'Tangy, crunchy favourites from India''s streets', 4),
    ('Main Course', 'Main meals and curries', 5),
    ('Biryani & Rice', 'Fragrant biryanis and rice dishes', 6),
    ('Indian Breads', 'Fresh breads from the tandoor and tawa', 7),
    ('South Indian', 'Dosas, idlis and classics from the south', 8),
    ('Desserts', 'Sweets and desserts', 9),
    ('Beverages', 'Hot and cold drinks', 10)
) AS v(name, description, display_order)
WHERE NOT EXISTS (
    SELECT 1 FROM menu_categories c WHERE LOWER(BTRIM(c.name)) = LOWER(BTRIM(v.name))
);

-- Put the standard categories in menu order; other categories keep their order.
UPDATE menu_categories c
SET display_order = v.display_order, updated_at = NOW()
FROM (VALUES
    ('Soups', 1),
    ('Starters', 2),
    ('Tandoor', 3),
    ('Chaat & Street Food', 4),
    ('Main Course', 5),
    ('Biryani & Rice', 6),
    ('Indian Breads', 7),
    ('South Indian', 8),
    ('Desserts', 9),
    ('Beverages', 10)
) AS v(name, display_order)
WHERE LOWER(BTRIM(c.name)) = LOWER(BTRIM(v.name))
  AND c.display_order <> v.display_order;

INSERT INTO menu_items (name, description, price, category_id, available,
                        preparation_time_minutes, image_url, food_type, spice_level,
                        created_at, updated_at)
SELECT v.name, v.description, v.price, c.id, TRUE, v.prep, v.image_url, v.food_type, v.spice,
       NOW(), NOW()
FROM (VALUES
    ('Soups', 'Tomato Shorba', 'Spiced tomato broth tempered with cumin, ginger and fresh coriander.', 120.00, 'VEG', 'MILD', 10, NULL),
    ('Soups', 'Sweet Corn Soup', 'Thick, comforting soup of sweet corn and finely chopped vegetables.', 130.00, 'VEG', 'NONE', 10, 'https://smartserve.anshuyadav.me/menu/sweet-corn-soup.jpg'),
    ('Soups', 'Mulligatawny Soup', 'Anglo-Indian lentil and chicken soup with pepper, cumin and a squeeze of lemon.', 160.00, 'NON_VEG', 'MEDIUM', 15, 'https://smartserve.anshuyadav.me/menu/mulligatawny-soup.jpg'),
    ('Soups', 'Rasam', 'Tangy South Indian tamarind and tomato broth with black pepper and curry leaves.', 100.00, 'VEG', 'MEDIUM', 10, 'https://smartserve.anshuyadav.me/menu/rasam.jpg'),
    ('Soups', 'Manchow Soup', 'Indo-Chinese hot and sour vegetable soup topped with crispy fried noodles.', 140.00, 'VEG', 'MEDIUM', 12, 'https://smartserve.anshuyadav.me/menu/manchow-soup.jpg'),
    ('Starters', 'Samosa', 'Two crisp pastries stuffed with spiced potato and peas, served with chutneys.', 60.00, 'VEG', 'MEDIUM', 10, 'https://smartserve.anshuyadav.me/menu/samosa.jpg'),
    ('Starters', 'Onion Pakora', 'Crunchy onion fritters in spiced gram flour batter with mint chutney.', 120.00, 'VEG', 'MEDIUM', 12, 'https://smartserve.anshuyadav.me/menu/onion-pakora.jpg'),
    ('Starters', 'Hara Bhara Kabab', 'Pan-fried patties of spinach, peas and potato with green chutney.', 180.00, 'VEG', 'MILD', 15, NULL),
    ('Starters', 'Aloo Tikki', 'Golden potato patties served with tamarind and mint chutney.', 110.00, 'VEG', 'MILD', 12, NULL),
    ('Starters', 'Gobi Manchurian', 'Crispy cauliflower tossed in a spicy, tangy Indo-Chinese sauce.', 190.00, 'VEG', 'MEDIUM', 15, 'https://smartserve.anshuyadav.me/menu/gobi-manchurian.jpg'),
    ('Starters', 'Chilli Paneer', 'Paneer cubes wok-tossed with peppers, onions and chilli sauce.', 240.00, 'VEG', 'HOT', 15, NULL),
    ('Starters', 'Chicken 65', 'Spicy deep-fried chicken bites with curry leaves and green chillies.', 260.00, 'NON_VEG', 'HOT', 18, 'https://smartserve.anshuyadav.me/menu/chicken-65.jpg'),
    ('Starters', 'Chilli Chicken', 'Crispy chicken tossed with peppers, onions and a fiery soy-chilli glaze.', 270.00, 'NON_VEG', 'HOT', 18, 'https://smartserve.anshuyadav.me/menu/chilli-chicken.jpg'),
    ('Starters', 'Amritsari Fish Fry', 'Ajwain-spiced batter-fried fish, Amritsar style, with lemon and chutney.', 320.00, 'NON_VEG', 'MEDIUM', 18, 'https://smartserve.anshuyadav.me/menu/amritsari-fish-fry.jpg'),
    ('Tandoor', 'Tandoori Chicken', 'Half chicken marinated in yogurt and spices, roasted in the tandoor.', 380.00, 'NON_VEG', 'MEDIUM', 25, 'https://smartserve.anshuyadav.me/menu/tandoori-chicken.jpg'),
    ('Tandoor', 'Chicken Tikka', 'Boneless chicken chunks in a spiced yogurt marinade, chargrilled.', 320.00, 'NON_VEG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/chicken-tikka.jpg'),
    ('Tandoor', 'Chicken Malai Tikka', 'Creamy, mildly spiced chicken tikka with cheese, cream and cardamom.', 340.00, 'NON_VEG', 'MILD', 20, 'https://smartserve.anshuyadav.me/menu/chicken-malai-tikka.jpg'),
    ('Tandoor', 'Mutton Seekh Kebab', 'Minced mutton with herbs and spices, skewered and grilled.', 360.00, 'NON_VEG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/mutton-seekh-kebab.jpg'),
    ('Tandoor', 'Tandoori Prawns', 'Jumbo prawns marinated in ajwain and chilli, roasted in the tandoor.', 450.00, 'NON_VEG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/tandoori-prawns.jpg'),
    ('Tandoor', 'Fish Tikka', 'Boneless fish chunks in a mustard and spice marinade, chargrilled.', 380.00, 'NON_VEG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/fish-tikka.jpg'),
    ('Tandoor', 'Mushroom Tikka', 'Button mushrooms in a tangy tikka marinade, grilled with peppers and onion.', 240.00, 'VEG', 'MEDIUM', 18, NULL),
    ('Chaat & Street Food', 'Pani Puri', 'Crisp puris filled with potato, chickpeas and tangy spiced mint water.', 80.00, 'VEG', 'MEDIUM', 8, 'https://smartserve.anshuyadav.me/menu/pani-puri.jpg'),
    ('Chaat & Street Food', 'Bhel Puri', 'Puffed rice tossed with sev, onion, tomato and sweet and green chutneys.', 90.00, 'VEG', 'MILD', 8, 'https://smartserve.anshuyadav.me/menu/bhel-puri.jpg'),
    ('Chaat & Street Food', 'Papdi Chaat', 'Crisp papdi topped with potato, yogurt, chutneys and sev.', 110.00, 'VEG', 'MILD', 8, NULL),
    ('Chaat & Street Food', 'Pav Bhaji', 'Buttery mashed vegetable curry served with toasted pav buns.', 160.00, 'VEG', 'MEDIUM', 15, 'https://smartserve.anshuyadav.me/menu/pav-bhaji.jpg'),
    ('Chaat & Street Food', 'Vada Pav', 'Mumbai''s spiced potato fritter in a soft bun with garlic chutney.', 60.00, 'VEG', 'MEDIUM', 8, NULL),
    ('Chaat & Street Food', 'Chole Bhature', 'Spicy chickpea curry with two fluffy fried bhature.', 180.00, 'VEG', 'MEDIUM', 15, 'https://smartserve.anshuyadav.me/menu/chole-bhature.jpg'),
    ('Chaat & Street Food', 'Paneer Kathi Roll', 'Flaky paratha wrapped around spiced paneer tikka, onions and chutney.', 170.00, 'VEG', 'MEDIUM', 12, NULL),
    ('Chaat & Street Food', 'Chicken Kathi Roll', 'Flaky paratha wrapped around chicken tikka, onions and mint chutney.', 190.00, 'NON_VEG', 'MEDIUM', 12, NULL),
    ('Main Course', 'Palak Paneer', 'Paneer cubes simmered in a smooth, lightly spiced spinach gravy.', 270.00, 'VEG', 'MILD', 20, 'https://smartserve.anshuyadav.me/menu/palak-paneer.jpg'),
    ('Main Course', 'Shahi Paneer', 'Paneer in a rich, mildly sweet cream and cashew gravy.', 280.00, 'VEG', 'MILD', 20, 'https://smartserve.anshuyadav.me/menu/shahi-paneer.jpg'),
    ('Main Course', 'Kadai Paneer', 'Paneer with peppers and onion in a bold kadai masala.', 280.00, 'VEG', 'MEDIUM', 20, NULL),
    ('Main Course', 'Malai Kofta', 'Soft paneer and potato dumplings in a creamy tomato gravy.', 280.00, 'VEG', 'MILD', 25, 'https://smartserve.anshuyadav.me/menu/malai-kofta.jpg'),
    ('Main Course', 'Chana Masala', 'Chickpeas slow-cooked in a tangy onion-tomato masala.', 240.00, 'VEG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/chana-masala.jpg'),
    ('Main Course', 'Rajma Masala', 'Red kidney beans in a thick, home-style Punjabi gravy.', 230.00, 'VEG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/rajma-masala.jpg'),
    ('Main Course', 'Aloo Gobi', 'Potato and cauliflower dry-cooked with turmeric, cumin and ginger.', 210.00, 'VEG', 'MILD', 18, 'https://smartserve.anshuyadav.me/menu/aloo-gobi.jpg'),
    ('Main Course', 'Baingan Bharta', 'Smoky fire-roasted aubergine mashed with onion, tomato and spices.', 230.00, 'VEG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/baingan-bharta.jpg'),
    ('Main Course', 'Bhindi Masala', 'Okra stir-fried with onions, tomatoes and dry spices.', 220.00, 'VEG', 'MEDIUM', 18, 'https://smartserve.anshuyadav.me/menu/bhindi-masala.jpg'),
    ('Main Course', 'Dal Tadka', 'Yellow lentils tempered with ghee, cumin, garlic and red chilli.', 200.00, 'VEG', 'MEDIUM', 15, 'https://smartserve.anshuyadav.me/menu/dal-tadka.jpg'),
    ('Main Course', 'Sarson ka Saag', 'Slow-cooked mustard greens finished with white butter, Punjabi style.', 240.00, 'VEG', 'MILD', 25, 'https://smartserve.anshuyadav.me/menu/sarson-ka-saag.jpg'),
    ('Main Course', 'Dum Aloo', 'Baby potatoes slow-cooked in a spiced yogurt gravy, Kashmiri style.', 240.00, 'VEG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/dum-aloo.jpg'),
    ('Main Course', 'Butter Chicken', 'Tandoori chicken in a velvety tomato, butter and cream gravy.', 360.00, 'NON_VEG', 'MILD', 25, 'https://smartserve.anshuyadav.me/menu/butter-chicken.jpg'),
    ('Main Course', 'Chicken Tikka Masala', 'Chargrilled chicken tikka in a spiced, creamy onion-tomato masala.', 370.00, 'NON_VEG', 'MEDIUM', 25, 'https://smartserve.anshuyadav.me/menu/chicken-tikka-masala.jpg'),
    ('Main Course', 'Kadai Chicken', 'Chicken cooked with peppers and freshly pounded kadai spices.', 350.00, 'NON_VEG', 'MEDIUM', 25, 'https://smartserve.anshuyadav.me/menu/kadai-chicken.jpg'),
    ('Main Course', 'Chicken Chettinad', 'Fiery Chettinad curry with black pepper, fennel and roasted coconut.', 370.00, 'NON_VEG', 'HOT', 25, 'https://smartserve.anshuyadav.me/menu/chicken-chettinad.jpg'),
    ('Main Course', 'Chicken Vindaloo', 'Goan-style tangy, fiery curry with vinegar, garlic and red chillies.', 360.00, 'NON_VEG', 'HOT', 25, 'https://smartserve.anshuyadav.me/menu/chicken-vindaloo.jpg'),
    ('Main Course', 'Mutton Rogan Josh', 'Kashmiri slow-cooked mutton in a fragrant red chilli gravy.', 420.00, 'NON_VEG', 'MEDIUM', 35, 'https://smartserve.anshuyadav.me/menu/mutton-rogan-josh.jpg'),
    ('Main Course', 'Laal Maas', 'Rajasthani mutton curry with fiery Mathania red chillies.', 440.00, 'NON_VEG', 'HOT', 35, NULL),
    ('Main Course', 'Goan Fish Curry', 'Fish simmered in a tangy coconut and kokum curry.', 400.00, 'NON_VEG', 'MEDIUM', 25, 'https://smartserve.anshuyadav.me/menu/goan-fish-curry.jpg'),
    ('Main Course', 'Kerala Prawn Curry', 'Prawns in a coconut milk curry with curry leaves and kudampuli.', 450.00, 'NON_VEG', 'MEDIUM', 25, NULL),
    ('Main Course', 'Egg Curry', 'Boiled eggs in a spiced onion-tomato gravy.', 220.00, 'EGG', 'MEDIUM', 20, 'https://smartserve.anshuyadav.me/menu/egg-curry.jpg'),
    ('Biryani & Rice', 'Hyderabadi Chicken Biryani', 'Dum-cooked basmati and chicken layered with saffron, mint and fried onions.', 340.00, 'NON_VEG', 'MEDIUM', 30, 'https://smartserve.anshuyadav.me/menu/hyderabadi-chicken-biryani.jpg'),
    ('Biryani & Rice', 'Mutton Dum Biryani', 'Slow-cooked mutton and fragrant basmati sealed and cooked on dum.', 420.00, 'NON_VEG', 'MEDIUM', 35, 'https://smartserve.anshuyadav.me/menu/mutton-dum-biryani.jpg'),
    ('Biryani & Rice', 'Veg Biryani', 'Basmati layered with spiced vegetables, saffron and fried onions.', 260.00, 'VEG', 'MEDIUM', 25, 'https://smartserve.anshuyadav.me/menu/veg-biryani.jpg'),
    ('Biryani & Rice', 'Egg Biryani', 'Spiced biryani rice layered with masala boiled eggs.', 260.00, 'EGG', 'MEDIUM', 25, NULL),
    ('Biryani & Rice', 'Jeera Rice', 'Basmati rice tempered with cumin and ghee.', 150.00, 'VEG', 'NONE', 12, NULL),
    ('Biryani & Rice', 'Veg Pulao', 'Fragrant basmati cooked with vegetables and whole spices.', 180.00, 'VEG', 'MILD', 18, 'https://smartserve.anshuyadav.me/menu/veg-pulao.jpg'),
    ('Biryani & Rice', 'Steamed Rice', 'Plain steamed basmati rice.', 110.00, 'VEG', 'NONE', 10, 'https://smartserve.anshuyadav.me/menu/steamed-rice.jpg'),
    ('Indian Breads', 'Tandoori Roti', 'Whole-wheat flatbread baked in the tandoor.', 30.00, 'VEG', 'NONE', 8, NULL),
    ('Indian Breads', 'Rumali Roti', 'Paper-thin handkerchief bread cooked on an inverted tawa.', 40.00, 'VEG', 'NONE', 8, NULL),
    ('Indian Breads', 'Butter Naan', 'Soft leavened bread from the tandoor, brushed with butter.', 60.00, 'VEG', 'NONE', 8, 'https://smartserve.anshuyadav.me/menu/butter-naan.jpg'),
    ('Indian Breads', 'Garlic Naan', 'Tandoori naan topped with garlic and fresh coriander.', 70.00, 'VEG', 'NONE', 8, 'https://smartserve.anshuyadav.me/menu/garlic-naan.jpg'),
    ('Indian Breads', 'Lachha Paratha', 'Flaky, multi-layered whole-wheat paratha.', 60.00, 'VEG', 'NONE', 10, 'https://smartserve.anshuyadav.me/menu/lachha-paratha.jpg'),
    ('Indian Breads', 'Aloo Paratha', 'Whole-wheat paratha stuffed with spiced potato, served with curd and pickle.', 90.00, 'VEG', 'MILD', 12, 'https://smartserve.anshuyadav.me/menu/aloo-paratha.jpg'),
    ('Indian Breads', 'Amritsari Kulcha', 'Crisp stuffed kulcha from Amritsar, baked in the tandoor.', 110.00, 'VEG', 'MILD', 12, NULL),
    ('Indian Breads', 'Makki di Roti', 'Rustic Punjabi maize flatbread, best with sarson ka saag.', 50.00, 'VEG', 'NONE', 10, NULL),
    ('South Indian', 'Masala Dosa', 'Crisp rice crepe filled with spiced potato, with sambar and chutney.', 140.00, 'VEG', 'MILD', 15, 'https://smartserve.anshuyadav.me/menu/masala-dosa.jpg'),
    ('South Indian', 'Mysore Masala Dosa', 'Dosa spread with fiery red chutney and filled with potato masala.', 160.00, 'VEG', 'MEDIUM', 15, 'https://smartserve.anshuyadav.me/menu/mysore-masala-dosa.jpg'),
    ('South Indian', 'Idli Sambar', 'Three steamed rice cakes with sambar and coconut chutney.', 100.00, 'VEG', 'MILD', 10, 'https://smartserve.anshuyadav.me/menu/idli-sambar.jpg'),
    ('South Indian', 'Medu Vada', 'Crisp lentil doughnuts with sambar and coconut chutney.', 100.00, 'VEG', 'MILD', 12, 'https://smartserve.anshuyadav.me/menu/medu-vada.jpg'),
    ('South Indian', 'Onion Uttapam', 'Thick rice pancake topped with onions, chillies and coriander.', 140.00, 'VEG', 'MILD', 15, 'https://smartserve.anshuyadav.me/menu/onion-uttapam.jpg'),
    ('South Indian', 'Ven Pongal', 'Creamy rice and moong dal with ghee, pepper, cumin and cashews.', 120.00, 'VEG', 'MILD', 15, 'https://smartserve.anshuyadav.me/menu/ven-pongal.jpg'),
    ('South Indian', 'Appam with Vegetable Stew', 'Lacy Kerala rice hoppers with a mild coconut milk vegetable stew.', 180.00, 'VEG', 'MILD', 18, 'https://smartserve.anshuyadav.me/menu/appam-with-vegetable-stew.jpg'),
    ('South Indian', 'Bisi Bele Bath', 'Karnataka-style spiced rice, lentils and vegetables with ghee.', 150.00, 'VEG', 'MEDIUM', 15, 'https://smartserve.anshuyadav.me/menu/bisi-bele-bath.jpg'),
    ('South Indian', 'Lemon Rice', 'Rice tempered with lemon, mustard seeds, peanuts and curry leaves.', 130.00, 'VEG', 'MILD', 10, NULL),
    ('South Indian', 'Curd Rice', 'Cool rice mixed with yogurt, tempered with mustard and curry leaves.', 130.00, 'VEG', 'NONE', 10, NULL),
    ('Desserts', 'Rasmalai', 'Soft paneer discs soaked in saffron and cardamom milk with pistachios.', 130.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/rasmalai.jpg'),
    ('Desserts', 'Rasgulla', 'Spongy cottage cheese balls in light sugar syrup.', 90.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/rasgulla.jpg'),
    ('Desserts', 'Gajar ka Halwa', 'Slow-cooked carrot pudding with milk, ghee and nuts.', 120.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/gajar-ka-halwa.jpg'),
    ('Desserts', 'Jalebi', 'Crisp, syrup-soaked spirals, served warm.', 90.00, 'VEG', 'NONE', 8, 'https://smartserve.anshuyadav.me/menu/jalebi.jpg'),
    ('Desserts', 'Rice Kheer', 'Creamy rice pudding with cardamom, saffron and nuts.', 110.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/rice-kheer.jpg'),
    ('Desserts', 'Shrikhand', 'Thick strained yogurt sweetened with saffron and cardamom.', 110.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/shrikhand.jpg'),
    ('Desserts', 'Phirni', 'Chilled ground-rice pudding set in clay pots, with rose and pistachio.', 120.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/phirni.jpg'),
    ('Beverages', 'Sweet Lassi', 'Chilled, frothy sweetened yogurt drink.', 90.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/sweet-lassi.jpg'),
    ('Beverages', 'Masala Chaas', 'Spiced buttermilk with roasted cumin, ginger and mint.', 60.00, 'VEG', 'MILD', 5, NULL),
    ('Beverages', 'Filter Coffee', 'South Indian decoction coffee with frothy hot milk, served in a dabara.', 60.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/filter-coffee.jpg'),
    ('Beverages', 'Nimbu Pani', 'Fresh lime soda, sweet or salted, with a pinch of black salt.', 60.00, 'VEG', 'NONE', 5, 'https://smartserve.anshuyadav.me/menu/nimbu-pani.jpg'),
    ('Beverages', 'Jaljeera', 'Tangy cumin and mint cooler with tamarind and black salt.', 70.00, 'VEG', 'MILD', 5, 'https://smartserve.anshuyadav.me/menu/jaljeera.jpg'),
    ('Beverages', 'Thandai', 'Chilled milk with almonds, fennel, rose and saffron.', 120.00, 'VEG', 'NONE', 5, NULL),
    ('Beverages', 'Badam Milk', 'Warm or chilled almond milk with saffron and cardamom.', 100.00, 'VEG', 'NONE', 5, NULL)
) AS v(category, name, description, price, food_type, spice, prep, image_url)
JOIN menu_categories c ON LOWER(BTRIM(c.name)) = LOWER(BTRIM(v.category))
WHERE NOT EXISTS (
    SELECT 1 FROM menu_items m WHERE LOWER(BTRIM(m.name)) = LOWER(BTRIM(v.name))
);
