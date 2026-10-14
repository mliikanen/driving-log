## MODIFIED Requirements

### Requirement: The details screen shows a "Refueling" event's fuel details
The system SHALL show, on the details screen of a refueling's entry, its fuel amount (in the unit it was entered in),
its fuel type, whether it was a full fill-up and, when the entry also holds a "Distance" or "Odometer reading" event,
that event's figure as its mileage line, shown as that event's own figure is. The screen SHALL be the same whether
opened from the "Refueling" row or the mileage row. A refueling with no mileage SHALL show no mileage line.

#### Scenario: A refueling's details
- **WHEN** the user opens the details screen of a "Refueling" event of 42.3 L, Diesel, filled up, with a trip-distance mileage of 30 km
- **THEN** the screen shows "42.30 L," "Diesel," that it was a full fill-up, and "+30 km" as its mileage

#### Scenario: Opened from the mileage row
- **WHEN** the user taps the "Distance" row of a refueling logged with a trip-distance mileage of 30 km
- **THEN** the screen shows the refueling's fuel amount, fuel type and fill-up state, and "+30 km" as its mileage, exactly as when opened from its "Refueling" row

#### Scenario: No mileage line without one
- **WHEN** the user opens the details screen of a "Refueling" event saved with no mileage
- **THEN** the screen shows the fuel amount, fuel type and fill-up state, and no mileage line
