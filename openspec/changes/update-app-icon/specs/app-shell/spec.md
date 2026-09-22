# Spec Delta

## ADDED Requirements

### Requirement: The application has a custom launcher icon
The system SHALL show a custom adaptive launcher icon (foreground and background layers) in the device launcher and
app drawer, in place of no custom icon being declared today. A stub: the icon's artwork, its background color and
whether a themed/monochrome variant is included are settled together with the open questions in the design, once
the developer supplies the source SVG.

#### Scenario: The launcher shows the custom icon
- **WHEN** the installed application is viewed in the device launcher or app drawer
- **THEN** it shows the custom icon, not a default or missing one

### Requirement: The launch window shows a splash icon
The system SHALL show a splash icon on the launch window during cold start, layered on top of the existing
background-color behavior (`app-shell`'s "Material Design 3 theming" requirement, unchanged by this delta). A stub:
whether the splash icon reuses the launcher's artwork as-is or a simplified variant is an open question in the
design.

#### Scenario: Cold start shows the splash icon
- **WHEN** the user launches the application from a cold start
- **THEN** the launch window shows the splash icon on the scheme's background color, before the first composed frame
