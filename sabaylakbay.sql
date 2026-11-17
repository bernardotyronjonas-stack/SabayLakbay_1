-- =====================================================================
--  SabayLakbay database (MySQL 8 / MariaDB 10.4+, works with XAMPP)
--  Based on the project ERD. Redundant data removed:
--    * no group_members table: members = the organizer + APPROVED bookings
--    * no stored participant count or FULL/COMPLETED status: they are computed
--    * reviews have no reviewer column: the reviewer comes from the booking
--    * itinerary has no cost column: costs live in activity_expenses
--    * no payment table: payments are settled outside the app
--  Added because the app needs them (marked ADDED below):
--    users.phone, activities meeting place/time, transport, notes, image, created date,
--    group_messages.is_announcement, direct_messages, notifications,
--    reports.message_id / details / action taken / dates,
--    users address (street, barangay, city, province, ZIP), home pin and user_interests (personalized home page),
--    users.birthdate / gender / emergency contact and id_verifications.id_type / selfie (sign-up),
--    admin_logs (record of every admin action),
--    direct_messages.is_read and chat_reads (unread messages), activities meeting place pin (groups near you)
--
--  HOW TO RUN: phpMyAdmin > SQL tab > paste this whole file > Go
--  WARNING: running it again DELETES the database and starts fresh.
--
--  Sample logins:  traveler juan@sabaylakbay.com / 1234     admin admin@sabaylakbay.com / admin1234
-- =====================================================================

DROP DATABASE IF EXISTS sabaylakbay;
CREATE DATABASE sabaylakbay CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sabaylakbay;

CREATE TABLE users (
    user_id                   INT AUTO_INCREMENT PRIMARY KEY,
    full_name                 VARCHAR(100) NOT NULL,
    email                     VARCHAR(120) NOT NULL UNIQUE,
    password_hash             VARCHAR(64)  NOT NULL,                                        -- SHA-256 hash, never the plain password
    phone                     VARCHAR(20),                                                  -- ADDED (asked on Register)
    profile_picture           VARCHAR(255),
    birthdate                 DATE,                                                         -- ADDED (must be 18+ to sign up)
    gender                    VARCHAR(20),                                                  -- ADDED (optional: Female, Male, Prefer not to say)
    emergency_contact_name    VARCHAR(100),                                                 -- ADDED (who to call during a trip)
    emergency_contact_phone   VARCHAR(20),                                                  -- ADDED
    street                    VARCHAR(150),                                                 -- ADDED (house no., street, building)
    barangay                  VARCHAR(100),                                                 -- ADDED
    city                      VARCHAR(100),                                                 -- ADDED (city / municipality)
    province                  VARCHAR(100),                                                 -- ADDED (or Metro Manila)
    zip_code                  VARCHAR(4),                                                   -- ADDED
    latitude                  DECIMAL(9,6),                                                 -- ADDED (optional home pin, for trips near you)
    longitude                 DECIMAL(9,6),                                                 -- ADDED
    role                      ENUM('TRAVELER','ADMIN') NOT NULL DEFAULT 'TRAVELER',
    account_status            ENUM('ACTIVE','SUSPENDED','REMOVED') NOT NULL DEFAULT 'ACTIVE',
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ADDED: what each traveler likes (same names as category), asked on sign-up
CREATE TABLE user_interests (
    user_id                   INT NOT NULL,
    category                  VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, category),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE id_verifications (
    verification_id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT NOT NULL,
    id_type                   VARCHAR(40),                                                  -- ADDED (National ID, Driver's License, Passport...)
    id_photo_path             VARCHAR(255) NOT NULL,
    selfie_path               VARCHAR(255),                                                 -- ADDED (face photo from the camera, compared with the ID)
    status                    ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by               INT NULL,                                                     -- the admin who checked it
    reviewed_at               DATETIME NULL,
    FOREIGN KEY (user_id)     REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- an "activity" is a travel group / trip
CREATE TABLE activities (
    activity_id               INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT NOT NULL,
    title                     VARCHAR(120) NOT NULL,
    category                  VARCHAR(40)  NOT NULL,                                        -- Hiking, Beach, Food trip ...
    description               TEXT,
    destination               VARCHAR(120) NOT NULL,
    start_date                DATE NOT NULL,
    end_date                  DATE NOT NULL,
    difficulty                ENUM('EASY','MODERATE','HARD') NOT NULL DEFAULT 'EASY',
    price_per_person          DECIMAL(10,2) NOT NULL,                                       -- estimated budget per person
    max_participants          INT NOT NULL,
    status                    ENUM('OPEN','CANCELLED') NOT NULL DEFAULT 'OPEN',             -- full / completed are computed
    meeting_place             VARCHAR(200),                                                 -- ADDED
    meeting_time              TIME,                                                         -- ADDED
    transport_mode            VARCHAR(60),                                                  -- ADDED
    notes                     TEXT,                                                         -- ADDED (notes from the organizer)
    image_path                VARCHAR(255),                                                 -- ADDED (cover photo)
    latitude                  DECIMAL(9,6),                                                 -- ADDED (map pin of the destination)
    longitude                 DECIMAL(9,6),                                                 -- ADDED (map pin of the destination)
    meeting_latitude          DECIMAL(9,6),                                                 -- ADDED (map pin of the meeting place: "groups near you")
    meeting_longitude         DECIMAL(9,6),                                                 -- ADDED
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,                  -- ADDED (sort by newest, analytics)
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_activities_dates        CHECK (end_date >= start_date),
    CONSTRAINT chk_activities_participants CHECK (max_participants >= 2),
    CONSTRAINT chk_activities_price        CHECK (price_per_person > 0)
);

CREATE TABLE itinerary (
    itinerary_id              INT AUTO_INCREMENT PRIMARY KEY,
    activity_id               INT NOT NULL,
    activity_name             VARCHAR(150) NOT NULL,
    location                  VARCHAR(150),
    scheduled_time            DATETIME NOT NULL,
    details                   VARCHAR(500),
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id) ON DELETE CASCADE
);

CREATE TABLE activity_expenses (
    expense_id                INT AUTO_INCREMENT PRIMARY KEY,
    activity_id               INT NOT NULL,
    name                      VARCHAR(120) NOT NULL,
    estimated_amount          DECIMAL(10,2) NOT NULL,                                       -- per person
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id) ON DELETE CASCADE
);

-- a booking is a request to join a trip. APPROVED bookings are the members.
CREATE TABLE bookings (
    booking_id                INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT NOT NULL,
    activity_id               INT NOT NULL,
    request_message           VARCHAR(500),
    status                    ENUM('PENDING','APPROVED','REJECTED','CANCELLED','LEFT') NOT NULL DEFAULT 'PENDING',
    requested_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)     REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id) ON DELETE CASCADE
);

CREATE TABLE group_messages (
    message_id                INT AUTO_INCREMENT PRIMARY KEY,
    activity_id               INT NOT NULL,
    user_id                   INT NOT NULL,
    content                   TEXT NOT NULL,
    is_announcement           BOOLEAN NOT NULL DEFAULT FALSE,                               -- ADDED (trip updates from the organizer)
    sent_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)   REFERENCES users(user_id) ON DELETE CASCADE
);

-- ADDED: private messages, e.g. contacting the group organizer
CREATE TABLE direct_messages (
    dm_id                     INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT NOT NULL,
    receiver_id               INT NOT NULL,
    content                   TEXT NOT NULL,
    sent_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read                   BOOLEAN NOT NULL DEFAULT FALSE,                               -- ADDED (unread badge on Messages)
    FOREIGN KEY (user_id)   REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ADDED: the last group message each member has seen, so the app knows what is unread
CREATE TABLE chat_reads (
    user_id                   INT NOT NULL,
    activity_id               INT NOT NULL,
    last_read_message_id      INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, activity_id),
    FOREIGN KEY (user_id)     REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id) ON DELETE CASCADE
);

-- What was reported is the one id that is filled in: a user, an activity, or one group message.
CREATE TABLE reports (
    report_id                 INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT NOT NULL,
    reported_user_id          INT NULL,
    activity_id               INT NULL,
    message_id                INT NULL,                                                     -- ADDED (a reported group message)
    reason                    VARCHAR(100) NOT NULL,
    details                   TEXT,                                                         -- ADDED
    status                    ENUM('PENDING','UNDER_REVIEW','RESOLVED','DISMISSED') NOT NULL DEFAULT 'PENDING',
    action_taken              VARCHAR(100),                                                 -- ADDED
    reviewed_by               INT NULL,
    reviewed_at               DATETIME NULL,                                                -- ADDED
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,                  -- ADDED
    FOREIGN KEY (user_id)      REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (reported_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (activity_id)      REFERENCES activities(activity_id) ON DELETE SET NULL,
    FOREIGN KEY (message_id)       REFERENCES group_messages(message_id) ON DELETE SET NULL,
    FOREIGN KEY (reviewed_by)      REFERENCES users(user_id) ON DELETE SET NULL
);

-- reviewed_user_id NULL = a review of the trip itself
CREATE TABLE reviews (
    review_id                 INT AUTO_INCREMENT PRIMARY KEY,
    booking_id                INT NOT NULL,                                                 -- the reviewer and the trip come from here
    reviewed_user_id          INT NULL,
    rating                    TINYINT NOT NULL CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    comment                   VARCHAR(1000),
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id)       REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE blocked_users (
    block_id                  INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT NOT NULL,
    blocked_user_id           INT NOT NULL,
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, blocked_user_id),
    FOREIGN KEY (user_id)      REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (blocked_user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ADDED: request approved / rejected, new join requests, trip updates
CREATE TABLE notifications (
    notification_id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT NOT NULL,
    message                   VARCHAR(500) NOT NULL,
    activity_id               INT NULL,
    is_read                   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)     REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id) ON DELETE SET NULL
);

