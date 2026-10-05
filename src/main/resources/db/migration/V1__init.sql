CREATE TABLE coordinates (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    x BIGINT NOT NULL,
    y REAL NOT NULL,

    CONSTRAINT pk_coordinates PRIMARY KEY (id),
    CONSTRAINT chk_coordinates_y_max CHECK (y <= 791)
);

CREATE TABLE cars (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    cool BOOLEAN NOT NULL,

    CONSTRAINT pk_cars PRIMARY KEY (id)
);

CREATE TABLE human_beings (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    name TEXT NOT NULL,
    coordinates_id INTEGER NOT NULL,
    creation_date DATE NOT NULL DEFAULT CURRENT_DATE,
    real_hero BOOLEAN NOT NULL,
    has_toothpick BOOLEAN NOT NULL,
    car_id INTEGER NOT NULL,
    mood TEXT,
    impact_speed BIGINT,
    minutes_of_waiting DOUBLE PRECISION NOT NULL,
    weapon_type TEXT,

    CONSTRAINT pk_human_beings PRIMARY KEY (id),
    CONSTRAINT chk_human_beings_id_positive CHECK (id > 0),
    CONSTRAINT chk_human_beings_name_not_empty CHECK (name <> ''),
    CONSTRAINT chk_human_beings_mood CHECK (
        mood IN ('GLOOM', 'RAGE', 'FRENZY')
    ),
    CONSTRAINT chk_human_beings_weapon_type CHECK (
        weapon_type IN ('PISTOL', 'RIFLE', 'KNIFE', 'BAT')
    ),
    CONSTRAINT fk_human_beings_coordinates FOREIGN KEY (coordinates_id)
        REFERENCES coordinates (id) ON DELETE RESTRICT,
    CONSTRAINT fk_human_beings_car FOREIGN KEY (car_id)
        REFERENCES cars (id) ON DELETE RESTRICT
);
