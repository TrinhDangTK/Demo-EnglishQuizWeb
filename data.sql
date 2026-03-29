DROP DATABASE IF EXISTS english_quiz;
CREATE DATABASE english_quiz;
USE english_quiz;

CREATE TABLE category (
  id INT PRIMARY KEY,
  title VARCHAR(255) NOT NULL
);

CREATE TABLE level (
  id INT PRIMARY KEY,
  category_id INT NOT NULL,
  level_name VARCHAR(50) NOT NULL,
  FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE question (
  id INT PRIMARY KEY,
  level_id INT NOT NULL,
  title TEXT NOT NULL,
  explaination TEXT,
  type VARCHAR(10) DEFAULT 'S',
  media_url VARCHAR(500),
  FOREIGN KEY (level_id) REFERENCES level(id)
);

CREATE TABLE answer (
  id INT PRIMARY KEY,
  question_id INT NOT NULL,
  label VARCHAR(10) NOT NULL,
  text TEXT NOT NULL,
  is_correct BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (question_id) REFERENCES question(id)
);

INSERT INTO category (id, title) VALUES
  (1, 'Grammar Basics'),
  (2, 'Vocabulary'),
  (3, 'Reading Comprehension');

INSERT INTO level (id, category_id, level_name) VALUES
  (101, 1, '1'),
  (102, 1, '2'),
  (201, 2, '1'),
  (202, 2, '2'),
  (301, 3, '1'),
  (302, 3, '2');

INSERT INTO question (id, level_id, title, explaination, type, media_url) VALUES
  (1001, 101, 'She ___ to school every day.', 'Use present simple with he/she/it: goes.', 'S', NULL),
  (1002, 101, 'Choose all nouns in this list.', 'Nouns are names of people, places, things, or ideas.', 'M', NULL),
  (1003, 102, 'If I ___ time, I will call you.', 'First conditional uses present simple in the if-clause.', 'S', NULL),
  (2001, 201, 'Which word means very big?', 'Huge means very big.', 'S', NULL),
  (2002, 201, 'Select all synonyms of happy.', 'Glad and joyful are synonyms of happy.', 'M', NULL),
  (2003, 202, 'The opposite of difficult is ___.', 'Easy is the antonym of difficult.', 'S', NULL),
  (3001, 301, 'Read: "Tom usually gets up at 6." What time does Tom usually get up?', 'The sentence states Tom gets up at 6.', 'S', NULL),
  (3002, 301, 'Select all statements that are TRUE about reading strategies.', 'Skimming gets the main idea; scanning finds details.', 'M', NULL),
  (3003, 302, 'Read: "Because it was raining, we stayed home." Why did they stay home?', 'They stayed home due to rain.', 'S', NULL);

INSERT INTO answer (id, question_id, label, text, is_correct) VALUES
  (5001, 1001, 'A', 'go', FALSE),
  (5002, 1001, 'B', 'goes', TRUE),
  (5003, 1001, 'C', 'going', FALSE),
  (5004, 1001, 'D', 'gone', FALSE),

  (5005, 1002, 'A', 'teacher', TRUE),
  (5006, 1002, 'B', 'quickly', FALSE),
  (5007, 1002, 'C', 'happiness', TRUE),
  (5008, 1002, 'D', 'run', FALSE),

  (5009, 1003, 'A', 'have', TRUE),
  (5010, 1003, 'B', 'had', FALSE),
  (5011, 1003, 'C', 'will have', FALSE),
  (5012, 1003, 'D', 'having', FALSE),

  (5013, 2001, 'A', 'tiny', FALSE),
  (5014, 2001, 'B', 'huge', TRUE),
  (5015, 2001, 'C', 'narrow', FALSE),
  (5016, 2001, 'D', 'short', FALSE),

  (5017, 2002, 'A', 'sad', FALSE),
  (5018, 2002, 'B', 'glad', TRUE),
  (5019, 2002, 'C', 'joyful', TRUE),
  (5020, 2002, 'D', 'angry', FALSE),

  (5021, 2003, 'A', 'hard', FALSE),
  (5022, 2003, 'B', 'easy', TRUE),
  (5023, 2003, 'C', 'complex', FALSE),
  (5024, 2003, 'D', 'tough', FALSE),

  (5025, 3001, 'A', '5 AM', FALSE),
  (5026, 3001, 'B', '6 AM', TRUE),
  (5027, 3001, 'C', '7 AM', FALSE),
  (5028, 3001, 'D', '8 AM', FALSE),

  (5029, 3002, 'A', 'Skimming helps find the main idea', TRUE),
  (5030, 3002, 'B', 'Scanning helps find specific details', TRUE),
  (5031, 3002, 'C', 'Reading backwards improves comprehension', FALSE),
  (5032, 3002, 'D', 'Speed reading reduces understanding', FALSE),

  (5033, 3003, 'A', 'They were tired', FALSE),
  (5034, 3003, 'B', 'It was raining', TRUE),
  (5035, 3003, 'C', 'They had no plans', FALSE),
  (5036, 3003, 'D', 'The house was comfortable', FALSE);

  -- Xem role va update role cho user hoac admin sau khi da co tai khoan
SELECT * FROM role;
-- Gán ADMIN cho user 'admin'
UPDATE user_account u
JOIN roles r ON r.name = 'ADMIN'
SET u.role_id = r.id
WHERE u.username = 'admin';
-- Gán USER cho user 'user1
UPDATE user_account u
JOIN roles r ON r.name = 'USER'
SET u.role_id = r.id
WHERE u.username = 'user1';