-- ADDED: record of every admin action (ID checks, suspensions, cancelled trips, announcements...)
CREATE TABLE admin_logs (
    log_id                    INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT NULL,
    action                    VARCHAR(40)  NOT NULL,
    target                    VARCHAR(150),
    details                   VARCHAR(500),
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX idx_activities_dates   ON activities(status, end_date, start_date);
CREATE INDEX idx_bookings_activity  ON bookings(activity_id, status);
CREATE INDEX idx_bookings_user      ON bookings(user_id, status);
CREATE INDEX idx_messages_activity  ON group_messages(activity_id, sent_at);
CREATE INDEX idx_dm_pair            ON direct_messages(user_id, receiver_id, sent_at);
CREATE INDEX idx_reports_status     ON reports(status, created_at);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

-- =====================================================================
--  SAMPLE DATA (dates are relative to today, so trips stay "upcoming")
--  Every sample traveler's password is 1234. Admin: admin1234.
-- =====================================================================

INSERT INTO users (user_id, full_name, email, password_hash, phone, profile_picture, role, account_status, created_at) VALUES
(1, 'Juan Dela Cruz', 'juan@sabaylakbay.com', SHA2('1234', 256), '0917 000 0001', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 28800 MINUTE)),
(2, 'Mark Santos', 'mark@sabaylakbay.com', SHA2('1234', 256), '0917 000 0002', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 53280 MINUTE)),
(3, 'Ana Reyes', 'ana@sabaylakbay.com', SHA2('1234', 256), '0917 000 0003', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 77760 MINUTE)),
(4, 'Carlo Mendoza', 'carlo@sabaylakbay.com', SHA2('1234', 256), '0917 000 0004', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 102240 MINUTE)),
(5, 'Bea Lim', 'bea@sabaylakbay.com', SHA2('1234', 256), '0917 000 0005', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 126720 MINUTE)),
(6, 'Paolo Cruz', 'paolo@sabaylakbay.com', SHA2('1234', 256), '0917 000 0006', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 151200 MINUTE)),
(7, 'Mika Tan', 'mika@sabaylakbay.com', SHA2('1234', 256), '0917 000 0007', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 175680 MINUTE)),
(8, 'Jerome Villanueva', 'jerome@sabaylakbay.com', SHA2('1234', 256), '0917 000 0008', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 200160 MINUTE)),
(9, 'Lara Gomez', 'lara@sabaylakbay.com', SHA2('1234', 256), '0917 000 0009', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 224640 MINUTE)),
(10, 'Nico Ramos', 'nico@sabaylakbay.com', SHA2('1234', 256), '0917 000 0010', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 249120 MINUTE)),
(11, 'Trish Castillo', 'trish@sabaylakbay.com', SHA2('1234', 256), '0917 000 0011', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 273600 MINUTE)),
(12, 'Ken Abad', 'ken@sabaylakbay.com', SHA2('1234', 256), '0917 000 0012', NULL, 'TRAVELER', 'SUSPENDED', DATE_SUB(NOW(), INTERVAL 298080 MINUTE)),
(13, 'Dan Ortega', 'dan@sabaylakbay.com', SHA2('1234', 256), '0917 000 0013', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 322560 MINUTE)),
(14, 'Ella Navarro', 'ella@sabaylakbay.com', SHA2('1234', 256), '0917 000 0014', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 347040 MINUTE)),
(15, 'Angela Aquino', 'angela15@mail.com', SHA2('1234', 256), '0918 015 0555', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 489600 MINUTE)),
(16, 'Carlos Castro', 'carlos16@mail.com', SHA2('1234', 256), '0918 016 0592', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 480960 MINUTE)),
(17, 'Lea Torres', 'lea17@mail.com', SHA2('1234', 256), '0918 017 0629', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 449280 MINUTE)),
(18, 'Leo Salazar', 'leo18@mail.com', SHA2('1234', 256), '0918 018 0666', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 460800 MINUTE)),
(19, 'Ivy Domingo', 'ivy19@mail.com', SHA2('1234', 256), '0918 019 0703', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 390240 MINUTE)),
(20, 'Gabriel Garcia', 'gabriel20@mail.com', SHA2('1234', 256), '0918 020 0740', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 391680 MINUTE)),
(21, 'Bianca Ramos', 'bianca21@mail.com', SHA2('1234', 256), '0918 021 0777', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 383040 MINUTE)),
(22, 'Adrian Navarro', 'adrian22@mail.com', SHA2('1234', 256), '0918 022 0814', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 370080 MINUTE)),
(23, 'Erika Mercado', 'erika23@mail.com', SHA2('1234', 256), '0918 023 0851', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 349920 MINUTE)),
(24, 'Jake Rivera', 'jake24@mail.com', SHA2('1234', 256), '0918 024 0888', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 318240 MINUTE)),
(25, 'Grace Pascual', 'grace25@mail.com', SHA2('1234', 256), '0918 025 0925', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 328320 MINUTE)),
(26, 'Sam Morales', 'sam26@mail.com', SHA2('1234', 256), '0918 026 0962', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 303840 MINUTE)),
(27, 'Camille Reyes', 'camille27@mail.com', SHA2('1234', 256), '0918 027 0999', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 282240 MINUTE)),
(28, 'Luis Bautista', 'luis28@mail.com', SHA2('1234', 256), '0918 028 1036', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 267840 MINUTE)),
(29, 'Hannah Villanueva', 'hannah29@mail.com', SHA2('1234', 256), '0918 029 1073', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 260640 MINUTE)),
(30, 'Ramon Flores', 'ramon30@mail.com', SHA2('1234', 256), '0918 030 1110', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 218880 MINUTE)),
(31, 'Faith Dizon', 'faith31@mail.com', SHA2('1234', 256), '0918 031 1147', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 234720 MINUTE)),
(32, 'Ryan Valdez', 'ryan32@mail.com', SHA2('1234', 256), '0918 032 1184', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 234720 MINUTE)),
(33, 'Sofia Soriano', 'sofia33@mail.com', SHA2('1234', 256), '0918 033 1221', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 218880 MINUTE)),
(34, 'Joshua Santos', 'joshua34@mail.com', SHA2('1234', 256), '0918 034 1258', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 182880 MINUTE)),
(35, 'Jasmine Aquino', 'jasmine35@mail.com', SHA2('1234', 256), '0918 035 1295', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 175680 MINUTE)),
(36, 'Joaquin Castro', 'joaquin36@mail.com', SHA2('1234', 256), '0918 036 1332', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 197280 MINUTE)),
(37, 'Mae Torres', 'mae37@mail.com', SHA2('1234', 256), '0918 037 1369', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 191520 MINUTE)),
(38, 'Nathan Salazar', 'nathan38@mail.com', SHA2('1234', 256), '0918 038 1406', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 131040 MINUTE)),
(39, 'Ella Domingo', 'ella39@mail.com', SHA2('1234', 256), '0918 039 1443', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 167040 MINUTE)),
(40, 'Rafael Garcia', 'rafael40@mail.com', SHA2('1234', 256), '0918 040 1480', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 155520 MINUTE)),
(41, 'Patricia Ramos', 'patricia41@mail.com', SHA2('1234', 256), '0918 041 1517', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 133920 MINUTE)),
(42, 'Vince Navarro', 'vince42@mail.com', SHA2('1234', 256), '0918 042 1554', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 139680 MINUTE)),
(43, 'Kaye Mercado', 'kaye43@mail.com', SHA2('1234', 256), '0918 043 1591', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 115200 MINUTE)),
(44, 'Ian Rivera', 'ian44@mail.com', SHA2('1234', 256), '0918 044 1628', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 115200 MINUTE)),
(45, 'Pia Pascual', 'pia45@mail.com', SHA2('1234', 256), '0918 045 1665', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 112320 MINUTE)),
(46, 'Miguel Morales', 'miguel46@mail.com', SHA2('1234', 256), '0918 046 1702', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 87840 MINUTE)),
(47, 'Andrea Reyes', 'andrea47@mail.com', SHA2('1234', 256), '0918 047 1739', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 112320 MINUTE)),
(48, 'Marco Bautista', 'marco48@mail.com', SHA2('1234', 256), '0918 048 1776', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 69120 MINUTE)),
(49, 'Rina Villanueva', 'rina49@mail.com', SHA2('1234', 256), '0918 049 1813', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 60480 MINUTE)),
(50, 'Enzo Flores', 'enzo50@mail.com', SHA2('1234', 256), '0918 050 1850', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 44640 MINUTE)),
(51, 'Joy Dizon', 'joy51@mail.com', SHA2('1234', 256), '0918 051 1887', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 53280 MINUTE)),
(52, 'Mark Valdez', 'mark52@mail.com', SHA2('1234', 256), '0918 052 1924', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 44640 MINUTE)),
(53, 'Isabel Soriano', 'isabel53@mail.com', SHA2('1234', 256), '0918 053 1961', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 67680 MINUTE)),
(54, 'Kevin Santos', 'kevin54@mail.com', SHA2('1234', 256), '0918 054 1998', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 38880 MINUTE)),
(55, 'Nicole Aquino', 'nicole55@mail.com', SHA2('1234', 256), '0918 055 2035', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(56, 'Daniel Castro', 'daniel56@mail.com', SHA2('1234', 256), '0918 056 2072', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 12960 MINUTE)),
(57, 'Abby Torres', 'abby57@mail.com', SHA2('1234', 256), '0918 057 2109', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 18720 MINUTE)),
(58, 'Aaron Salazar', 'aaron58@mail.com', SHA2('1234', 256), '0918 058 2146', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(59, 'Trina Domingo', 'trina59@mail.com', SHA2('1234', 256), '0918 059 2183', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 24480 MINUTE)),
(60, 'Paolo Garcia', 'paolo60@mail.com', SHA2('1234', 256), '0918 060 2220', NULL, 'TRAVELER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 4320 MINUTE)),
(99, 'Admin Team', 'admin@sabaylakbay.com', SHA2('admin1234', 256), '0917 000 0099', NULL, 'ADMIN', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 576000 MINUTE));

