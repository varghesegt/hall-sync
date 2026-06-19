-- Seed halls data into the database
-- ═══════════════════════════════════════════════════════════════════
-- HALLS: Matches frontend roomData.ts — 85 rooms, 25 seats each
-- Rooms: LH (Lecture Hall), SH (Seminar Hall), CONF (Conference Hall)
-- ═══════════════════════════════════════════════════════════════════

-- 2nd Floor: LH 201–220 (20 rooms)
INSERT INTO halls (id, name, capacity) VALUES ('LH-201', 'LH 201', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-202', 'LH 202', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-203', 'LH 203', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-204', 'LH 204', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-205', 'LH 205', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-206', 'LH 206', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-207', 'LH 207', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-208', 'LH 208', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-209', 'LH 209', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-210', 'LH 210', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-211', 'LH 211', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-212', 'LH 212', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-213', 'LH 213', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-214', 'LH 214', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-215', 'LH 215', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-216', 'LH 216', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-217', 'LH 217', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-218', 'LH 218', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-219', 'LH 219', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-220', 'LH 220', 25) ON CONFLICT (id) DO NOTHING;

-- 3rd Floor: LH 301–321 (21 rooms)
INSERT INTO halls (id, name, capacity) VALUES ('LH-301', 'LH 301', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-302', 'LH 302', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-303', 'LH 303', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-304', 'LH 304', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-305', 'LH 305', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-306', 'LH 306', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-307', 'LH 307', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-308', 'LH 308', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-309', 'LH 309', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-310', 'LH 310', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-311', 'LH 311', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-312', 'LH 312', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-313', 'LH 313', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-314', 'LH 314', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-315', 'LH 315', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-316', 'LH 316', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-317', 'LH 317', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-318', 'LH 318', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-319', 'LH 319', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-320', 'LH 320', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-321', 'LH 321', 25) ON CONFLICT (id) DO NOTHING;

-- 3rd Floor: SH 301, 302, 303 (3 rooms)
INSERT INTO halls (id, name, capacity) VALUES ('SH-301', 'SH 301', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('SH-302', 'SH 302', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('SH-303', 'SH 303', 25) ON CONFLICT (id) DO NOTHING;

-- 4th Floor: LH 401–420 (20 rooms)
INSERT INTO halls (id, name, capacity) VALUES ('LH-401', 'LH 401', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-402', 'LH 402', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-403', 'LH 403', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-404', 'LH 404', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-405', 'LH 405', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-406', 'LH 406', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-407', 'LH 407', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-408', 'LH 408', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-409', 'LH 409', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-410', 'LH 410', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-411', 'LH 411', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-412', 'LH 412', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-413', 'LH 413', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-414', 'LH 414', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-415', 'LH 415', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-416', 'LH 416', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-417', 'LH 417', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-418', 'LH 418', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-419', 'LH 419', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-420', 'LH 420', 25) ON CONFLICT (id) DO NOTHING;

-- 5th Floor: LH 501–515 (15 rooms)
INSERT INTO halls (id, name, capacity) VALUES ('LH-501', 'LH 501', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-502', 'LH 502', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-503', 'LH 503', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-504', 'LH 504', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-505', 'LH 505', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-506', 'LH 506', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-507', 'LH 507', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-508', 'LH 508', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-509', 'LH 509', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-510', 'LH 510', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-511', 'LH 511', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-512', 'LH 512', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-513', 'LH 513', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-514', 'LH 514', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('LH-515', 'LH 515', 25) ON CONFLICT (id) DO NOTHING;

-- 5th Floor: SH 501, 502, 503 (3 rooms)
INSERT INTO halls (id, name, capacity) VALUES ('SH-501', 'SH 501', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('SH-502', 'SH 502', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('SH-503', 'SH 503', 25) ON CONFLICT (id) DO NOTHING;

-- Conference Halls: CONF 1, 2, 3 (3 rooms)
INSERT INTO halls (id, name, capacity) VALUES ('CONF-1', 'CONF 1', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('CONF-2', 'CONF 2', 25) ON CONFLICT (id) DO NOTHING;
INSERT INTO halls (id, name, capacity) VALUES ('CONF-3', 'CONF 3', 25) ON CONFLICT (id) DO NOTHING;
