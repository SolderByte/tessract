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

  test();
}

void loop() {
  cycleRGB(10);
}

/* Test suite
 * @returns {void}
 */
void test() {
  /* LED Tests */
  // Turn on red LED
  toggleLed(REDPIN, 255, 500);

  // Turn on green LED
  toggleLed(GREPIN, 255, 500);

  // Turn on blue LED
  toggleLed(BLUPIN, 255, 500);

  // Turn on all LEDs
  toggleAllLed(255, 500);
}

/* Toggles LED
 * @param {Integer} pin - The pin number
 * @param {Integer} value - The value for the duty cycle
 * @param {Integer} time - The time for the delay in milliseconds
 * @returns {void}
 */
void toggleLed(int pin, int value, int time) {
  analogWrite(pin, value);
  delay(time);
  analogWrite(pin, 0);
  delay(time);
}

/* Toggles all LEDs
 * @param {Integer} value - The value for the duty cycle
 * @param {Integer} time - The time for the delay in milliseconds
 * @returns {void}
 */
void toggleAllLed(int value, int time) {
  analogWrite(REDPIN, 255);
  analogWrite(GREPIN, 255);
  analogWrite(BLUPIN, 255);
  delay(time);
  analogWrite(REDPIN, 0);
  analogWrite(GREPIN, 0);
  analogWrite(BLUPIN, 0);
  delay(time);
}

/* Cycles an RGB led trough the spectrum
 * @param {Integer} time - The time for the delay in milliseconds
 * @returns {void}
 */
void cycleRGB(int time) {
  // Default state
  analogWrite(REDPIN, 255);
  // Green increase
  for (int i = 0; i < 255; i++) {
    analogWrite(GREPIN, i);
    delay(time);
  }
  // Red decrease
  for (int i = 255; i >= 0; i--) {
    analogWrite(REDPIN, i);
    delay(time);
  }
  // Blue increase
  for (int i = 0; i < 255; i++) {
    analogWrite(BLUPIN, i);
    delay(time);
  }
  // Green decrease
  for (int i = 255; i >= 0; i--) {
    analogWrite(GREPIN, i);
    delay(time);
  }
  // Red increase
  for (int i = 0; i < 255; i++) {
    analogWrite(REDPIN, i);
    delay(time);
  }
  // Blue decrease
  for (int i = 255; i >= 0; i--) {
    analogWrite(BLUPIN, i);
    delay(time);
  }
}