-- ID checks (the sample files don't exist, the admin screen shows a placeholder)
INSERT INTO id_verifications (user_id, id_photo_path, status, reviewed_by, reviewed_at) VALUES
(1, 'uploads/ids/sample-id-1.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(2, 'uploads/ids/sample-id-2.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(3, 'uploads/ids/sample-id-3.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(4, 'uploads/ids/sample-id-4.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(5, 'uploads/ids/sample-id-5.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(6, 'uploads/ids/sample-id-6.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(7, 'uploads/ids/sample-id-7.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(8, 'uploads/ids/sample-id-8.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(9, 'uploads/ids/sample-id-9.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(10, 'uploads/ids/sample-id-10.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(11, 'uploads/ids/sample-id-11.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(12, 'uploads/ids/sample-id-12.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(13, 'uploads/ids/sample-id-13.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(14, 'uploads/ids/sample-id-14.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(15, 'uploads/ids/sample-id-15.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(16, 'uploads/ids/sample-id-16.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(17, 'uploads/ids/sample-id-17.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(18, 'uploads/ids/sample-id-18.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(19, 'uploads/ids/sample-id-19.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(20, 'uploads/ids/sample-id-20.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(21, 'uploads/ids/sample-id-21.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(22, 'uploads/ids/sample-id-22.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(23, 'uploads/ids/sample-id-23.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(24, 'uploads/ids/sample-id-24.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(25, 'uploads/ids/sample-id-25.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(26, 'uploads/ids/sample-id-26.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(27, 'uploads/ids/sample-id-27.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(28, 'uploads/ids/sample-id-28.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(29, 'uploads/ids/sample-id-29.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(30, 'uploads/ids/sample-id-30.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(31, 'uploads/ids/sample-id-31.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(32, 'uploads/ids/sample-id-32.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(33, 'uploads/ids/sample-id-33.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(34, 'uploads/ids/sample-id-34.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(35, 'uploads/ids/sample-id-35.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(36, 'uploads/ids/sample-id-36.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(37, 'uploads/ids/sample-id-37.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(38, 'uploads/ids/sample-id-38.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(39, 'uploads/ids/sample-id-39.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(40, 'uploads/ids/sample-id-40.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(41, 'uploads/ids/sample-id-41.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(42, 'uploads/ids/sample-id-42.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(43, 'uploads/ids/sample-id-43.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(44, 'uploads/ids/sample-id-44.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(45, 'uploads/ids/sample-id-45.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(46, 'uploads/ids/sample-id-46.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(47, 'uploads/ids/sample-id-47.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(48, 'uploads/ids/sample-id-48.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(49, 'uploads/ids/sample-id-49.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(50, 'uploads/ids/sample-id-50.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(51, 'uploads/ids/sample-id-51.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(52, 'uploads/ids/sample-id-52.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(53, 'uploads/ids/sample-id-53.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(54, 'uploads/ids/sample-id-54.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(55, 'uploads/ids/sample-id-55.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(56, 'uploads/ids/sample-id-56.jpg', 'APPROVED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(57, 'uploads/ids/sample-id-57.jpg', 'REJECTED', 99, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(58, 'uploads/ids/sample-id-58.jpg', 'PENDING', NULL, NULL),
(59, 'uploads/ids/sample-id-59.jpg', 'PENDING', NULL, NULL),
(60, 'uploads/ids/sample-id-60.jpg', 'PENDING', NULL, NULL);

-- activities 1-8 are the main sample trips, 9+ are completed trips from the past year
INSERT INTO activities (activity_id, user_id, title, category, description, destination, start_date, end_date, difficulty, price_per_person, max_participants, meeting_place, meeting_time, transport_mode, notes, image_path, created_at) VALUES
(1, 2, 'Mt. Mayon Adventure', 'Hiking', 'Three days around Mayon: Cagsawa Ruins, Lignon Hill, ATV lava trail and Bicol food trip.', 'Albay', DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY), 'MODERATE', 1600, 10, 'Cubao Bus Terminal, Quezon City', '20:00:00', 'Bus', 'Bring a light jacket and comfy shoes. Budget covers bus, ATV and lodging. Food is separate.', '/images/mayon.jpg', DATE_SUB(NOW(), INTERVAL 4320 MINUTE)),
(2, 3, 'Island Hopping - Caramoan', 'Beach', 'Hop around Caramoan''s islands: Matukad, Lahos and Sabitang Laya. Snorkeling and beach camping.', 'Camarines Sur', DATE_ADD(CURDATE(), INTERVAL 19 DAY), DATE_ADD(CURDATE(), INTERVAL 22 DAY), 'EASY', 1200, 8, 'Naga City Plaza', '05:00:00', 'Van + Boat', 'Waterproof bag is a must. Bring your own snorkel if you have one.', '/images/caramoan.jpg', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(3, 4, 'Trekking + Falls', 'Hiking', 'Day hike to Daranak and Batlag Falls with a short trek. Beginner friendly.', 'Tanay, Rizal', DATE_ADD(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'EASY', 900, 6, 'Starmall EDSA Shaw, Mandaluyong', '04:30:00', 'Van', 'Wear trekking sandals. We leave on time, so please be early.', '/images/falls.jpg', DATE_SUB(NOW(), INTERVAL 7200 MINUTE)),
(4, 1, 'Baguio Food Trip', 'Food trip', 'Cafes, Good Shepherd, night market, strawberry farm. Chill trip, lots of eating.', 'Benguet', DATE_ADD(CURDATE(), INTERVAL 25 DAY), DATE_ADD(CURDATE(), INTERVAL 27 DAY), 'EASY', 2500, 8, 'SM North EDSA, The Block entrance', '06:00:00', 'Private car', 'Two cars, we split gas and toll. Bring a jacket, it gets cold at night.', '/images/baguio.jpg', DATE_SUB(NOW(), INTERVAL 2880 MINUTE)),
(5, 5, 'Siargao Surf Week', 'Beach', 'Surf lessons at Cloud 9, island hopping and Sugba Lagoon.', 'Surigao del Norte', DATE_ADD(CURDATE(), INTERVAL 40 DAY), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'MODERATE', 8500, 6, 'NAIA Terminal 3', '05:30:00', 'Plane + Van', 'Flights not included in the budget. Book your own ticket after approval.', '/images/siargao.jpg', DATE_SUB(NOW(), INTERVAL 11520 MINUTE)),
(6, 6, 'Batanes Escape', 'Sightseeing', 'North and South Batan tour, Sabtang Island and Marlboro Hills.', 'Batanes', DATE_ADD(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 64 DAY), 'EASY', 15000, 10, 'NAIA Terminal 2', '04:00:00', 'Plane', 'Weather can delay flights. Keep your schedule flexible.', '/images/batanes.jpg', DATE_SUB(NOW(), INTERVAL 14400 MINUTE)),
(7, 7, 'Coron Weekend', 'Island hopping', 'Kayangan Lake, Twin Lagoon and the shipwreck snorkel tour.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 17 DAY), 'MODERATE', 5200, 8, 'NAIA Terminal 3', '05:00:00', 'Plane + Boat', 'Done! Please leave a review for the group.', '/images/coron.jpg', DATE_SUB(NOW(), INTERVAL 57600 MINUTE)),
(8, 8, 'Pagudpud Road Trip', 'Road trip', 'Bangui windmills, Kapurpurawan, Blue Lagoon and empanada stops.', 'Ilocos Norte', DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 16 DAY), 'EASY', 3000, 5, 'NLEX Balintawak Toll Plaza', '03:00:00', 'Private car', 'We split gas and toll equally.', '/images/pagudpud.jpg', DATE_SUB(NOW(), INTERVAL 1200 MINUTE)),
(9, 3, 'Sagada and Baguio', 'Sightseeing', 'Past trip.', 'Benguet', DATE_SUB(CURDATE(), INTERVAL 327 DAY), DATE_SUB(CURDATE(), INTERVAL 326 DAY), 'EASY', 1200, 3, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 499680 MINUTE)),
(10, 7, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 334 DAY), DATE_SUB(CURDATE(), INTERVAL 332 DAY), 'MODERATE', 1800, 4, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 499680 MINUTE)),
(11, 5, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 294 DAY), DATE_SUB(CURDATE(), INTERVAL 291 DAY), 'HARD', 4500, 6, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 444960 MINUTE)),
(12, 2, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 302 DAY), DATE_SUB(CURDATE(), INTERVAL 302 DAY), 'HARD', 6000, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 453600 MINUTE)),
(13, 2, 'Bohol Countryside', 'Sightseeing', 'Past trip.', 'Bohol', DATE_SUB(CURDATE(), INTERVAL 267 DAY), DATE_SUB(CURDATE(), INTERVAL 267 DAY), 'HARD', 3500, 6, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 410400 MINUTE)),
(14, 2, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 281 DAY), DATE_SUB(CURDATE(), INTERVAL 278 DAY), 'MODERATE', 6000, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 416160 MINUTE)),
(15, 4, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 276 DAY), DATE_SUB(CURDATE(), INTERVAL 275 DAY), 'EASY', 1200, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 413280 MINUTE)),
(16, 3, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 240 DAY), DATE_SUB(CURDATE(), INTERVAL 237 DAY), 'MODERATE', 3500, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 355680 MINUTE)),
(17, 3, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 244 DAY), DATE_SUB(CURDATE(), INTERVAL 241 DAY), 'EASY', 1200, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 380160 MINUTE)),
(18, 3, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 234 DAY), DATE_SUB(CURDATE(), INTERVAL 234 DAY), 'HARD', 900, 4, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 360000 MINUTE)),
(19, 2, 'Sagada and Baguio', 'Sightseeing', 'Past trip.', 'Benguet', DATE_SUB(CURDATE(), INTERVAL 197 DAY), DATE_SUB(CURDATE(), INTERVAL 195 DAY), 'MODERATE', 2500, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 308160 MINUTE)),
(20, 6, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 202 DAY), DATE_SUB(CURDATE(), INTERVAL 200 DAY), 'MODERATE', 1500, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 316800 MINUTE)),
(21, 9, 'Cebu Canyoneering', 'Adventure', 'Past trip.', 'Cebu', DATE_SUB(CURDATE(), INTERVAL 194 DAY), DATE_SUB(CURDATE(), INTERVAL 192 DAY), 'HARD', 1800, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 305280 MINUTE)),
(22, 4, 'Tanay Sea of Clouds', 'Hiking', 'Past trip.', 'Tanay, Rizal', DATE_SUB(CURDATE(), INTERVAL 214 DAY), DATE_SUB(CURDATE(), INTERVAL 213 DAY), 'EASY', 3500, 11, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 335520 MINUTE)),
(23, 3, 'Cebu Canyoneering', 'Adventure', 'Past trip.', 'Cebu', DATE_SUB(CURDATE(), INTERVAL 183 DAY), DATE_SUB(CURDATE(), INTERVAL 182 DAY), 'MODERATE', 900, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 275040 MINUTE)),
(24, 3, 'Tanay Sea of Clouds', 'Hiking', 'Past trip.', 'Tanay, Rizal', DATE_SUB(CURDATE(), INTERVAL 162 DAY), DATE_SUB(CURDATE(), INTERVAL 162 DAY), 'MODERATE', 1200, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 262080 MINUTE)),
(25, 3, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 164 DAY), DATE_SUB(CURDATE(), INTERVAL 163 DAY), 'HARD', 1500, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 262080 MINUTE)),
(26, 2, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 188 DAY), DATE_SUB(CURDATE(), INTERVAL 188 DAY), 'MODERATE', 1800, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 283680 MINUTE)),
(27, 5, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 144 DAY), DATE_SUB(CURDATE(), INTERVAL 143 DAY), 'HARD', 3500, 9, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 228960 MINUTE)),
(28, 9, 'Tanay Sea of Clouds', 'Hiking', 'Past trip.', 'Tanay, Rizal', DATE_SUB(CURDATE(), INTERVAL 157 DAY), DATE_SUB(CURDATE(), INTERVAL 156 DAY), 'EASY', 1500, 11, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 239040 MINUTE)),
(29, 3, 'Sagada and Baguio', 'Sightseeing', 'Past trip.', 'Benguet', DATE_SUB(CURDATE(), INTERVAL 143 DAY), DATE_SUB(CURDATE(), INTERVAL 140 DAY), 'HARD', 6000, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 230400 MINUTE)),
(30, 7, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 154 DAY), DATE_SUB(CURDATE(), INTERVAL 153 DAY), 'MODERATE', 1800, 10, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 243360 MINUTE)),
(31, 2, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 148 DAY), DATE_SUB(CURDATE(), INTERVAL 146 DAY), 'EASY', 1500, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 233280 MINUTE)),
(32, 3, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 110 DAY), DATE_SUB(CURDATE(), INTERVAL 109 DAY), 'MODERATE', 3500, 10, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 178560 MINUTE)),
(33, 2, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 104 DAY), DATE_SUB(CURDATE(), INTERVAL 102 DAY), 'MODERATE', 2500, 6, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 175680 MINUTE)),
(34, 2, 'Cebu Canyoneering', 'Adventure', 'Past trip.', 'Cebu', DATE_SUB(CURDATE(), INTERVAL 135 DAY), DATE_SUB(CURDATE(), INTERVAL 135 DAY), 'HARD', 2500, 9, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 208800 MINUTE)),
(35, 5, 'Sagada and Baguio', 'Sightseeing', 'Past trip.', 'Benguet', DATE_SUB(CURDATE(), INTERVAL 120 DAY), DATE_SUB(CURDATE(), INTERVAL 117 DAY), 'EASY', 4500, 4, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 195840 MINUTE)),
(36, 2, 'Bohol Countryside', 'Sightseeing', 'Past trip.', 'Bohol', DATE_SUB(CURDATE(), INTERVAL 132 DAY), DATE_SUB(CURDATE(), INTERVAL 130 DAY), 'HARD', 4500, 10, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 201600 MINUTE)),
(37, 3, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 102 DAY), DATE_SUB(CURDATE(), INTERVAL 102 DAY), 'MODERATE', 1800, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 161280 MINUTE)),
(38, 2, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 84 DAY), DATE_SUB(CURDATE(), INTERVAL 82 DAY), 'EASY', 6000, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 136800 MINUTE)),
(39, 10, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 97 DAY), DATE_SUB(CURDATE(), INTERVAL 94 DAY), 'EASY', 1500, 10, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 158400 MINUTE)),
(40, 7, 'Cebu Canyoneering', 'Adventure', 'Past trip.', 'Cebu', DATE_SUB(CURDATE(), INTERVAL 87 DAY), DATE_SUB(CURDATE(), INTERVAL 87 DAY), 'HARD', 2500, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 135360 MINUTE)),
(41, 7, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 86 DAY), DATE_SUB(CURDATE(), INTERVAL 84 DAY), 'HARD', 3500, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 136800 MINUTE)),
(42, 3, 'Cebu Canyoneering', 'Adventure', 'Past trip.', 'Cebu', DATE_SUB(CURDATE(), INTERVAL 89 DAY), DATE_SUB(CURDATE(), INTERVAL 87 DAY), 'MODERATE', 2500, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 142560 MINUTE)),
(43, 2, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 52 DAY), DATE_SUB(CURDATE(), INTERVAL 50 DAY), 'MODERATE', 1800, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 86400 MINUTE)),
(44, 5, 'Batanes North Loop', 'Sightseeing', 'Past trip.', 'Batanes', DATE_SUB(CURDATE(), INTERVAL 63 DAY), DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'EASY', 4500, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 118080 MINUTE)),
(45, 2, 'Tanay Sea of Clouds', 'Hiking', 'Past trip.', 'Tanay, Rizal', DATE_SUB(CURDATE(), INTERVAL 59 DAY), DATE_SUB(CURDATE(), INTERVAL 58 DAY), 'EASY', 4500, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 112320 MINUTE)),
(46, 6, 'Sagada and Baguio', 'Sightseeing', 'Past trip.', 'Benguet', DATE_SUB(CURDATE(), INTERVAL 65 DAY), DATE_SUB(CURDATE(), INTERVAL 64 DAY), 'HARD', 1200, 9, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 115200 MINUTE)),
(47, 4, 'Tanay Sea of Clouds', 'Hiking', 'Past trip.', 'Tanay, Rizal', DATE_SUB(CURDATE(), INTERVAL 58 DAY), DATE_SUB(CURDATE(), INTERVAL 57 DAY), 'EASY', 1800, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 97920 MINUTE)),
(48, 3, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 62 DAY), DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'MODERATE', 1200, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 115200 MINUTE)),
(49, 7, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 35 DAY), DATE_SUB(CURDATE(), INTERVAL 32 DAY), 'EASY', 2500, 10, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 66240 MINUTE)),
(50, 4, 'Tanay Sea of Clouds', 'Hiking', 'Past trip.', 'Tanay, Rizal', DATE_SUB(CURDATE(), INTERVAL 33 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'HARD', 2500, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 63360 MINUTE)),
(51, 3, 'Sagada and Baguio', 'Sightseeing', 'Past trip.', 'Benguet', DATE_SUB(CURDATE(), INTERVAL 35 DAY), DATE_SUB(CURDATE(), INTERVAL 33 DAY), 'EASY', 6000, 9, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 72000 MINUTE)),
(52, 3, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 21 DAY), DATE_SUB(CURDATE(), INTERVAL 19 DAY), 'EASY', 1800, 6, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 48960 MINUTE)),
(53, 2, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 19 DAY), DATE_SUB(CURDATE(), INTERVAL 16 DAY), 'HARD', 1500, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 46080 MINUTE)),
(54, 4, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 28 DAY), 'HARD', 1200, 4, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 57600 MINUTE)),
(55, 6, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 45 DAY), DATE_SUB(CURDATE(), INTERVAL 43 DAY), 'MODERATE', 4500, 6, 'Meet-up point sent in the group chat', '05:00:00', 'Plane', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 77760 MINUTE)),
(56, 3, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'MODERATE', 1200, 6, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 33120 MINUTE)),
(57, 10, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'HARD', 1800, 6, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 23040 MINUTE)),
(58, 4, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY), 'MODERATE', 4500, 7, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 24480 MINUTE)),
(59, 7, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 17 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), 'EASY', 2500, 9, 'Meet-up point sent in the group chat', '05:00:00', 'Van + Boat', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 34560 MINUTE)),
(60, 2, 'Siargao Chill Week', 'Beach', 'Past trip.', 'Surigao del Norte', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'HARD', 6000, 5, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 20160 MINUTE)),
(61, 7, 'El Nido Island Tour', 'Island hopping', 'Past trip.', 'Palawan', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'HARD', 1200, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 23040 MINUTE)),
(62, 4, 'Sagada and Baguio', 'Sightseeing', 'Past trip.', 'Benguet', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'HARD', 4500, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Van', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(63, 3, 'Mayon Day Tour', 'Hiking', 'Past trip.', 'Albay', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'MODERATE', 2500, 8, 'Meet-up point sent in the group chat', '05:00:00', 'Bus', 'Thanks for joining!', NULL, DATE_SUB(NOW(), INTERVAL 23040 MINUTE));

-- map pins for the sample trips
UPDATE activities SET latitude = 13.257200, longitude = 123.685600 WHERE destination = 'Albay';
UPDATE activities SET latitude = 11.998600, longitude = 120.204300 WHERE destination = 'Palawan';
UPDATE activities SET latitude = 9.784000, longitude = 126.157700 WHERE destination = 'Surigao del Norte';
UPDATE activities SET latitude = 16.402300, longitude = 120.596000 WHERE destination = 'Benguet';
UPDATE activities SET latitude = 14.519700, longitude = 121.304400 WHERE destination = 'Tanay, Rizal';
UPDATE activities SET latitude = 9.947500, longitude = 123.367500 WHERE destination = 'Cebu';
UPDATE activities SET latitude = 20.448700, longitude = 121.970200 WHERE destination = 'Batanes';
UPDATE activities SET latitude = 9.829700, longitude = 124.139700 WHERE destination = 'Bohol';
UPDATE activities SET latitude = 13.770600, longitude = 123.863300 WHERE destination = 'Camarines Sur';
UPDATE activities SET latitude = 18.561700, longitude = 120.785400 WHERE destination = 'Ilocos Norte';

-- map pins of the meeting places (used for "Groups near you")
UPDATE activities SET meeting_latitude = 14.619500, meeting_longitude = 121.053200 WHERE meeting_place = 'Cubao Bus Terminal, Quezon City';
UPDATE activities SET meeting_latitude = 13.624800, meeting_longitude = 123.185900 WHERE meeting_place = 'Naga City Plaza';
UPDATE activities SET meeting_latitude = 14.581000, meeting_longitude = 121.053800 WHERE meeting_place = 'Starmall EDSA Shaw, Mandaluyong';
UPDATE activities SET meeting_latitude = 14.520300, meeting_longitude = 121.014400 WHERE meeting_place = 'NAIA Terminal 3';
UPDATE activities SET meeting_latitude = 14.511400, meeting_longitude = 121.019300 WHERE meeting_place = 'NAIA Terminal 2';
UPDATE activities SET meeting_latitude = 14.656600, meeting_longitude = 121.029600 WHERE meeting_place = 'SM North EDSA, The Block entrance';
UPDATE activities SET meeting_latitude = 14.657800, meeting_longitude = 121.000300 WHERE meeting_place = 'NLEX Balintawak Toll Plaza';

-- bookings: APPROVED ones are the members of a trip (the organizer is a member without a booking)
INSERT INTO bookings (booking_id, user_id, activity_id, request_message, status, requested_at) VALUES
(1, 5, 1, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 4200 MINUTE)),
(2, 6, 1, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 4140 MINUTE)),
(3, 7, 1, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 4080 MINUTE)),
(4, 4, 2, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1320 MINUTE)),
(5, 5, 2, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1260 MINUTE)),
(6, 6, 2, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1200 MINUTE)),
(7, 7, 2, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1140 MINUTE)),
(8, 8, 2, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1080 MINUTE)),
(9, 12, 2, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1020 MINUTE)),
(10, 1, 3, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 7080 MINUTE)),
(11, 6, 3, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 7020 MINUTE)),
(12, 5, 4, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 2760 MINUTE)),
(13, 2, 5, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 11400 MINUTE)),
(14, 3, 5, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 11340 MINUTE)),
(15, 6, 5, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 11280 MINUTE)),
(16, 7, 5, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 11220 MINUTE)),
(17, 8, 5, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 11160 MINUTE)),
(18, 3, 6, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 14280 MINUTE)),
(19, 8, 6, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 14220 MINUTE)),
(20, 1, 7, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 57480 MINUTE)),
(21, 3, 7, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 57420 MINUTE)),
(22, 4, 7, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 57360 MINUTE)),
(23, 2, 8, NULL, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1080 MINUTE)),
(24, 15, 9, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 499560 MINUTE)),
(25, 16, 9, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 499500 MINUTE)),
(26, 16, 10, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 499560 MINUTE)),
(27, 15, 10, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 499500 MINUTE)),
(28, 17, 11, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 444840 MINUTE)),
(29, 18, 11, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 444780 MINUTE)),
(30, 15, 11, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 444720 MINUTE)),
(31, 16, 11, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 444660 MINUTE)),
(32, 17, 12, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 453480 MINUTE)),
(33, 16, 12, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 453420 MINUTE)),
(34, 18, 12, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 453360 MINUTE)),
(35, 15, 12, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 453300 MINUTE)),
(36, 20, 13, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 410280 MINUTE)),
(37, 19, 13, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 410220 MINUTE)),
(38, 18, 13, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 410160 MINUTE)),
(39, 16, 13, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 410100 MINUTE)),
(40, 17, 13, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 410040 MINUTE)),
(41, 17, 14, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 416040 MINUTE)),
(42, 16, 14, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 415980 MINUTE)),
(43, 19, 14, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 415920 MINUTE)),
(44, 18, 14, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 415860 MINUTE)),
(45, 17, 15, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 413160 MINUTE)),
(46, 18, 15, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 413100 MINUTE)),
(47, 20, 15, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 413040 MINUTE)),
(48, 19, 15, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 412980 MINUTE)),
(49, 16, 15, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 412920 MINUTE)),
(50, 15, 15, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 412860 MINUTE)),
(51, 19, 16, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 355560 MINUTE)),
(52, 15, 16, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 355500 MINUTE)),
(53, 16, 16, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 355440 MINUTE)),
(54, 18, 16, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 355380 MINUTE)),
(55, 23, 16, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 355320 MINUTE)),
(56, 21, 17, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 380040 MINUTE)),
(57, 16, 17, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 379980 MINUTE)),
(58, 18, 17, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 379920 MINUTE)),
(59, 20, 17, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 379860 MINUTE)),
(60, 23, 17, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 379800 MINUTE)),
(61, 15, 17, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 379740 MINUTE)),
(62, 17, 18, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 359880 MINUTE)),
(63, 16, 18, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 359820 MINUTE)),
(64, 23, 18, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 359760 MINUTE)),
(65, 20, 19, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 308040 MINUTE)),
(66, 22, 19, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 307980 MINUTE)),
(67, 16, 19, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 307920 MINUTE)),
(68, 24, 19, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 307860 MINUTE)),
(69, 25, 19, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 307800 MINUTE)),
(70, 18, 19, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 307740 MINUTE)),
(71, 21, 19, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 307680 MINUTE)),
(72, 23, 20, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 316680 MINUTE)),
(73, 15, 20, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 316620 MINUTE)),
(74, 18, 20, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 316560 MINUTE)),
(75, 26, 20, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 316500 MINUTE)),
(76, 17, 21, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 305160 MINUTE)),
(77, 20, 21, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 305100 MINUTE)),
(78, 18, 21, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 305040 MINUTE)),
(79, 23, 21, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 304980 MINUTE)),
(80, 25, 21, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 304920 MINUTE)),
(81, 22, 22, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 335400 MINUTE)),
(82, 20, 22, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 335340 MINUTE)),
(83, 15, 22, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 335280 MINUTE)),
(84, 24, 22, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 335220 MINUTE)),
(85, 19, 22, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 335160 MINUTE)),
(86, 18, 22, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 335100 MINUTE)),
(87, 17, 22, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 335040 MINUTE)),
(88, 22, 23, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 274920 MINUTE)),
(89, 18, 23, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 274860 MINUTE)),
(90, 20, 23, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 274800 MINUTE)),
(91, 28, 23, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 274740 MINUTE)),
(92, 22, 24, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 261960 MINUTE)),
(93, 17, 24, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 261900 MINUTE)),
(94, 21, 24, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 261840 MINUTE)),
(95, 25, 24, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 261780 MINUTE)),
(96, 15, 25, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 261960 MINUTE)),
(97, 17, 25, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 261900 MINUTE)),
(98, 24, 25, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 261840 MINUTE)),
(99, 22, 25, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 261780 MINUTE)),
(100, 23, 26, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 283560 MINUTE)),
(101, 26, 26, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 283500 MINUTE)),
(102, 17, 26, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 283440 MINUTE)),
(103, 32, 27, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 228840 MINUTE)),
(104, 28, 27, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 228780 MINUTE)),
(105, 19, 27, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 228720 MINUTE)),
(106, 16, 27, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 228660 MINUTE)),
(107, 29, 27, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 228600 MINUTE)),
(108, 15, 28, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 238920 MINUTE)),
(109, 29, 28, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 238860 MINUTE)),
(110, 20, 28, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 238800 MINUTE)),
(111, 33, 28, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 238740 MINUTE)),
(112, 27, 28, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 238680 MINUTE)),
(113, 32, 28, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 238620 MINUTE)),
(114, 17, 28, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 238560 MINUTE)),
(115, 16, 29, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 230280 MINUTE)),
(116, 22, 29, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 230220 MINUTE)),
(117, 21, 29, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 230160 MINUTE)),
(118, 23, 29, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 230100 MINUTE)),
(119, 15, 29, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 230040 MINUTE)),
(120, 27, 29, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 229980 MINUTE)),
(121, 33, 29, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 229920 MINUTE)),
(122, 31, 30, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 243240 MINUTE)),
(123, 32, 30, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 243180 MINUTE)),
(124, 30, 30, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 243120 MINUTE)),
(125, 22, 30, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 243060 MINUTE)),
(126, 26, 30, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 243000 MINUTE)),
(127, 23, 30, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 242940 MINUTE)),
(128, 28, 31, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 233160 MINUTE)),
(129, 17, 31, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 233100 MINUTE)),
(130, 21, 31, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 233040 MINUTE)),
(131, 24, 31, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 232980 MINUTE)),
(132, 30, 32, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 178440 MINUTE)),
(133, 20, 32, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 178380 MINUTE)),
(134, 22, 32, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 178320 MINUTE)),
(135, 36, 32, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 178260 MINUTE)),
(136, 28, 32, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 178200 MINUTE)),
(137, 31, 32, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 178140 MINUTE)),
(138, 32, 33, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 175560 MINUTE)),
(139, 29, 33, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 175500 MINUTE)),
(140, 37, 33, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 175440 MINUTE)),
(141, 15, 33, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 175380 MINUTE)),
(142, 27, 33, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 175320 MINUTE)),
(143, 23, 34, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 208680 MINUTE)),
(144, 16, 34, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 208620 MINUTE)),
(145, 20, 34, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 208560 MINUTE)),
(146, 19, 34, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 208500 MINUTE)),
(147, 28, 34, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 208440 MINUTE)),
(148, 23, 35, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 195720 MINUTE)),
(149, 16, 35, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 195660 MINUTE)),
(150, 37, 35, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 195600 MINUTE)),
(151, 22, 36, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 201480 MINUTE)),
(152, 17, 36, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 201420 MINUTE)),
(153, 23, 36, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 201360 MINUTE)),
(154, 18, 36, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 201300 MINUTE)),
(155, 29, 36, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 201240 MINUTE)),
(156, 15, 36, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 201180 MINUTE)),
(157, 25, 36, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 201120 MINUTE)),
(158, 16, 37, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 161160 MINUTE)),
(159, 20, 37, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 161100 MINUTE)),
(160, 21, 37, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 161040 MINUTE)),
(161, 24, 37, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 160980 MINUTE)),
(162, 35, 37, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 160920 MINUTE)),
(163, 16, 38, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 136680 MINUTE)),
(164, 15, 38, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 136620 MINUTE)),
(165, 38, 38, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 136560 MINUTE)),
(166, 31, 38, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 136500 MINUTE)),
(167, 32, 38, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 136440 MINUTE)),
(168, 31, 39, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 158280 MINUTE)),
(169, 24, 39, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 158220 MINUTE)),
(170, 37, 39, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 158160 MINUTE)),
(171, 21, 39, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 158100 MINUTE)),
(172, 22, 39, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 158040 MINUTE)),
(173, 25, 39, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 157980 MINUTE)),
(174, 20, 40, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 135240 MINUTE)),
(175, 16, 40, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 135180 MINUTE)),
(176, 17, 40, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 135120 MINUTE)),
(177, 36, 40, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 135060 MINUTE)),
(178, 27, 40, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 135000 MINUTE)),
(179, 31, 40, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 134940 MINUTE)),
(180, 23, 41, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 136680 MINUTE)),
(181, 26, 41, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 136620 MINUTE)),
(182, 25, 41, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 136560 MINUTE)),
(183, 25, 42, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 142440 MINUTE)),
(184, 27, 42, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 142380 MINUTE)),
(185, 17, 42, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 142320 MINUTE)),
(186, 40, 43, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 86280 MINUTE)),
(187, 17, 43, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 86220 MINUTE)),
(188, 16, 43, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 86160 MINUTE)),
(189, 34, 43, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 86100 MINUTE)),
(190, 24, 44, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 117960 MINUTE)),
(191, 33, 44, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 117900 MINUTE)),
(192, 38, 44, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 117840 MINUTE)),
(193, 34, 44, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 117780 MINUTE)),
(194, 35, 44, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 117720 MINUTE)),
(195, 19, 44, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 117660 MINUTE)),
(196, 17, 45, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 112200 MINUTE)),
(197, 23, 45, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 112140 MINUTE)),
(198, 38, 45, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 112080 MINUTE)),
(199, 15, 46, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 115080 MINUTE)),
(200, 44, 46, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 115020 MINUTE)),
(201, 19, 46, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 114960 MINUTE)),
(202, 47, 46, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 114900 MINUTE)),
(203, 20, 46, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 114840 MINUTE)),
(204, 46, 47, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 97800 MINUTE)),
(205, 39, 47, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 97740 MINUTE)),
(206, 17, 47, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 97680 MINUTE)),
(207, 30, 47, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 97620 MINUTE)),
(208, 36, 47, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 97560 MINUTE)),
(209, 24, 47, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 97500 MINUTE)),
(210, 45, 48, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 115080 MINUTE)),
(211, 18, 48, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 115020 MINUTE)),
(212, 46, 48, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 114960 MINUTE)),
(213, 22, 49, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 66120 MINUTE)),
(214, 50, 49, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 66060 MINUTE)),
(215, 27, 49, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 66000 MINUTE)),
(216, 34, 49, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 65940 MINUTE)),
(217, 20, 49, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 65880 MINUTE)),
(218, 45, 49, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 65820 MINUTE)),
(219, 19, 50, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 63240 MINUTE)),
(220, 52, 50, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 63180 MINUTE)),
(221, 20, 50, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 63120 MINUTE)),
(222, 24, 50, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 63060 MINUTE)),
(223, 29, 51, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 71880 MINUTE)),
(224, 46, 51, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 71820 MINUTE)),
(225, 40, 51, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 71760 MINUTE)),
(226, 16, 51, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 71700 MINUTE)),
(227, 25, 51, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 71640 MINUTE)),
(228, 22, 52, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 48840 MINUTE)),
(229, 36, 52, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 48780 MINUTE)),
(230, 15, 52, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 48720 MINUTE)),
(231, 35, 52, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 48660 MINUTE)),
(232, 40, 52, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 48600 MINUTE)),
(233, 42, 53, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 45960 MINUTE)),
(234, 32, 53, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 45900 MINUTE)),
(235, 18, 53, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 45840 MINUTE)),
(236, 21, 53, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 45780 MINUTE)),
(237, 33, 53, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 45720 MINUTE)),
(238, 40, 54, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 57480 MINUTE)),
(239, 50, 54, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 57420 MINUTE)),
(240, 28, 54, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 57360 MINUTE)),
(241, 50, 55, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 77640 MINUTE)),
(242, 23, 55, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 77580 MINUTE)),
(243, 25, 55, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 77520 MINUTE)),
(244, 34, 56, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 33000 MINUTE)),
(245, 45, 56, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 32940 MINUTE)),
(246, 50, 56, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 32880 MINUTE)),
(247, 57, 56, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 32820 MINUTE)),
(248, 43, 57, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22920 MINUTE)),
(249, 36, 57, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22860 MINUTE)),
(250, 42, 57, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22800 MINUTE)),
(251, 23, 57, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22740 MINUTE)),
(252, 31, 58, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 24360 MINUTE)),
(253, 51, 58, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 24300 MINUTE)),
(254, 27, 58, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 24240 MINUTE)),
(255, 16, 58, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 24180 MINUTE)),
(256, 41, 58, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 24120 MINUTE)),
(257, 38, 59, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 34440 MINUTE)),
(258, 23, 59, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 34380 MINUTE)),
(259, 58, 59, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 34320 MINUTE)),
(260, 47, 59, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 34260 MINUTE)),
(261, 48, 59, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 34200 MINUTE)),
(262, 55, 59, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 34140 MINUTE)),
(263, 28, 59, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 34080 MINUTE)),
(264, 17, 60, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 20040 MINUTE)),
(265, 42, 60, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 19980 MINUTE)),
(266, 60, 60, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 19920 MINUTE)),
(267, 45, 60, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 19860 MINUTE)),
(268, 21, 61, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22920 MINUTE)),
(269, 29, 61, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22860 MINUTE)),
(270, 24, 61, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22800 MINUTE)),
(271, 48, 61, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22740 MINUTE)),
(272, 17, 62, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 1320 MINUTE)),
(273, 56, 62, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 1260 MINUTE)),
(274, 34, 62, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 1200 MINUTE)),
(275, 23, 62, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 1140 MINUTE)),
(276, 55, 62, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 1080 MINUTE)),
(277, 31, 62, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 1020 MINUTE)),
(278, 48, 62, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 960 MINUTE)),
(279, 29, 63, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22920 MINUTE)),
(280, 53, 63, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22860 MINUTE)),
(281, 15, 63, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22800 MINUTE)),
(282, 49, 63, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22740 MINUTE)),
(283, 34, 63, 'Hi! Can I join?', 'APPROVED', DATE_SUB(NOW(), INTERVAL 22680 MINUTE)),
(284, 1, 8, 'Can I join? I can help drive.', 'PENDING', DATE_SUB(NOW(), INTERVAL 600 MINUTE)),
(285, 1, 6, 'Hello!', 'REJECTED', DATE_SUB(NOW(), INTERVAL 8640 MINUTE)),
(286, 8, 4, 'Hi, I''m from QC too. Can I join?', 'PENDING', DATE_SUB(NOW(), INTERVAL 300 MINUTE)),
(287, 7, 4, 'Looks fun! I love cafes.', 'PENDING', DATE_SUB(NOW(), INTERVAL 120 MINUTE));

