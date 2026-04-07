-- ============================================================
-- LISTENING CATEGORY — Insert data
-- Chạy file này sau khi app đã tạo bảng (ddl-auto=update).
-- Audio mp3 được generate bằng gTTS và đặt tại:
--   src/main/resources/static/audio/<filename>.mp3
-- ============================================================

-- ── 1. Category ─────────────────────────────────────────────
INSERT INTO category (id, title, vip_only) VALUES
  (4, 'Listening', TRUE);

-- ── 2. Levels ───────────────────────────────────────────────
INSERT INTO level (id, category_id, level_name) VALUES
  (401, 4, '1'),  
  (402, 4, '2');  


  INSERT INTO question (id, level_id, title, explaination, type, media_url) VALUES
    -- Level 1 – Beginner
    (4001, 401,
    'Listen to the audio. What time does the train leave?',
    'The speaker says "The train to London leaves at seven thirty in the morning from platform three."',
    'S', '/audio/listen_401_q1.mp3'),

    (4002, 401,
    'Listen to the directions. Which way should you turn at the traffic lights?',
    'The directions say to turn LEFT at the traffic lights, then go straight for two blocks.',
    'S', '/audio/listen_401_q2.mp3'),

    (4003, 401,
    'Listen to the order. Select ALL items the customer orders.',
    'The customer orders a large coffee with milk and two sugars, AND a blueberry muffin — two items.',
    'M', '/audio/listen_401_q3.mp3'),

    -- Level 2 – Intermediate
    (4004, 402,
    'Listen to the phone call. How many people is the table reservation for?',
    'The caller says "I need a table for FOUR people this Friday evening at seven o''clock."',
    'S', '/audio/listen_402_q1.mp3'),

    (4005, 402,
    'Listen to the weather forecast. What is predicted for the afternoon?',
    'The forecast mentions "a chance of heavy rain in the afternoon."',
    'S', '/audio/listen_402_q2.mp3'),

    (4006, 402,
    'Listen to the announcement. Select ALL correct statements about the library.',
    'The library closes on Monday (public holiday) and reopens on Tuesday from 9 a.m. to 8 p.m.',
    'M', '/audio/listen_402_q3.mp3');

  -- ── 4. Answers ──────────────────────────────────────────────
  -- q4001 – train departure time
  INSERT INTO answer (id, question_id, label, text, is_correct) VALUES
    (6001, 4001, 'A', '6:30 a.m.',FALSE),
    (6002, 4001, 'B', '7:30 a.m.',TRUE),
    (6003, 4001, 'C', '8:00 a.m.',FALSE),
    (6004, 4001, 'D', '7:00 p.m.',FALSE);

  -- q4002 – directions
  INSERT INTO answer (id, question_id, label, text, is_correct) VALUES
    (6005, 4002, 'A', 'Turn right',FALSE),
    (6006, 4002, 'B', 'Go straight',FALSE),
    (6007, 4002, 'C', 'Turn left',TRUE),
    (6008, 4002, 'D', 'Turn around',FALSE);

  -- q4003 – coffee order (multi-select: coffee + muffin)
  INSERT INTO answer (id, question_id, label, text, is_correct) VALUES
    (6009, 4003, 'A', 'A large coffee with milk and two sugars',TRUE),
    (6010, 4003, 'B', 'A slice of chocolate cake',FALSE),
    (6011, 4003, 'C', 'A blueberry muffin',TRUE),
    (6012, 4003, 'D', 'A bottle of water',FALSE);

  -- q4004 – reservation
  INSERT INTO answer (id, question_id, label, text, is_correct) VALUES
    (6013, 4004, 'A', '2 people',FALSE),
    (6014, 4004, 'B', '3 people',FALSE),
    (6015, 4004, 'C', '4 people',TRUE),
    (6016, 4004, 'D', '6 people',FALSE);

  -- q4005 – weather forecast afternoon
  INSERT INTO answer (id, question_id, label, text, is_correct) VALUES
    (6017, 4005, 'A', 'Sunny and hot',FALSE),
    (6018, 4005, 'B', 'Heavy rain',TRUE),
    (6019, 4005, 'C', 'Light snow',FALSE),
    (6020, 4005, 'D', 'Strong winds',FALSE);

  -- q4006 – library announcement (multi-select)
  INSERT INTO answer (id, question_id, label, text, is_correct) VALUES
    (6021, 4006, 'A', 'The library is closed on Monday',TRUE),
    (6022, 4006, 'B', 'The library is closed on Sunday',FALSE),
    (6023, 4006, 'C', 'The library reopens on Tuesday at 9 a.m.',TRUE),
    (6024, 4006, 'D', 'The library reopens on Wednesday',FALSE);
