// Waits until the clock shows the next whole minute. The log distance form's default time is to the minute, so an entry
// logged in the same minute as the vehicle's initial odometer would be dated before it and would not change the odometer.
var next = Math.floor(Date.now() / 60000) * 60000 + 60000
while (Date.now() < next + 1000) {
}
