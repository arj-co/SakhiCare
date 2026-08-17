// Care Desk Audio Controller
function playEmergencyChime(isMuted) {
    if (isMuted) return;
    try {
        const audio = new Audio("https://actions.google.com/sounds/v1/alarms/beep_short.ogg");
        audio.play();
    } catch (e) {
        console.warn("Audio play blocked by browser policy:", e);
    }
}