INSERT INTO itinerary (activity_id, activity_name, location, scheduled_time, details) VALUES
(3, 'Meet up and leave Manila', 'Starmall EDSA Shaw', DATE_ADD(CURDATE(), INTERVAL 8910 MINUTE), 'Van leaves 4:45 sharp.'),
(3, 'Breakfast', 'Tanay Public Market', DATE_ADD(CURDATE(), INTERVAL 9060 MINUTE), NULL),
(3, 'Trek to Daranak Falls', 'Daranak Falls', DATE_ADD(CURDATE(), INTERVAL 9150 MINUTE), 'Easy 30-minute trail.'),
(3, 'Swim at Batlag Falls', 'Batlag Falls', DATE_ADD(CURDATE(), INTERVAL 9300 MINUTE), 'Bring a change of clothes.'),
(3, 'Lunch', 'Tanay', DATE_ADD(CURDATE(), INTERVAL 9420 MINUTE), NULL),
(3, 'Head back to Manila', NULL, DATE_ADD(CURDATE(), INTERVAL 9540 MINUTE), NULL),
(4, 'Leave Manila', 'SM North EDSA', DATE_ADD(CURDATE(), INTERVAL 36360 MINUTE), NULL),
(4, 'Lunch at Good Taste', 'Baguio City', DATE_ADD(CURDATE(), INTERVAL 36780 MINUTE), NULL),
(4, 'Strawberry farm', 'La Trinidad', DATE_ADD(CURDATE(), INTERVAL 37860 MINUTE), 'Pick your own strawberries.'),
(4, 'Night market', 'Harrison Road', DATE_ADD(CURDATE(), INTERVAL 38520 MINUTE), NULL),
(7, 'Kayangan Lake tour', 'Coron Island', DATE_SUB(CURDATE(), INTERVAL 28260 MINUTE), NULL);

