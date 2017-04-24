#include <SoftwareSerial.h>

SoftwareSerial bleSerial(3, 2);
// SoftwareSerial(rxPin, txPin)
// RX is digital pin 3 (connect to TX of other device)
// TX is digital pin 2 (connect to RX of other device)
//
// ATtiny85     HM-10
// ------------------
// Pin 3    <-  TX
// Pin 2    ->  RX

/*              Protocol
---------------------------------------------
| 00000000 | 00000000 | 00000000 | 00000000 |
---------------------------------------------
    |           |           |           |
    |           |           |           |
    v           v           v           v
  Command      Red        Green       Blue
    |
    |
    |--> 000000XX Action {00: Off, 01: On, 10: Blink, 11: Pulse}
    |
    |--> 0000XX00 Blink/Pulse amount {00: 1, 01: 2, 10: 3, 11: 5}
    |
    |--> 00XX0000 Blink/Pulse duration {00: Fastest, 01: Fast, 10: Normal, 11: Slow}
    |
    |--> XX000000 Blink/Pulse Repeat {00: 0, 01: 5 secs, 10: 15 secs, 11: 30 secs}
*/

// Data
unsigned char COMMAND;
unsigned char RED;
unsigned char GREEN;
unsigned char BLUE;

// Defaults
int ACTION = 0;
int AMOUNT = 0;
int DURATION = 0;
int REPEAT = 0;
int MAP_ACTION[] = {0, 1, 2, 3};
int MAP_AMOUNT[] = {1, 2, 3, 5};
int MAP_DURATION[] = {50, 100, 250, 500};
int MAP_REPEAT[] = {0, 5000, 15000, 30000};

// LED
int REDPIN = 1;
int GREPIN = 0;
int BLUPIN = 4;

// Time
int TIMESTAMP = 0;

void setup() {
  // Set pins for LEDs as outputs
  pinMode(REDPIN, OUTPUT);
  pinMode(GREPIN, OUTPUT);
  pinMode(BLUPIN, OUTPUT);

  // HM-10 defaults to 9600 baud rate
  bleSerial.begin(9600);

  // Configure the HM-10 module
  delay(250);
  bleSerial.print("AT+ROLE0"); // Slave mode (0: Peripheral, 1: Central, default: 0)
  delay(250);
  bleSerial.print("AT+MODE2"); // (0: Tranmission, 1: PIO+0, 2: Remote Control+0, default: 0)
  delay(250);
  bleSerial.print("AT+IMME1"); // (0: Work immediately, 1: wait for AT+ commands, wait for AT+START to resume work, default: 0) Don't enter transmission mode until told. ("AT+IMME0" is wait until "AT+START" to work. "AT+WORK1" is connect right away.)
  delay(250);
  bleSerial.print("AT+BAUD0"); // (0: 9600, 1: 19200, 2: 38400, 3: 57600, 4: 115200)
  delay(250);
  bleSerial.print("AT+NAMETessract"); // Set name
  delay(250);
  bleSerial.print("AT+PASS123456"); // Set password
  delay(250);
  bleSerial.print("AT+START"); // Work immediately when AT+IMME1 is set.
  delay(250);

  TIMESTAMP = millis();
}

void loop() {
  // Check for data
  if (bleSerial.available()) {
    // Read in data
    while (bleSerial.available()) {
      COMMAND = bleSerial.read();
      delay(1);
      RED = bleSerial.read();
      delay(1);
      GREEN = bleSerial.read();
      delay(1);
      BLUE = bleSerial.read();
      delay(1);
    }

    // Parse command
    parseCommands();

    // Run command
    runCommands();
  }
}

/* Parses the COMMAND variable according to the protocol
 * @returns {void}
 */
void parseCommands() {
  // Reset to defaults
  ACTION = 0;
  AMOUNT = 0;
  DURATION = 0;
  REPEAT = 0;

  // Shift out values
  REPEAT = COMMAND & 3;
  COMMAND >>= 2;
  DURATION = COMMAND & 3;
  COMMAND >>= 2;
  AMOUNT = COMMAND & 3;
  COMMAND >>= 2;
  ACTION = COMMAND & 3;
}

