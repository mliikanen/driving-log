# Test photos

The pictures the Maestro flows put on the emulator (`addMedia`) and the unit tests read. A flow that needs a picture adds it itself, so a fresh emulator is enough.

| File | Used for |
|---|---|
| `photo-landscape.png` | A generated landscape image (red, white and blue bands, a green disc): the crop, the stored versions and the picture flows. |
| `photo-solid-purple.png` | A generated solid color that is not one of the vehicle color presets: the color taken from a picture. |
| `photo-car-red.jpg`, `photo-car-blue.jpg`, `photo-car-grey.jpg`, `photo-car-white.jpg` | Real photos of cars in the street: the color taken from a picture (flow 23, and `RealPhotoColorJvmTest`, which checks the extracted color of each). They have no location data. |