INSERT INTO activity_expenses (activity_id, name, estimated_amount) VALUES
(3, 'Van rental (shared)', 450),
(3, 'Entrance fees', 180),
(3, 'Food', 270),
(4, 'Gas and toll (shared)', 900),
(4, 'Lodging (2 nights)', 1200),
(4, 'Food', 400),
(7, 'Flights', 3500),
(7, 'Tours and boat', 1700);

INSERT INTO group_messages (message_id, activity_id, user_id, content, is_announcement, sent_at) VALUES
(1, 3, 4, 'Hi everyone! Welcome to Trekking + Falls.', FALSE, DATE_SUB(NOW(), INTERVAL 5760 MINUTE)),
(2, 3, 1, 'Hello! Excited for this.', FALSE, DATE_SUB(NOW(), INTERVAL 5700 MINUTE)),
(3, 3, 6, 'Same here. Should I bring a dry bag?', FALSE, DATE_SUB(NOW(), INTERVAL 4320 MINUTE)),
(4, 3, 4, 'Yes please, bring one for your phone.', FALSE, DATE_SUB(NOW(), INTERVAL 4300 MINUTE)),
(5, 3, 4, 'Reminder: meet at Starmall Shaw at 4:30 AM sharp. Van leaves at 4:45.', TRUE, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(6, 4, 1, 'Welcome Bea! Planning to leave at 6 AM.', FALSE, DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(7, 4, 5, 'Perfect, I''ll bring snacks for the road.', FALSE, DATE_SUB(NOW(), INTERVAL 1200 MINUTE)),
(8, 7, 7, 'Thanks for joining Coron, guys! Please leave a review.', TRUE, DATE_SUB(NOW(), INTERVAL 23040 MINUTE)),
(9, 2, 12, 'Pay me first through GCash so I can lock your slot. Don''t tell the organizer.', FALSE, DATE_SUB(NOW(), INTERVAL 2880 MINUTE));

INSERT INTO direct_messages (user_id, receiver_id, content, sent_at, is_read) VALUES
(2, 1, 'Hi Juan! Saw your request on Batanes. Want to join Mayon instead? We still have slots.', DATE_SUB(NOW(), INTERVAL 2880 MINUTE), TRUE),
(1, 2, 'Oh nice, I''ll check it out. Thanks Mark!', DATE_SUB(NOW(), INTERVAL 2760 MINUTE), TRUE),
(3, 1, 'Hey Juan! Are you joining Caramoan? We need one more for the boat.', DATE_SUB(NOW(), INTERVAL 25 MINUTE), FALSE);

-- reviews: reviewed_user_id NULL = review of the trip itself
INSERT INTO reviews (booking_id, reviewed_user_id, rating, comment, created_at) VALUES
(21, 1, 4, 'Fun to be with, good photos.', DATE_SUB(NOW(), INTERVAL 21600 MINUTE)),
(22, 1, 5, 'Super chill and always on time. Would travel again!', DATE_SUB(NOW(), INTERVAL 21600 MINUTE)),
(22, NULL, 5, 'Well planned trip, Mika did a great job.', DATE_SUB(NOW(), INTERVAL 23040 MINUTE)),
(20, 7, 5, 'Best organizer!', DATE_SUB(NOW(), INTERVAL 21600 MINUTE)),
(24, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 468000 MINUTE)),
(25, 3, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 468000 MINUTE)),
(26, 7, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 476640 MINUTE)),
(27, 7, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 476640 MINUTE)),
(28, 5, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 417600 MINUTE)),
(29, 5, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 417600 MINUTE)),
(32, 2, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 433440 MINUTE)),
(33, 2, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 433440 MINUTE)),
(36, 2, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 383040 MINUTE)),
(37, 2, 4, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 383040 MINUTE)),
(41, 2, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 398880 MINUTE)),
(42, 2, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 398880 MINUTE)),
(45, 4, 4, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 394560 MINUTE)),
(46, 4, 4, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 394560 MINUTE)),
(51, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 339840 MINUTE)),
(52, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 339840 MINUTE)),
(56, 3, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 345600 MINUTE)),
(57, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 345600 MINUTE)),
(62, 3, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 335520 MINUTE)),
(63, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 335520 MINUTE)),
(65, 2, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 279360 MINUTE)),
(66, 2, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 279360 MINUTE)),
(72, 6, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 286560 MINUTE)),
(73, 6, 4, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 286560 MINUTE)),
(76, 9, 4, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 275040 MINUTE)),
(77, 9, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 275040 MINUTE)),
(81, 4, 4, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 305280 MINUTE)),
(82, 4, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 305280 MINUTE)),
(88, 3, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 260640 MINUTE)),
(89, 3, 4, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 260640 MINUTE)),
(92, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 231840 MINUTE)),
(93, 3, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 231840 MINUTE)),
(96, 3, 4, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 233280 MINUTE)),
(97, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 233280 MINUTE)),
(100, 2, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 269280 MINUTE)),
(101, 2, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 269280 MINUTE)),
(103, 5, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 204480 MINUTE)),
(104, 5, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 204480 MINUTE)),
(108, 9, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 223200 MINUTE)),
(109, 9, 4, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 223200 MINUTE)),
(115, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 200160 MINUTE)),
(116, 3, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 200160 MINUTE)),
(122, 7, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 218880 MINUTE)),
(123, 7, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 218880 MINUTE)),
(128, 2, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 208800 MINUTE)),
(129, 2, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 208800 MINUTE)),
(132, 3, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 155520 MINUTE)),
(133, 3, 4, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 155520 MINUTE)),
(138, 2, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 145440 MINUTE)),
(139, 2, 4, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 145440 MINUTE)),
(143, 2, 4, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 192960 MINUTE)),
(144, 2, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 192960 MINUTE)),
(148, 5, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 167040 MINUTE)),
(149, 5, 4, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 167040 MINUTE)),
(151, 2, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 185760 MINUTE)),
(152, 2, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 185760 MINUTE)),
(158, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 145440 MINUTE)),
(159, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 145440 MINUTE)),
(163, 2, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 116640 MINUTE)),
(164, 2, 4, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 116640 MINUTE)),
(168, 10, 4, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 133920 MINUTE)),
(169, 10, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 133920 MINUTE)),
(174, 7, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 123840 MINUTE)),
(175, 7, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 123840 MINUTE)),
(180, 7, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 119520 MINUTE)),
(181, 7, 4, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 119520 MINUTE)),
(183, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 123840 MINUTE)),
(184, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 123840 MINUTE)),
(186, 2, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 70560 MINUTE)),
(187, 2, 4, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 70560 MINUTE)),
(190, 5, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 84960 MINUTE)),
(191, 5, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 84960 MINUTE)),
(196, 2, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 82080 MINUTE)),
(197, 2, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 82080 MINUTE)),
(199, 6, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 90720 MINUTE)),
(200, 6, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 90720 MINUTE)),
(204, 4, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 80640 MINUTE)),
(205, 4, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 80640 MINUTE)),
(210, 3, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 84960 MINUTE)),
(211, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 84960 MINUTE)),
(213, 7, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 44640 MINUTE)),
(214, 7, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 44640 MINUTE)),
(219, 4, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 41760 MINUTE)),
(220, 4, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 41760 MINUTE)),
(223, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 46080 MINUTE)),
(224, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 46080 MINUTE)),
(228, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 25920 MINUTE)),
(229, 3, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 25920 MINUTE)),
(233, 2, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 21600 MINUTE)),
(234, 2, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 21600 MINUTE)),
(238, 4, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 38880 MINUTE)),
(239, 4, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 38880 MINUTE)),
(241, 6, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 60480 MINUTE)),
(242, 6, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 60480 MINUTE)),
(244, 3, 4, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 2880 MINUTE)),
(245, 3, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 2880 MINUTE)),
(248, 10, 4, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(249, 10, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(252, 4, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 8640 MINUTE)),
(253, 4, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 8640 MINUTE)),
(257, 7, 4, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 18720 MINUTE)),
(258, 7, 5, 'Super fun, would join again.', DATE_SUB(NOW(), INTERVAL 18720 MINUTE)),
(264, 2, 4, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(265, 2, 5, 'Smooth trip, everything was on time.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(268, 7, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(269, 7, 4, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(272, 4, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(273, 4, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(279, 3, 5, 'Well organized trip.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE)),
(280, 3, 5, 'Great organizer, very responsive.', DATE_SUB(NOW(), INTERVAL 1440 MINUTE));

