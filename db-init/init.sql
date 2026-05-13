
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) UNIQUE NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role ENUM('ADMIN', 'USER') DEFAULT 'USER',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_bet_limit (
  limit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  bet_date DATE NOT NULL,
  limit_count INT NOT NULL DEFAULT 0, 
  UNIQUE(user_id, bet_date), 
  CHECK(limit_count <= 20),
  FOREIGN KEY (user_id) REFERENCES users(id)
);


CREATE TABLE pairs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  symbol VARCHAR(50) UNIQUE NOT NULL
);


CREATE TABLE rounds (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pair_id BIGINT NOT NULL,
  open_price DECIMAL(18,8) NOT NULL,    
  target_up DECIMAL(18,8) NOT NULL,      
  target_down DECIMAL(18,8) NOT NULL,    
  status ENUM('VOTING', 'LOCKED', 'SETTLED', 'CANCELLED') DEFAULT 'VOTING',
  result ENUM('UP', 'DOWN', 'NA') DEFAULT 'NA',
  version INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
  settled_at TIMESTAMP NULL,  
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (pair_id) REFERENCES pairs(id)
);


CREATE TABLE bets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  round_id BIGINT NOT NULL,
  prediction ENUM('UP', 'DOWN') NOT NULL,
  status ENUM('PENDING', 'WIN', 'LOSE', 'TIE') DEFAULT 'PENDING', 
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT unique_user_round UNIQUE (user_id, round_id), 
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (round_id) REFERENCES rounds(id)
);


-- Seed data for Pairs (5 pairs)
INSERT INTO pairs (symbol) VALUES 
('BTCUSDT'), 
('ETHUSDT'), 
('BNBUSDT'), 
('SOLUSDT'), 
('ADAUSDT');

-- Seed data for Users (Password: admin)
INSERT INTO users (username, email, password, role) VALUES 
('admin', 'admin@example.com', '$2a$10$5Bz0BTqWLCjtesDSk.dxFuIxQdywj0BPnks7CEQuqe8LWyCVET7xu', 'ADMIN'),
('user', 'user@example.com', '$2a$10$5Bz0BTqWLCjtesDSk.dxFuIxQdywj0BPnks7CEQuqe8LWyCVET7xu', 'USER');
