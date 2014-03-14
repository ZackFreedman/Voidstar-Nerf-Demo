/* * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Hardware Glassware Nerf Blaster Firmware          *
 * by Zack Freedman of Voidstar Lab                  *
 * More info: github.com/zackfreedman/nerfglassware  *
 *                                                   *
 * This code is released into the                    *
 * public domain.                                    *
 * * * * * * * * * * * * * * * * * * * * * * * * * * */

//Uncomment to initialize the RN41 or 42. You only need to do this once.
//#define CONFIGURE_RN4X

// These pins chosen for convenience
#define armedSensor 16
#define magSensor 15

// Use these pins instead if you want interrupts
//#define armedSensor 2
//#define magSensor 3

// Change this to fit your mag.
// TODO: Hack the project to detect mag size.
#define defaultMagSize 18 
#define timeBetweenTransmissions 100 // Keep below 200ms for responsiveness

#define bluetooth Serial1 // Syntactic sugar
// You can create a SoftwareSerial port use Hardware Serial, etc and refer to it in the previous line

byte ammoLeft;
byte magSize;

boolean isArmed;
boolean wasArmed;
boolean magIsInserted;
boolean magWasInserted;

void setup() {
  bluetooth.begin(115200);
  Serial.begin(115200);

  pinMode(armedSensor, INPUT_PULLUP); // Pullup resistors are for chumps
  pinMode(magSensor, INPUT_PULLUP);

  isArmed = wasArmed = readArmedSensor();
  magIsInserted = magWasInserted = readMagSensor();

  if (magIsInserted) ammoLeft = defaultMagSize; // TODO: Detect how much ammo is left in the mag

#ifdef CONFIGURE_RN4X
  bluetooth.print("$$$"); // Put RN4X into command mode
  delay(200);
  bluetooth.println("SA,1"); // Most important configuration. Sets to SPP security, which doesn't need password entry.
  delay(200);
  bluetooth.println("SN,Nerf"); // Recognized by Android code
  delay(200);
  bluetooth.println("SO,>"); // Causes RN4X to print status strings (pairing, connecting, etc)
  delay(200);
  bluetooth.println("SP,0000"); // Just in case SPP fails, fall back to a known password
  delay(200);
  bluetooth.println("R,1"); // Restart the module
  delay(1000);
#endif
}

void loop() {
  while (bluetooth.available() > 1) {
    if (bluetooth.read() == '>' &&
      bluetooth.read() == 'N' &&
      bluetooth.read() == 'E' &&
      bluetooth.read() == 'W') { // Detects ">NEW PAIRING XXXXX" message
      bluetooth.println("$$$");
      delay(100);
      bluetooth.println("R,1"); // Restart device. A bug in the RN4X prevents SPP from working immediately after pairing
    }
  }

  isArmed = readArmedSensor();
  magIsInserted = readMagSensor();

  if (magIsInserted) {
    if (!magWasInserted) ammoLeft = magSize = defaultMagSize; // If a mag has just been loaded, replenish ammo
    else if (!isArmed && wasArmed) ammoLeft = max(ammoLeft - 1, 0); // If a shot was just fired, remove a round from the clip
  }
  else ammoLeft = magSize = 0;

  wasArmed = isArmed;
  magWasInserted = magIsInserted;

  writePacketTo(bluetooth);
  if (bluetooth != Serial) writePacketTo(Serial); // For debugging

  delay(timeBetweenTransmissions); // Prevents Android device's BT serial buffer from overloading
}

boolean readArmedSensor() {
  return !digitalRead(armedSensor); // These are active low so we can use the internal pullups
}

boolean readMagSensor() {
  return !digitalRead(magSensor);
}

void writePacketTo(Stream &stream) { // Abstracts out packet assembly so we can use same method to send and debug
  stream.print('<'); // Start of packet (preamble)
  stream.write(ammoLeft);
  stream.write(magSize);
  stream.write(isArmed);
  stream.write(magIsInserted);
  stream.print('>'); // End of packet (delimiter)
}






