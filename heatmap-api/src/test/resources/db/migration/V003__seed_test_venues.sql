INSERT INTO venues (
  id,
  owner_id,
  name,
  address,
  lat,
  lng,
  h3_res9,
  category,
  description,
  status
)
VALUES
  (
    '22222222-2222-2222-2222-222222222221',
    '11111111-1111-1111-1111-111111111111',
    'Cafe One',
    'Kazan Center, 1',
    55.7961,
    49.1064,
    617733123456789001,
    'food',
    'Seed venue for generator checks',
    'ACTIVE'
  ),
  (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Bar One',
    'Kazan Center, 2',
    55.7905,
    49.1140,
    617733123456789002,
    'entertainment',
    'Seed venue for generator checks',
    'ACTIVE'
  ),
  (
    '22222222-2222-2222-2222-222222222223',
    '11111111-1111-1111-1111-111111111111',
    'Shop One',
    'Kazan Center, 3',
    55.8030,
    49.0950,
    617733123456789003,
    'shopping',
    'Seed venue for generator checks',
    'ACTIVE'
  );
