# tessract

Arduino instructions

1. Download and install Arduino
2. Add ATtiny support
  * Arduino > Preferences > "Additional Boards Manager URLs "boards manager
  * Add the following json URL [json](https://raw.githubusercontent.com/damellis/attiny/ide-1.6.x-boards-manager/package_damellis_attiny_index.json)
  * Arduino > Tools > Board > Boards Manager
  * Download and Install the attiny
3. Select ATtiny Board
  * Arduino > Tools > Board > ATtiny25/45/85
4. Select ATtiny85 processor
  * Arduino > Tools > Processor > ATtiny85
5. Select Internal 8MHz
  * Arduino > Tools > Clock > Internal 8MHz

Programmer instructions

1. Add a 10uF capacitor to the Arduino board between RESET and GND
2. Select the Programmer
  * Arduino > Tools > Programmer > Arduino as ISP
3. Burn bootloader to make it run at 8MHz

| Arduino Pin | ATtiny85 Pin |
|---|---|
| 5V | VCC |
| GND | GND |
| Pin 10 | RESET |
| Pin 11 | Pin 0 |
| Pin 12 | Pin 1 |
| Pin 13 | Pin 2 |
