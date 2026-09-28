INSERT INTO roles (id, name, description, created_at)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    'CUSTOMER',
    'Standard platform customer',
    CURRENT_TIMESTAMP
),
(
    '10000000-0000-0000-0000-000000000002',
    'SUPPORT',
    'Customer support operator',
    CURRENT_TIMESTAMP
),
(
    '10000000-0000-0000-0000-000000000003',
    'ADMIN',
    'Platform administrator',
    CURRENT_TIMESTAMP
);

INSERT INTO permissions (id, name, description)
VALUES
(
    '20000000-0000-0000-0000-000000000001',
    'PROFILE_READ',
    'Read own customer profile'
),
(
    '20000000-0000-0000-0000-000000000002',
    'PORTFOLIO_READ',
    'Read own portfolio'
),
(
    '20000000-0000-0000-0000-000000000003',
    'ORDER_CREATE',
    'Create investment order'
),
(
    '20000000-0000-0000-0000-000000000004',
    'USER_READ',
    'Read user information'
),
(
    '20000000-0000-0000-0000-000000000005',
    'ROLE_ASSIGN',
    'Assign roles to users'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'PROFILE_READ',
    'PORTFOLIO_READ',
    'ORDER_CREATE'
)
WHERE r.name = 'CUSTOMER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'PROFILE_READ',
    'USER_READ'
)
WHERE r.name = 'SUPPORT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
WHERE r.name = 'ADMIN';