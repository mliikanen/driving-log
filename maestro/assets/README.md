# Test photos

The pictures the Maestro flows put on the emulator (`addMedia`) and the unit tests read. The setup flow of each manifest that needs pictures (`setup.yaml` in its directory) adds them once, so a fresh emulator is enough.

| File | Used for |
|---|---|
| `photo-landscape.png` | A generated landscape image (red, white and blue bands, a green disc): the crop, the stored versions and the `picture` and `resilience` flows (uploaded by their `setup.yaml`). |
| `photo-solid-purple.png` | A generated solid color that is not one of the vehicle color presets: the color taken from a picture (the `appearance` manifest's `setup.yaml` uploads it, with `photo-car-red.jpg`). |
| `photo-car-red.jpg`, `photo-car-blue.jpg`, `photo-car-grey.jpg`, `photo-car-white.jpg` | Real photos of cars in the street: the color taken from a picture (`photo-car-red.jpg` is chosen by the `appearance` flow `color-from-photo`; all four are read by `RealPhotoColorJvmTest`, which checks the extracted color of each). They have no location data. |