/* Executes the commands
 * @returns {void}
 */
void runCommands() {
  // Turn off
  if (ACTION == MAP_ACTION[0]) {
    toggleLed(0, 0, 0);
  }

  // Turn on
  if (ACTION == MAP_ACTION[1]) {
    toggleLed(RED, GREEN, BLUE);
  }

  // Blink
  if (ACTION == MAP_ACTION[2]) {
    blinkLed(MAP_AMOUNT[AMOUNT], MAP_DURATION[DURATION], RED, GREEN, BLUE);
  }

  // Pulse
  if (ACTION == MAP_ACTION[3]) {
    pulseLed(MAP_AMOUNT[AMOUNT], MAP_DURATION[DURATION], RED, GREEN, BLUE);
  }

  // Repeat
  if (REPEAT > MAP_REPEAT[0]) {
    int now = millis();
    int diff = now - TIMESTAMP;

    if (diff > MAP_REPEAT[REPEAT]) {
      TIMESTAMP = millis();
      runCommands();
    }
  }
}

/* Blinks the LED
 * @param {Integer} amount - The amount of times to blink
 * @param {Integer} duratiion - The duration (ms) in between blinks
 * @param {Integer} red - The value of red
 * @param {Integer} green - The value of green
 * @param {Integer} blue - The value of blue
 * @returns {void}
 */
void blinkLed(int amount, int duration, unsigned char red, unsigned char green, unsigned char blue) {
  // Iterate over amount of blinks
  for (int i = 0; i < amount; i++) {
    analogWrite(REDPIN, red);
    analogWrite(GREPIN, green);
    analogWrite(BLUPIN, blue);
    delay(duration);
    analogWrite(REDPIN, 0);
    analogWrite(GREPIN, 0);
    analogWrite(BLUPIN, 0);
    delay(duration);
  }
}

/* Pulses the LED
 * @param {Integer} amount - The amount of times to blink
 * @param {Integer} duratiion - The duration (ms) in between blinks
 * @param {Integer} red - The value of red
 * @param {Integer} green - The value of green
 * @param {Integer} blue - The value of blue
 * @returns {void}
 */
void pulseLed(int amount, int duration, unsigned char red, unsigned char green, unsigned char blue) {
  // Convert duration
  int _delay = 1;
  if (duration == MAP_DURATION[3]) {
    _delay = 8;
  } else if (duration == MAP_DURATION[2]) {
    _delay = 5;
  } else if (duration == MAP_DURATION[1]) {
    _delay = 3;
  } else if (duration == MAP_DURATION[0]) {
    _delay = 1;
  }

  // Iterate over amount of pulses
  for (int i = 0; i < amount; i++) {
    // Pulse up
    for (int j = 0; j < 255; j++) {
      if (j <= red) {
        analogWrite(REDPIN, j);
      }
      if (j <= green) {
        analogWrite(GREPIN, j);
      }
      if (j <= blue) {
        analogWrite(BLUPIN, j);
      }
      // Magic delay
      if (j <= 55) {
        delay(_delay);
      }
      delay(_delay);
    }

    // Pulse down
    for (int k = 255; k >= 0; k--) {
      if (k <= red) {
        analogWrite(REDPIN, k);
      }
      if (k <= green) {
        analogWrite(GREPIN, k);
      }
      if (k <= blue) {
        analogWrite(BLUPIN, k);
      }
      delay(_delay);
    }

    // Wait
    delay(100);
  }
}

/* Toggles LEDs
 * @param {Integer} red - The value of red
 * @param {Integer} green - The value of green
 * @param {Integer} blue - The value of blue
 * @returns {void}
 */
void toggleLed(unsigned char red, unsigned char green, unsigned char blue) {
  analogWrite(REDPIN, red);
  analogWrite(GREPIN, green);
  analogWrite(BLUPIN, blue);
}

