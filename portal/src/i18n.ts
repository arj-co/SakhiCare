export type PortalLanguage = "en" | "hi" | "mr";
export const portalCopy: Record<PortalLanguage, Record<string, string>> = {
  en: { workspace: "Workspace", queue: "Patient queue", transport: "108 transport", notifications: "Notifications", directory: "Directory & guidance", facilities: "Facilities", workers: "ASHA workers", protocols: "Protocols", live: "Live sync", connecting: "Connecting", signOut: "Sign out" },
  hi: { workspace: "कार्य क्षेत्र", queue: "मरीज़ कतार", transport: "108 परिवहन", notifications: "सूचनाएं", directory: "निर्देशिका और मार्गदर्शन", facilities: "स्वास्थ्य केंद्र", workers: "आशा कार्यकर्ता", protocols: "प्रोटोकॉल", live: "लाइव सिंक", connecting: "कनेक्ट हो रहा है", signOut: "साइन आउट" },
  mr: { workspace: "कार्य क्षेत्र", queue: "रुग्णांची यादी", transport: "108 वाहतूक", notifications: "सूचना", directory: "निर्देशिका आणि मार्गदर्शन", facilities: "आरोग्य केंद्रे", workers: "आशा सेविका", protocols: "प्रोटोकॉल", live: "लाइव्ह सिंक", connecting: "जोडत आहे", signOut: "साइन आउट" },
};
