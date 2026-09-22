# Spec Delta

## Purpose
Lets a logged event carry a picture, taken or chosen the way a vehicle's own picture is, stored in the application's
private storage and referenced by id. A stub: the requirement below is the proposed shape, settled together with the
open questions in the design.

## ADDED Requirements

### Requirement: A picture can be attached when logging an event
The system SHALL let the user attach a picture to an event on the log event form, through the system's own chooser
of image sources, the same way a vehicle's picture is chosen. Saving the form SHALL store the picture with the new
event. Leaving the form without saving SHALL add neither the event nor the picture.

#### Scenario: Log an event with a picture
- **WHEN** the user attaches a picture while logging a distance entry and saves
- **THEN** the log contains the new entry together with that picture

#### Scenario: No picture is optional
- **WHEN** the user saves a valid entry without attaching a picture
- **THEN** the log contains the new entry with no picture