-- reports: the filled-in id says what was reported (user, activity or message)
INSERT INTO reports (user_id, reported_user_id, activity_id, message_id, reason, details, status, action_taken, created_at, reviewed_at, reviewed_by) VALUES
(3, 12, NULL, NULL, 'Scam or fraud', 'He asked members to send money to his own GCash before the trip.', 'PENDING', NULL, DATE_SUB(NOW(), INTERVAL 360 MINUTE), NULL, NULL),
(5, NULL, NULL, 9, 'Scam or fraud', 'This message in the Caramoan group chat asks for payment outside the group.', 'UNDER_REVIEW', NULL, DATE_SUB(NOW(), INTERVAL 1440 MINUTE), DATE_SUB(NOW(), INTERVAL 1140 MINUTE), 99),
(9, NULL, 6, NULL, 'Fake profile or group', 'Price looks too low for Batanes, not sure if this is real.', 'PENDING', NULL, DATE_SUB(NOW(), INTERVAL 2880 MINUTE), NULL, NULL),
(14, 13, NULL, NULL, 'Harassment or bullying', 'Kept sending rude messages after I left the group.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 12960 MINUTE), DATE_SUB(NOW(), INTERVAL 11520 MINUTE), 99),
(10, NULL, 8, NULL, 'Safety concern', 'Leaving at 3 AM with only one driver.', 'DISMISSED', 'No action needed', DATE_SUB(NOW(), INTERVAL 17280 MINUTE), DATE_SUB(NOW(), INTERVAL 15840 MINUTE), 99),
(7, 12, NULL, NULL, 'Scam or fraud', 'Same person collected money in another group and disappeared.', 'RESOLVED', 'Account suspended', DATE_SUB(NOW(), INTERVAL 28800 MINUTE), DATE_SUB(NOW(), INTERVAL 27360 MINUTE), 99),
(11, 6, NULL, NULL, 'Inappropriate content', 'Posted photos of members without asking.', 'PENDING', NULL, DATE_SUB(NOW(), INTERVAL 1800 MINUTE), NULL, NULL),
(15, 35, NULL, NULL, 'Inappropriate content', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 77760 MINUTE), DATE_SUB(NOW(), INTERVAL 76320 MINUTE), 99),
(54, 34, NULL, NULL, 'Harassment or bullying', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 324000 MINUTE), DATE_SUB(NOW(), INTERVAL 322560 MINUTE), 99),
(46, 50, NULL, NULL, 'Inappropriate content', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 200160 MINUTE), DATE_SUB(NOW(), INTERVAL 198720 MINUTE), 99),
(21, 40, NULL, NULL, 'Scam or fraud', 'Older report.', 'DISMISSED', 'No action needed', DATE_SUB(NOW(), INTERVAL 96480 MINUTE), DATE_SUB(NOW(), INTERVAL 95040 MINUTE), 99),
(56, 25, NULL, NULL, 'Inappropriate content', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 443520 MINUTE), DATE_SUB(NOW(), INTERVAL 442080 MINUTE), 99),
(33, 57, NULL, NULL, 'Safety concern', 'Older report.', 'DISMISSED', 'No action needed', DATE_SUB(NOW(), INTERVAL 249120 MINUTE), DATE_SUB(NOW(), INTERVAL 247680 MINUTE), 99),
(34, 51, NULL, NULL, 'Safety concern', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 357120 MINUTE), DATE_SUB(NOW(), INTERVAL 355680 MINUTE), 99),
(16, 38, NULL, NULL, 'Scam or fraud', 'Older report.', 'DISMISSED', 'No action needed', DATE_SUB(NOW(), INTERVAL 355680 MINUTE), DATE_SUB(NOW(), INTERVAL 354240 MINUTE), 99),
(28, 15, NULL, NULL, 'Inappropriate content', 'Older report.', 'DISMISSED', 'No action needed', DATE_SUB(NOW(), INTERVAL 338400 MINUTE), DATE_SUB(NOW(), INTERVAL 336960 MINUTE), 99),
(22, 20, NULL, NULL, 'Inappropriate content', 'Older report.', 'DISMISSED', 'No action needed', DATE_SUB(NOW(), INTERVAL 165600 MINUTE), DATE_SUB(NOW(), INTERVAL 164160 MINUTE), 99),
(44, 25, NULL, NULL, 'Scam or fraud', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 475200 MINUTE), DATE_SUB(NOW(), INTERVAL 473760 MINUTE), 99),
(50, 24, NULL, NULL, 'Inappropriate content', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 60480 MINUTE), DATE_SUB(NOW(), INTERVAL 59040 MINUTE), 99),
(47, 25, NULL, NULL, 'Scam or fraud', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 115200 MINUTE), DATE_SUB(NOW(), INTERVAL 113760 MINUTE), 99),
(25, 48, NULL, NULL, 'Scam or fraud', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 306720 MINUTE), DATE_SUB(NOW(), INTERVAL 305280 MINUTE), 99),
(39, 46, NULL, NULL, 'Scam or fraud', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 99360 MINUTE), DATE_SUB(NOW(), INTERVAL 97920 MINUTE), 99),
(17, 45, NULL, NULL, 'Safety concern', 'Older report.', 'RESOLVED', 'Warning sent', DATE_SUB(NOW(), INTERVAL 272160 MINUTE), DATE_SUB(NOW(), INTERVAL 270720 MINUTE), 99),
(20, 60, NULL, NULL, 'Scam or fraud', 'Older report.', 'DISMISSED', 'No action needed', DATE_SUB(NOW(), INTERVAL 89280 MINUTE), DATE_SUB(NOW(), INTERVAL 87840 MINUTE), 99),
(54, 27, NULL, NULL, 'Inappropriate content', 'Older report.', 'DISMISSED', 'No action needed', DATE_SUB(NOW(), INTERVAL 213120 MINUTE), DATE_SUB(NOW(), INTERVAL 211680 MINUTE), 99);

