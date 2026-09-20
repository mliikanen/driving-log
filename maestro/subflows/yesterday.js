// The digits to type into the form's date picker (MMDDYYYY, en-US; the picker adds the slashes) for yesterday and for
// tomorrow, and the name of yesterday's weekday.
var d = new Date(Date.now() - 24 * 60 * 60 * 1000)
var pad = function (n) { return (n < 10 ? "0" : "") + n }
output.yesterday = pad(d.getMonth() + 1) + pad(d.getDate()) + d.getFullYear()
output.yesterdayWeekday = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"][d.getDay()]
var t = new Date(Date.now() + 24 * 60 * 60 * 1000)
output.tomorrow = pad(t.getMonth() + 1) + pad(t.getDate()) + t.getFullYear()
