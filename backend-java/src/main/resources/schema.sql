DROP TABLE IF EXISTS access_record;
DROP TABLE IF EXISTS plate_whitelist;
DROP TABLE IF EXISTS owner;
DROP TABLE IF EXISTS parking_space;

CREATE TABLE parking_space (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    space_code VARCHAR(50) NOT NULL UNIQUE,
    area VARCHAR(50),
    floor INT DEFAULT 1,
    status TINYINT DEFAULT 0 COMMENT '0空/1占用/2升降中',
    x_pos INT DEFAULT 0,
    y_pos INT DEFAULT 0,
    width INT DEFAULT 100,
    height INT DEFAULT 60,
    speaker_id VARCHAR(100) COMMENT '大门播报喇叭ID'
);

CREATE TABLE owner (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plate_whitelist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    owner_id BIGINT,
    space_id BIGINT,
    is_active TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES owner(id),
    FOREIGN KEY (space_id) REFERENCES parking_space(id)
);

CREATE TABLE access_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(20),
    space_id BIGINT,
    event_type VARCHAR(10) COMMENT 'IN/OUT',
    event_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO parking_space (space_code, area, floor, status, x_pos, y_pos, width, height, speaker_id) VALUES
('P001', 'A区', 1, 0, 50, 50, 100, 60, 'SPK-MAIN-01'),
('P002', 'A区', 1, 0, 180, 50, 100, 60, 'SPK-MAIN-01'),
('P003', 'A区', 1, 0, 310, 50, 100, 60, 'SPK-MAIN-02'),
('P004', 'A区', 1, 0, 440, 50, 100, 60, 'SPK-MAIN-02'),
('P005', 'B区', 1, 0, 50, 150, 100, 60, 'SPK-MAIN-01'),
('P006', 'B区', 1, 0, 180, 150, 100, 60, 'SPK-MAIN-01'),
('P007', 'B区', 1, 0, 310, 150, 100, 60, 'SPK-MAIN-02'),
('P008', 'B区', 1, 0, 440, 150, 100, 60, 'SPK-MAIN-02');

INSERT INTO owner (name, phone) VALUES
('张三', '13800138001'),
('李四', '13900139002');

INSERT INTO plate_whitelist (plate_number, owner_id, space_id, is_active) VALUES
('京A12345', 1, 1, 1),
('京B67890', 1, 2, 1),
('沪C11111', 2, 5, 1),
('粤D22222', 2, 6, 1);