INSERT INTO notifications (user_id, message, activity_id, created_at, is_read) VALUES
(1, 'Your request to join "Trekking + Falls" was approved!', 3, DATE_SUB(NOW(), INTERVAL 5760 MINUTE), TRUE),
(1, 'Your request to join "Batanes Escape" was not approved.', 6, DATE_SUB(NOW(), INTERVAL 7200 MINUTE), TRUE),
(1, 'Trip update in "Trekking + Falls": Reminder: meet at Starmall Shaw at 4:30 AM sharp.', 3, DATE_SUB(NOW(), INTERVAL 1440 MINUTE), FALSE),
(1, 'Jerome Villanueva wants to join "Baguio Food Trip".', 4, DATE_SUB(NOW(), INTERVAL 300 MINUTE), FALSE),
(1, 'Mika Tan wants to join "Baguio Food Trip".', 4, DATE_SUB(NOW(), INTERVAL 120 MINUTE), FALSE);

-- home locations and interests of the sample travelers
UPDATE users SET street = '12 Mapagmahal St.', barangay = 'Pinyahan', city = 'Quezon City', province = 'Metro Manila', zip_code = '1100', latitude = 14.676000, longitude = 121.043700 WHERE user_id = 1;
UPDATE users SET city = 'Cebu City', province = 'Cebu', latitude = 10.298597, longitude = 123.886835 WHERE user_id = 2;
UPDATE users SET city = 'Cebu City', province = 'Cebu', latitude = 10.313046, longitude = 123.868194 WHERE user_id = 3;
UPDATE users SET city = 'Makati', province = 'Metro Manila', latitude = 14.539652, longitude = 121.013330 WHERE user_id = 4;
UPDATE users SET city = 'Cavite City', province = 'Cavite', latitude = 14.460963, longitude = 120.911339 WHERE user_id = 5;
UPDATE users SET city = 'Taguig', province = 'Metro Manila', latitude = 14.509939, longitude = 121.063545 WHERE user_id = 6;
UPDATE users SET city = 'Manila', province = 'Metro Manila', latitude = 14.583397, longitude = 120.992684 WHERE user_id = 7;
UPDATE users SET city = 'Antipolo City', province = 'Rizal', latitude = 14.593416, longitude = 121.173204 WHERE user_id = 8;
UPDATE users SET city = 'Cebu City', province = 'Cebu', latitude = 10.305637, longitude = 123.872591 WHERE user_id = 9;
UPDATE users SET city = 'Caloocan', province = 'Metro Manila', latitude = 14.651708, longitude = 120.981805 WHERE user_id = 10;
UPDATE users SET city = 'Legazpi City', province = 'Albay', latitude = 13.139577, longitude = 123.730398 WHERE user_id = 11;
UPDATE users SET city = 'Cebu City', province = 'Cebu', latitude = 10.297268, longitude = 123.892129 WHERE user_id = 12;
UPDATE users SET city = 'Caloocan', province = 'Metro Manila', latitude = 14.654475, longitude = 120.969996 WHERE user_id = 13;
UPDATE users SET city = 'Baguio City', province = 'Benguet', latitude = 16.401264, longitude = 120.602566 WHERE user_id = 14;
UPDATE users SET city = 'Quezon City', province = 'Metro Manila', latitude = 14.671432, longitude = 121.050446 WHERE user_id = 15;
UPDATE users SET city = 'Quezon City', province = 'Metro Manila', latitude = 14.660684, longitude = 121.026058 WHERE user_id = 16;
UPDATE users SET city = 'Caloocan', province = 'Metro Manila', latitude = 14.646338, longitude = 120.981657 WHERE user_id = 17;
UPDATE users SET city = 'Makati', province = 'Metro Manila', latitude = 14.556678, longitude = 121.039735 WHERE user_id = 18;
UPDATE users SET city = 'Bacolod City', province = 'Negros Occidental', latitude = 10.673112, longitude = 122.945251 WHERE user_id = 19;
UPDATE users SET city = 'Naga City', province = 'Camarines Sur', latitude = 13.608849, longitude = 123.184078 WHERE user_id = 20;
UPDATE users SET city = 'Pasig', province = 'Metro Manila', latitude = 14.566910, longitude = 121.065264 WHERE user_id = 21;
UPDATE users SET city = 'Davao City', province = 'Davao del Sur', latitude = 7.205068, longitude = 125.473309 WHERE user_id = 22;
UPDATE users SET city = 'Cavite City', province = 'Cavite', latitude = 14.491015, longitude = 120.892695 WHERE user_id = 23;
UPDATE users SET city = 'Davao City', province = 'Davao del Sur', latitude = 7.186718, longitude = 125.442924 WHERE user_id = 24;
UPDATE users SET city = 'Pasig', province = 'Metro Manila', latitude = 14.580429, longitude = 121.069195 WHERE user_id = 25;
UPDATE users SET city = 'Antipolo City', province = 'Rizal', latitude = 14.604158, longitude = 121.180649 WHERE user_id = 26;
UPDATE users SET city = 'Makati', province = 'Metro Manila', latitude = 14.560076, longitude = 121.042619 WHERE user_id = 27;
UPDATE users SET city = 'Antipolo City', province = 'Rizal', latitude = 14.600157, longitude = 121.195824 WHERE user_id = 28;
UPDATE users SET city = 'Baguio City', province = 'Benguet', latitude = 16.388065, longitude = 120.605987 WHERE user_id = 29;
UPDATE users SET city = 'Legazpi City', province = 'Albay', latitude = 13.120024, longitude = 123.761839 WHERE user_id = 30;
UPDATE users SET city = 'Iloilo City', province = 'Iloilo', latitude = 10.721324, longitude = 122.581240 WHERE user_id = 31;
UPDATE users SET city = 'Bacolod City', province = 'Negros Occidental', latitude = 10.692830, longitude = 122.945128 WHERE user_id = 32;
UPDATE users SET city = 'Pasig', province = 'Metro Manila', latitude = 14.580929, longitude = 121.096636 WHERE user_id = 33;
UPDATE users SET city = 'Caloocan', province = 'Metro Manila', latitude = 14.660295, longitude = 120.955870 WHERE user_id = 34;
UPDATE users SET city = 'Iloilo City', province = 'Iloilo', latitude = 10.731805, longitude = 122.560990 WHERE user_id = 35;
UPDATE users SET city = 'Pasig', province = 'Metro Manila', latitude = 14.595922, longitude = 121.103300 WHERE user_id = 36;
UPDATE users SET city = 'Cebu City', province = 'Cebu', latitude = 10.304774, longitude = 123.873268 WHERE user_id = 37;
UPDATE users SET city = 'Pasig', province = 'Metro Manila', latitude = 14.570160, longitude = 121.090825 WHERE user_id = 38;
UPDATE users SET city = 'Bacolod City', province = 'Negros Occidental', latitude = 10.686506, longitude = 122.950021 WHERE user_id = 39;
UPDATE users SET city = 'Manila', province = 'Metro Manila', latitude = 14.617347, longitude = 120.993073 WHERE user_id = 40;
UPDATE users SET city = 'Baguio City', province = 'Benguet', latitude = 16.422024, longitude = 120.577102 WHERE user_id = 41;
UPDATE users SET city = 'Antipolo City', province = 'Rizal', latitude = 14.603699, longitude = 121.162336 WHERE user_id = 42;
UPDATE users SET city = 'Iloilo City', province = 'Iloilo', latitude = 10.732174, longitude = 122.571155 WHERE user_id = 43;
UPDATE users SET city = 'Makati', province = 'Metro Manila', latitude = 14.567746, longitude = 121.012842 WHERE user_id = 44;
UPDATE users SET city = 'Taguig', province = 'Metro Manila', latitude = 14.528147, longitude = 121.043940 WHERE user_id = 45;
UPDATE users SET city = 'Iloilo City', province = 'Iloilo', latitude = 10.714351, longitude = 122.560426 WHERE user_id = 46;
UPDATE users SET city = 'Antipolo City', province = 'Rizal', latitude = 14.587140, longitude = 121.156848 WHERE user_id = 47;
UPDATE users SET city = 'Baguio City', province = 'Benguet', latitude = 16.389194, longitude = 120.594940 WHERE user_id = 48;
UPDATE users SET city = 'Legazpi City', province = 'Albay', latitude = 13.132139, longitude = 123.744534 WHERE user_id = 49;
UPDATE users SET city = 'Iloilo City', province = 'Iloilo', latitude = 10.707852, longitude = 122.543788 WHERE user_id = 50;
UPDATE users SET city = 'Makati', province = 'Metro Manila', latitude = 14.552430, longitude = 121.028901 WHERE user_id = 51;
UPDATE users SET city = 'Iloilo City', province = 'Iloilo', latitude = 10.718294, longitude = 122.563431 WHERE user_id = 52;
UPDATE users SET city = 'Baguio City', province = 'Benguet', latitude = 16.415900, longitude = 120.581485 WHERE user_id = 53;
UPDATE users SET city = 'Makati', province = 'Metro Manila', latitude = 14.561546, longitude = 121.021534 WHERE user_id = 54;
UPDATE users SET city = 'Pasig', province = 'Metro Manila', latitude = 14.593980, longitude = 121.090838 WHERE user_id = 55;
UPDATE users SET city = 'Cebu City', province = 'Cebu', latitude = 10.334402, longitude = 123.874184 WHERE user_id = 56;
UPDATE users SET city = 'Makati', province = 'Metro Manila', latitude = 14.567998, longitude = 121.010859 WHERE user_id = 57;
UPDATE users SET city = 'Davao City', province = 'Davao del Sur', latitude = 7.184965, longitude = 125.438988 WHERE user_id = 58;
UPDATE users SET city = 'Cebu City', province = 'Cebu', latitude = 10.313318, longitude = 123.866123 WHERE user_id = 59;
UPDATE users SET city = 'Cebu City', province = 'Cebu', latitude = 10.334568, longitude = 123.869591 WHERE user_id = 60;
INSERT INTO user_interests (user_id, category) VALUES
(1, 'Hiking'),
(1, 'Beach'),
(1, 'Island hopping'),
(2, 'Camping'),
(2, 'Hiking'),
(3, 'Hiking'),
(3, 'Food trip'),
(3, 'City tour'),
(3, 'Camping'),
(4, 'Camping'),
(4, 'Hiking'),
(4, 'City tour'),
(4, 'Road trip'),
(5, 'Hiking'),
(5, 'Camping'),
(5, 'City tour'),
(5, 'Beach'),
(6, 'Island hopping'),
(6, 'Beach'),
(6, 'Road trip'),
(7, 'Food trip'),
(7, 'Sightseeing'),
(8, 'Food trip'),
(8, 'Adventure'),
(9, 'Adventure'),
(9, 'Sightseeing'),
(9, 'Island hopping'),
(10, 'Beach'),
(10, 'Road trip'),
(11, 'Road trip'),
(11, 'Beach'),
(11, 'Hiking'),
(12, 'Adventure'),
(12, 'Camping'),
(13, 'Sightseeing'),
(13, 'City tour'),
(13, 'Adventure'),
(13, 'Island hopping'),
(14, 'Beach'),
(14, 'Road trip'),
(15, 'Road trip'),
(15, 'Adventure'),
(15, 'Island hopping'),
(15, 'Sightseeing'),
(16, 'Sightseeing'),
(16, 'Island hopping'),
(16, 'Road trip'),
(17, 'Island hopping'),
(17, 'Food trip'),
(17, 'Adventure'),
(18, 'Adventure'),
(18, 'Camping'),
(19, 'City tour'),
(19, 'Road trip'),
(19, 'Sightseeing'),
(20, 'Food trip'),
(20, 'Island hopping'),
(20, 'Hiking'),
(21, 'Adventure'),
(21, 'Island hopping'),
(22, 'Sightseeing'),
(22, 'City tour'),
(22, 'Beach'),
(22, 'Adventure'),
(23, 'Hiking'),
(23, 'Adventure'),
(23, 'Camping'),
(23, 'Sightseeing'),
(24, 'Beach'),
(24, 'Adventure'),
(24, 'Sightseeing'),
(25, 'Island hopping'),
(25, 'Beach'),
(25, 'City tour'),
(26, 'City tour'),
(26, 'Beach'),
(27, 'Camping'),
(27, 'Island hopping'),
(28, 'Adventure'),
(28, 'Beach'),
(28, 'Hiking'),
(29, 'Adventure'),
(29, 'Road trip'),
(29, 'Hiking'),
(30, 'Adventure'),
(30, 'Island hopping'),
(30, 'Road trip'),
(31, 'Island hopping'),
(31, 'Hiking'),
(31, 'Camping'),
(32, 'Road trip'),
(32, 'Sightseeing'),
(33, 'City tour'),
(33, 'Sightseeing'),
(33, 'Adventure'),
(33, 'Beach'),
(34, 'Food trip'),
(34, 'Camping'),
(35, 'Sightseeing'),
(35, 'Hiking'),
(35, 'Adventure'),
(36, 'Sightseeing'),
(36, 'Adventure'),
(36, 'Camping'),
(36, 'City tour'),
(37, 'Food trip'),
(37, 'Beach'),
(38, 'Hiking'),
(38, 'Adventure'),
(38, 'Sightseeing'),
(39, 'Beach'),
(39, 'Camping'),
(39, 'Adventure'),
(39, 'Sightseeing'),
(40, 'Sightseeing'),
(40, 'Beach'),
(40, 'Camping'),
(41, 'Beach'),
(41, 'Island hopping'),
(41, 'City tour'),
(42, 'Island hopping'),
(42, 'Adventure'),
(42, 'Sightseeing'),
(43, 'Hiking'),
(43, 'City tour'),
(44, 'Island hopping'),
(44, 'Camping'),
(44, 'Adventure'),
(44, 'Beach'),
(45, 'Road trip'),
(45, 'Food trip'),
(46, 'Island hopping'),
(46, 'Hiking'),
(46, 'Sightseeing'),
(47, 'Camping'),
(47, 'Island hopping'),
(47, 'Road trip'),
(47, 'Beach'),
(48, 'Hiking'),
(48, 'Island hopping'),
(49, 'City tour'),
(49, 'Hiking'),
(50, 'Beach'),
(50, 'Hiking'),
(50, 'City tour'),
(51, 'Adventure'),
(51, 'Hiking'),
(51, 'Camping'),
(51, 'City tour'),
(52, 'City tour'),
(52, 'Food trip'),
(52, 'Sightseeing'),
(52, 'Island hopping'),
(53, 'Food trip'),
(53, 'Road trip'),
(53, 'Adventure'),
(53, 'Beach'),
(54, 'Adventure'),
(54, 'Sightseeing'),
(54, 'Hiking'),
(55, 'Road trip'),
(55, 'Beach'),
(55, 'Camping'),
(55, 'Adventure'),
(56, 'Road trip'),
(56, 'Island hopping'),
(57, 'Adventure'),
(57, 'Island hopping'),
(57, 'Sightseeing'),
(58, 'Camping'),
(58, 'Sightseeing'),
(58, 'Food trip'),
(58, 'Beach'),
(59, 'Sightseeing'),
(59, 'Adventure'),
(60, 'Road trip'),
(60, 'Beach'),
(60, 'Hiking'),
(60, 'Adventure');

-- sign-up details of the sample travelers
UPDATE users SET birthdate = '2000-09-28', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 576 8402' WHERE user_id = 1;
UPDATE users SET birthdate = '2002-10-07', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 624 8794' WHERE user_id = 2;
UPDATE users SET birthdate = '1991-02-15', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 192 9825' WHERE user_id = 3;
UPDATE users SET birthdate = '1987-10-13', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 769 3580' WHERE user_id = 4;
UPDATE users SET birthdate = '1986-09-03', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 294 4963' WHERE user_id = 5;
UPDATE users SET birthdate = '1986-08-11', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 962 4200' WHERE user_id = 6;
UPDATE users SET birthdate = '2002-04-21', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 104 2392' WHERE user_id = 7;
UPDATE users SET birthdate = '2000-11-09', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 664 2363' WHERE user_id = 8;
UPDATE users SET birthdate = '1994-06-25', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 395 1487' WHERE user_id = 9;
UPDATE users SET birthdate = '1988-10-25', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 210 5766' WHERE user_id = 10;
UPDATE users SET birthdate = '1998-02-01', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 100 4498' WHERE user_id = 11;
UPDATE users SET birthdate = '1992-01-16', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 825 7511' WHERE user_id = 12;
UPDATE users SET birthdate = '1999-02-19', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 897 5420' WHERE user_id = 13;
UPDATE users SET birthdate = '1996-02-10', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 519 2933' WHERE user_id = 14;
UPDATE users SET birthdate = '1990-04-23', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 161 8617' WHERE user_id = 15;
UPDATE users SET birthdate = '2001-03-22', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 558 9337' WHERE user_id = 16;
UPDATE users SET birthdate = '1992-12-25', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 759 7287' WHERE user_id = 17;
UPDATE users SET birthdate = '1989-07-14', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 100 5420' WHERE user_id = 18;
UPDATE users SET birthdate = '2004-05-01', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 503 2643' WHERE user_id = 19;
UPDATE users SET birthdate = '1987-03-07', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 109 6389' WHERE user_id = 20;
UPDATE users SET birthdate = '1995-07-03', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 313 4981' WHERE user_id = 21;
UPDATE users SET birthdate = '1986-10-12', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 564 3084' WHERE user_id = 22;
UPDATE users SET birthdate = '2004-08-27', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 987 7325' WHERE user_id = 23;
UPDATE users SET birthdate = '1991-11-05', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 333 5087' WHERE user_id = 24;
UPDATE users SET birthdate = '1992-03-24', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 667 4220' WHERE user_id = 25;
UPDATE users SET birthdate = '1998-08-20', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 148 2701' WHERE user_id = 26;
UPDATE users SET birthdate = '1989-01-17', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 344 7416' WHERE user_id = 27;
UPDATE users SET birthdate = '1994-07-27', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 602 5807' WHERE user_id = 28;
UPDATE users SET birthdate = '2002-03-24', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 229 4742' WHERE user_id = 29;
UPDATE users SET birthdate = '2001-09-21', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 729 2214' WHERE user_id = 30;
UPDATE users SET birthdate = '1994-04-07', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 170 5410' WHERE user_id = 31;
UPDATE users SET birthdate = '1999-08-08', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 280 5619' WHERE user_id = 32;
UPDATE users SET birthdate = '1997-09-19', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 194 6933' WHERE user_id = 33;
UPDATE users SET birthdate = '1990-08-11', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 807 9551' WHERE user_id = 34;
UPDATE users SET birthdate = '2004-03-19', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 118 8779' WHERE user_id = 35;
UPDATE users SET birthdate = '1997-12-10', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 121 2226' WHERE user_id = 36;
UPDATE users SET birthdate = '2001-02-24', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 239 2186' WHERE user_id = 37;
UPDATE users SET birthdate = '1988-08-18', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 145 3122' WHERE user_id = 38;
UPDATE users SET birthdate = '1996-06-03', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 179 7834' WHERE user_id = 39;
UPDATE users SET birthdate = '1986-08-19', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 778 7264' WHERE user_id = 40;
UPDATE users SET birthdate = '1998-10-01', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 182 2485' WHERE user_id = 41;
UPDATE users SET birthdate = '1989-05-14', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 497 8499' WHERE user_id = 42;
UPDATE users SET birthdate = '2000-08-27', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 631 9430' WHERE user_id = 43;
UPDATE users SET birthdate = '1986-05-20', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 122 4771' WHERE user_id = 44;
UPDATE users SET birthdate = '1989-08-25', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 597 5191' WHERE user_id = 45;
UPDATE users SET birthdate = '1986-06-10', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 726 4318' WHERE user_id = 46;
UPDATE users SET birthdate = '2002-03-25', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 775 8240' WHERE user_id = 47;
UPDATE users SET birthdate = '2001-04-11', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 356 4252' WHERE user_id = 48;
UPDATE users SET birthdate = '1999-04-07', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 697 6185' WHERE user_id = 49;
UPDATE users SET birthdate = '1992-03-05', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 957 1664' WHERE user_id = 50;
UPDATE users SET birthdate = '1988-05-27', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 561 8722' WHERE user_id = 51;
UPDATE users SET birthdate = '1994-04-27', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 740 9519' WHERE user_id = 52;
UPDATE users SET birthdate = '2001-11-11', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 962 8413' WHERE user_id = 53;
UPDATE users SET birthdate = '1996-02-27', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 993 1679' WHERE user_id = 54;
UPDATE users SET birthdate = '1994-10-12', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 910 1313' WHERE user_id = 55;
UPDATE users SET birthdate = '1990-07-15', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 887 5364' WHERE user_id = 56;
UPDATE users SET birthdate = '1993-03-26', gender = 'Prefer not to say', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 744 2889' WHERE user_id = 57;
UPDATE users SET birthdate = '2000-02-21', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 755 7040' WHERE user_id = 58;
UPDATE users SET birthdate = '1988-11-07', gender = 'Female', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 586 5194' WHERE user_id = 59;
UPDATE users SET birthdate = '1991-12-01', gender = 'Male', emergency_contact_name = 'Family contact', emergency_contact_phone = '0918 647 1592' WHERE user_id = 60;
UPDATE id_verifications SET id_type = 'National ID (PhilSys)';

-- every member has read the sample group chats (new messages after this show as unread)
INSERT INTO chat_reads (user_id, activity_id, last_read_message_id)
SELECT a.user_id, a.activity_id,
       COALESCE((SELECT MAX(m.message_id) FROM group_messages m WHERE m.activity_id = a.activity_id), 0)
FROM activities a;
INSERT INTO chat_reads (user_id, activity_id, last_read_message_id)
SELECT b.user_id, a.activity_id,
       COALESCE((SELECT MAX(m.message_id) FROM group_messages m WHERE m.activity_id = a.activity_id), 0)
FROM bookings b JOIN activities a ON a.activity_id = b.activity_id
WHERE b.status = 'APPROVED' AND b.user_id <> a.user_id;
